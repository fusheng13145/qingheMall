"""
青禾商城 · 写链路 per-user 限流验证（轻量，仅依赖 requests）
==========================================================
证明 #36：下单限流由「全局 20/s」改为「per-user 20/s」后，
聚合吞吐不再受单桶 20/s 钳制——多账号并发下成功下单速率应显著 > 20/s，
且库存一致性校验无超卖、无丢失。

关键修正（相对初版）：
 1) 限流判定由「HTTP 429」改为「body code==429」：
    RateLimitException 经 GlobalExceptionHandler 映射为 HTTP 200 + body code=429
    （非 HTTP 429），初版把限流拒绝误计为业务失败。
 2) 预登录建会话（带重试 + 顺序错峰），worker 复用「已认证会话」而非每线程并发自登录：
    规避登录接口 5/s 全局限流导致多数会话无 userId、限流 key 回落全局桶
    （OrderController.addOrder，20/s 全共享）的污染，确保每用户命中独立 per-user 桶。

流程：
 1) 顺序预置并登录 NUM_USERS 个账号（perftest_000..，密码 perf123，幂等），返回带 cookie 的会话；
 2) 目标 SKU(pd004) 库存置高（PID_TP_INIT）；
 3) 用 NUM_USERS 个线程（每线程一个【已认证】会话）并发下单 DURATION 秒；
 4) 统计成功(200/code200)/限流(code429)/业务失败/其他，输出聚合成功速率与「相对旧 20/s 上限倍数」；
 5) 直连 DB 核对：初始库存 - 剩余库存 == 成功下单数（无超卖/丢失）。

运行：python perf/verify_w_peruser.py
"""
import os
import time
import sys
import threading
import pymysql
import requests
from concurrent.futures import ThreadPoolExecutor

TARGET_HOST = os.getenv("TARGET_HOST", "http://localhost:8089")
TEST_USER_PREFIX = os.getenv("TEST_USER_PREFIX", "perftest_")
TEST_PWD = os.getenv("TEST_PWD", "perf123")
NUM_USERS = int(os.getenv("NUM_USERS", "50"))
DURATION = int(os.getenv("DURATION", "20"))
PID_TP = os.getenv("PID_TP", "pd004")
PID_TP_INIT = int(os.getenv("PID_TP_INIT", "100000"))
# 登录接口 5/s 全局限流；预登录顺序错峰步长需 < 1/5s 余量，这里取 0.25s（4/s < 5/s）
LOGIN_STEP = float(os.getenv("LOGIN_STEP", "0.25"))

DB_CFG = dict(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=3306,
    user="root",
    passwd=os.getenv("DB_PWD", "1234"),
    db=os.getenv("DB_NAME", "qinghedb"),
    charset="utf8mb4",
)
HEADERS = {"Origin": "http://localhost:5173"}

lock = threading.Lock()
stats = {"success": 0, "ratelimited": 0, "bizfail": 0, "other": 0}
code_hist = {}


def auth_user(idx):
    """顺序预登录，返回 (Session, user, ok)。带重试以应对登录 5/s 限流导致的瞬时拒绝。"""
    user = "%s%03d" % (TEST_USER_PREFIX, idx)
    s = requests.Session()
    s.headers.update(HEADERS)
    # 1) 先尝试直接登录（账号已存在时）
    try:
        r = s.post(TARGET_HOST + "/api/user/login",
                   data={"userName": user, "pwd": TEST_PWD}, timeout=10)
        if r.status_code == 200 and r.json().get("code") == 200:
            return s, user, True
    except Exception:
        pass
    # 2) 注册（幂等：已存在则报错忽略）
    try:
        requests.post(TARGET_HOST + "/api/user/reg",
                      data={"userName": user, "pwd": TEST_PWD, "role": "USER"},
                      headers=HEADERS, timeout=10)
    except Exception:
        pass
    # 3) 重试登录（应对 5/s 限流造成的 401/429）
    for _ in range(12):
        try:
            r = s.post(TARGET_HOST + "/api/user/login",
                       data={"userName": user, "pwd": TEST_PWD}, timeout=10)
            if r.status_code == 200 and r.json().get("code") == 200:
                return s, user, True
        except Exception:
            pass
        time.sleep(0.3)
    return s, user, False


