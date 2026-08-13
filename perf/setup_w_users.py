"""
青禾商城 · 写链路多用户压测 · 预置脚本（幂等，可重复执行）
=========================================================
1) 预注册 NUM_USERS 个账号（perftest_000..，密码 perf123），受 5/s 注册限流，
   逐条注册并间隔 0.25s 规避 429；已存在则跳过。
2) 将目标 SKU（pd004）库存调高至 PID_TP_INIT（默认 50000），供吞吐压测消耗。

依赖：requests、pymysql（pip install requests pymysql）。
运行：python perf/setup_w_users.py
"""
import os
import time
import sys
import pymysql
import requests

TARGET_HOST = os.getenv("TARGET_HOST", "http://localhost:8089")
TEST_USER_PREFIX = os.getenv("TEST_USER_PREFIX", "perftest_")
TEST_PWD = os.getenv("TEST_PWD", "perf123")
NUM_USERS = int(os.getenv("NUM_USERS", "100"))
PID_TP = os.getenv("PID_TP", "pd004")
PID_TP_INIT = int(os.getenv("PID_TP_INIT", "50000"))

DB_CFG = dict(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=3306,
    user="root",
    passwd=os.getenv("DB_PWD", "1234"),
    db=os.getenv("DB_NAME", "qinghedb"),
    charset="utf8mb4",
)

HEADERS = {"Origin": "http://localhost:5173"}


def ensure_user(idx):
    user = "%s%03d" % (TEST_USER_PREFIX, idx)
    # 先尝试登录
    r = requests.post(
        TARGET_HOST + "/api/user/login",
        data={"userName": user, "pwd": TEST_PWD},
        headers=HEADERS,
        timeout=10,
    )
    try:
        ok = r.status_code == 200 and r.json().get("code") == 200
    except Exception:
        ok = False
    if ok:
        return "exists"
    # 注册
    r = requests.post(
        TARGET_HOST + "/api/user/reg",
        data={"userName": user, "pwd": TEST_PWD, "role": "USER"},
        headers=HEADERS,
        timeout=10,
    )
    return "reg:%s" % r.status_code


def main():
    print("目标后端: %s  预置用户数: %d  前缀: %s" % (TARGET_HOST, NUM_USERS, TEST_USER_PREFIX))
    created = 0
    for i in range(NUM_USERS):
        status = ensure_user(i)
        if status == "reg:200" or status.startswith("reg:") and "200" in status:
            created += 1
        if (i + 1) % 20 == 0:
            print("  已处理 %d/%d ..." % (i + 1, NUM_USERS))
        time.sleep(0.25)  # 尊重 5/s 注册限流
    print("账号预置完成（新注册约 %d 个）。" % created)

    # 调高目标 SKU 库存
    try:
        conn = pymysql.connect(**DB_CFG)
        cur = conn.cursor()
        cur.execute("UPDATE product_detail SET stock=%s WHERE id=%s", (PID_TP_INIT, PID_TP))
        conn.commit()
        print("目标 SKU %s 库存已置为 %d。" % (PID_TP, PID_TP_INIT))
        cur.close()
        conn.close()
    except Exception as e:
        print("⚠️ 库存设置失败: %s" % e)
        sys.exit(1)


if __name__ == "__main__":
    main()
