"""
青禾商城 · 写链路边界验证（库存耗尽不超卖）
============================================
对库存较低的 SKU（默认 pd045，压测前已设为 30）以受控低速（3/s，远低于全局 20/s
限流，避免限流干扰判定）发起 100 次下单（quantity=1），验证：
  - 成功下单数 == 初始库存（30）
  - DB 剩余库存 == 0
  - order 表该 SKU 订单数 == 30
  - stock_log 表该 SKU ORDER_DEDUCT 流水数 == 30
  - 无超卖（剩余库存不为负、成功数不超初始）

依赖：后端 dev 在线（TARGET_HOST），且 pd045 库存已预设为 30。
运行：python perf/verify_stock.py
"""
import os
import time
import pymysql
import requests

HOST = os.getenv("TARGET_HOST", "http://localhost:8089")
PID_EDGE = os.getenv("PID_EDGE", "pd045")
PID_EDGE_INIT = int(os.getenv("PID_EDGE_INIT", "30"))
TEST_USER = os.getenv("TEST_USER", "perftest_main")
TEST_PWD = os.getenv("TEST_PWD", "perf123")
N = int(os.getenv("N_REQUESTS", "100"))
RATE = float(os.getenv("RATE", "3"))  # 请求/s，远低于 20/s 限流

DB_CFG = dict(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=3306,
    user="root",
    passwd=os.getenv("DB_PWD", "1234"),
    db=os.getenv("DB_NAME", "qinghedb"),
    charset="utf8mb4",
)


def main():
    s = requests.Session()
    s.headers.update({"Origin": "http://localhost:5173"})  # CSRF 白名单来源，否则 403
    r = s.post(HOST + "/api/user/login", data={"userName": TEST_USER, "pwd": TEST_PWD})
    if r.json().get("code") != 200:
        raise SystemExit("登录失败: %s" % r.text)
    print("登录 OK，开始边界下单 (%d 次, %.1f/s, SKU=%s 初始库存=%d)" % (N, RATE, PID_EDGE, PID_EDGE_INIT))

    success = 0
    biz_fail = 0
    ratelimited = 0
    interval = 1.0 / RATE
    t0 = time.time()
    for i in range(N):
        r = s.post(
            HOST + "/api/order/add",
            json={
                "productDetailId": PID_EDGE,
                "quantity": 1,
                "receiverName": "边界",
                "receiverPhone": "13800000000",
                "receiverAddress": "x",
            },
        )
        if r.status_code == 200 and r.json().get("code") == 200:
            success += 1
        elif r.status_code == 429:
            ratelimited += 1
        else:
            biz_fail += 1
        time.sleep(interval)
    elapsed = time.time() - t0

    conn = pymysql.connect(**DB_CFG)
    cur = conn.cursor()
    cur.execute("SELECT stock FROM product_detail WHERE id=%s", (PID_EDGE,))
    remain = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM `order` WHERE product_detail_id=%s", (PID_EDGE,))
    order_cnt = cur.fetchone()[0]
    cur.execute(
        "SELECT COUNT(*) FROM stock_log WHERE product_detail_id=%s AND change_type='ORDER_DEDUCT'",
        (PID_EDGE,),
    )
    log_cnt = cur.fetchone()[0]
    cur.close()
    conn.close()

    print("\n==================== 边界不超卖校验 (SKU=%s) ====================" % PID_EDGE)
    print("请求总数=%d  成功下单=%d  业务失败=%d  限流拦截=%d" % (N, success, biz_fail, ratelimited))
    print("耗时=%.1fs  成功速率=%.2f/s" % (elapsed, success / elapsed if elapsed else 0))
    print("DB 剩余库存=%d  订单表订单数=%d  库存流水(ORDER_DEDUCT)=%d" % (remain, order_cnt, log_cnt))
    print("----------------------------------------------------------------------")
    ok = (
        success == PID_EDGE_INIT
        and remain == 0
        and order_cnt == PID_EDGE_INIT
        and log_cnt == PID_EDGE_INIT
        and remain >= 0
    )
    if ok:
        print("✅ 不超卖验证通过：成功下单数==初始库存==订单数==流水数==%d，剩余库存=0" % PID_EDGE_INIT)
    else:
        print("⚠️ 不超卖验证异常：成功=%d 剩余=%d 订单=%d 流水=%d 初始=%d" % (success, remain, order_cnt, log_cnt, PID_EDGE_INIT))
    print("====================================================================")
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