def worker(s):
    payload = {
        "productDetailId": PID_TP,
        "quantity": 1,
        "receiverName": "压测",
        "receiverPhone": "13800000000",
        "receiverAddress": "青禾压测地址",
    }
    end = time.time() + DURATION
    while time.time() < end:
        try:
            r = s.post(TARGET_HOST + "/api/order/add", json=payload, timeout=10)
            try:
                code = r.json().get("code")
            except Exception:
                code = None
            with lock:
                code_hist[(r.status_code, code)] = code_hist.get((r.status_code, code), 0) + 1
            if r.status_code == 200 and code == 200:
                with lock:
                    stats["success"] += 1
            elif code == 429:
                # 限流经 GlobalExceptionHandler 映射为 HTTP 200 + body code 429（非 HTTP 429）
                with lock:
                    stats["ratelimited"] += 1
            elif code == 401:
                with lock:
                    stats["bizfail"] += 1
            else:
                with lock:
                    stats["bizfail"] += 1
        except Exception:
            with lock:
                stats["other"] += 1


def main():
    print("目标后端: %s  用户数: %d  时长: %ds  SKU: %s(库存置%d)"
          % (TARGET_HOST, NUM_USERS, DURATION, PID_TP, PID_TP_INIT))
    # 1) 顺序预登录（错峰 + 重试，规避登录 5/s 限流污染）
    sessions = []
    ok_cnt = 0
    for i in range(NUM_USERS):
        s, user, ok = auth_user(i)
        sessions.append(s)
        if ok:
            ok_cnt += 1
        else:
            print("  ⚠️ 用户 %s 登录失败（将回落全局桶）" % user)
        if (i + 1) % 10 == 0:
            print("  预登录 %d/%d（成功 %d）" % (i + 1, NUM_USERS, ok_cnt))
        time.sleep(LOGIN_STEP)
    print("预登录完成：%d/%d 会话已认证" % (ok_cnt, NUM_USERS))
    # 2) 目标 SKU 库存置高
    try:
        conn = pymysql.connect(**DB_CFG)
        cur = conn.cursor()
        cur.execute("UPDATE product_detail SET stock=%s WHERE id=%s", (PID_TP_INIT, PID_TP))
        conn.commit()
        cur.close(); conn.close()
        print("SKU %s 库存已置 %d" % (PID_TP, PID_TP_INIT))
    except Exception as e:
        print("⚠️ 库存设置失败: %s" % e); sys.exit(1)
    # 3) 并发下单（复用已认证会话）
    t0 = time.time()
    with ThreadPoolExecutor(max_workers=NUM_USERS) as ex:
        list(ex.map(worker, sessions))
    elapsed = time.time() - t0
    succ = stats["success"]; rl = stats["ratelimited"]; bf = stats["bizfail"]; ot = stats["other"]
    rate = succ / elapsed if elapsed else 0
    print("\n==================== 写链路(per-user)验证结果 ====================")
    print("压测时长(s)      : %.1f" % elapsed)
    print("成功下单          : %d" % succ)
    print("限流拦截(code429) : %d" % rl)
    print("业务失败          : %d" % bf)
    print("其他异常          : %d" % ot)
    print("成功速率(req/s)   : %.2f" % rate)
    print("相对旧全局 20/s   : %.2fx  (>>1 即证明全局天花板已解除)" % (rate / 20.0))
    print("HTTP/body code 直方图（前 6）:")
    for (st, cd), n in sorted(code_hist.items(), key=lambda kv: -kv[1])[:6]:
        print("    (%s, %s): %d" % (st, cd, n))
    # 4) 库存一致性
    try:
        conn = pymysql.connect(**DB_CFG)
        cur = conn.cursor()
        cur.execute("SELECT stock FROM product_detail WHERE id=%s", (PID_TP,))
        remain = cur.fetchone()[0]
        cur.close(); conn.close()
        consumed = PID_TP_INIT - remain
        print("DB 剩余库存       : %d" % remain)
        print("DB 实际扣减       : %d" % consumed)
        if consumed == succ and remain >= 0:
            print("✅ 一致性通过：无超卖、无丢失")
        else:
            print("⚠️ 不一致：差值=%d" % (consumed - succ))
    except Exception as e:
        print("⚠️ 库存校验异常: %s" % e)
    print("==================================================================")


if __name__ == "__main__":
    main()
