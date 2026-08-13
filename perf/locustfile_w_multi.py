"""
青禾商城 · 写链路压测脚本（多用户池，验证 per-user 限流天花板解除）
=================================================================
目标接口：POST /api/order/add  （OrderController.addOrder）
鉴权方式：HttpSession（Spring Session → Redis），每个虚拟用户独立会话。

与 locustfile_w.py（单共享账号）的区别：
- 本脚本为每个虚拟用户分配**唯一账号**（perftest_<idx>），
  使 per-user 限流（RateLimitAspect 按会话 userId 分桶）下每个用户拥有
  **独立的 20/s 令牌桶**，从而聚合吞吐随用户数线性放开，
  直接验证 #36「全局 20/s 天花板已解除」。
- 单账号脚本下所有虚拟用户落到同一 user:<id> 桶，无法体现解除效果。

运行（需先启动后端 dev，端口见 TARGET_HOST）：
  # 1) 预置多用户 + 调高目标 SKU 库存（幂等，可重复）
  python perf/setup_w_users.py
  # 2) 多用户写链路压测
  locust -f perf/locustfile_w_multi.py -u 100 -r 20 -t 60s --headless \\
         -H http://localhost:8089 --html perf/wm_100.html --csv perf/wm_100
"""
import os
import time
import itertools
import threading
import pymysql
from locust import HttpUser, task, between, events

TARGET_HOST = os.getenv("TARGET_HOST", "http://localhost:8089")
PID_TP = os.getenv("PID_TP", "pd004")                  # 吞吐目标 SKU
PID_TP_INIT = int(os.getenv("PID_TP_INIT", "50000"))   # 吞吐目标 SKU 初始设定库存
TEST_USER_PREFIX = os.getenv("TEST_USER_PREFIX", "perftest_")
TEST_PWD = os.getenv("TEST_PWD", "perf123")
NUM_USERS = int(os.getenv("NUM_USERS", "100"))

DB_CFG = dict(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=3306,
    user="root",
    passwd=os.getenv("DB_PWD", "1234"),
    db=os.getenv("DB_NAME", "qinghedb"),
    charset="utf8mb4",
)

# 全局唯一账号分配器（headless 单进程下安全）
_lock = threading.Lock()
_idx = itertools.count(0)


class C:
    success = 0
    ratelimited = 0
    bizfail = 0


T0 = time.time()


def _user_for(idx):
    return "%s%03d" % (TEST_USER_PREFIX, idx)


class OrderUser(HttpUser):
    host = TARGET_HOST
    wait_time = between(0.05, 0.2)  # 意图速率远高于单用户 20/s

    def on_start(self):
        self.client.headers.update({"Origin": "http://localhost:5173"})
        with _lock:
            self.slot = next(_idx)
        self.username = _user_for(self.slot)
        # 登录预置账号（不存在则先注册，受 5/s 注册限流，setup 已预置故通常直接登录）
        r = self.client.post(
            "/api/user/login",
            data={"userName": self.username, "pwd": TEST_PWD},
            name="login",
        )
        if r.status_code != 200 or (r.text and '"code":200' not in r.text and '"code": 200' not in r.text):
            # 可能账号尚未预置：尝试注册后登录
            self.client.post(
                "/api/user/reg",
                data={"userName": self.username, "pwd": TEST_PWD, "role": "USER"},
                name="reg_fallback",
            )
            self.client.post(
                "/api/user/login",
                data={"userName": self.username, "pwd": TEST_PWD},
                name="login_after_reg",
            )

    @task(10)
    def place_order(self):
        with self.client.post(
            "/api/order/add",
            json={
                "productDetailId": PID_TP,
                "quantity": 1,
                "receiverName": "压测",
                "receiverPhone": "13800000000",
                "receiverAddress": "青禾压测地址",
            },
            name="order_add",
            catch_response=True,
        ) as resp:
            try:
                body = resp.json()
                code = body.get("code")
            except Exception:
                resp.failure("non-json resp status=%s" % resp.status_code)
                C.bizfail += 1
                return
            if resp.status_code == 200 and code == 200:
                resp.success()
                C.success += 1
            elif resp.status_code == 429:
                resp.success()
                C.ratelimited += 1
            elif code == 401:
                resp.failure("未登录(401)")
                C.bizfail += 1
            else:
                resp.failure("业务失败 code=%s %s" % (code, resp.text[:120]))
                C.bizfail += 1


@events.test_stop.add_listener
def on_stop(environment, **kw):
    elapsed = time.time() - T0
    print("\n==================== 写链路(多用户)压测 · 库存一致性校验 ====================")
    print("吞吐目标 SKU      : %s (初始设定库存=%d)" % (PID_TP, PID_TP_INIT))
    print("并发虚拟用户数    : %d (每用户独立 per-user 20/s 桶)" % NUM_USERS)
    print("压测时长(s)       : %.1f" % elapsed)
    print("Locust 成功下单   : %d" % C.success)
    print("Locust 限流拦截   : %d  (HTTP 429，预期业务防护，非系统故障)" % C.ratelimited)
    print("Locust 业务失败   : %d" % C.bizfail)
    if elapsed > 0:
        print("成功下单速率(req/s): %.2f" % (C.success / elapsed))
        print("聚合吞吐 vs 旧全局 20/s 上限: %.2fx" % ((C.success / elapsed) / 20.0))
    try:
        conn = pymysql.connect(**DB_CFG)
        cur = conn.cursor()
        cur.execute("SELECT stock FROM product_detail WHERE id=%s", (PID_TP,))
        row = cur.fetchone()
        remain = row[0] if row else None
        cur.close()
        conn.close()
        if remain is None:
            print("⚠️ 未找到 SKU %s" % PID_TP)
            return
        consumed = PID_TP_INIT - remain
        print("DB 剩余库存       : %d" % remain)
        print("DB 实际扣减       : %d" % consumed)
        if consumed == C.success and remain >= 0:
            print("✅ 一致性通过：DB扣减量 == 成功下单数，无超卖、无丢失")
        else:
            diff = consumed - C.success
            print("⚠️ 不一致：DB扣减(%d) != 成功下单(%d)，差值=%d" % (consumed, C.success, diff))
    except Exception as e:
        print("⚠️ 库存校验异常: %s" % e)
    print("==========================================================================")
