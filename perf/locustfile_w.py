"""
青禾商城 · 写链路压测脚本（下单 + 库存扣减）
================================================
目标接口：POST /api/order/add  （OrderController.addOrder，全局限流 20/s → 429）
鉴权方式：HttpSession（Spring Session → Redis），Locust HttpUser 自动管理 SESSION cookie。

关键设计：
- 所有虚拟用户登录同一预注册账号 perftest_main（不共享 session，各自独立会话），
  下单时按 productDetailId 分布式锁串行化 + 原子扣减库存，天然验证高并发不超卖。
- 下单接口全局限流 20/s（Redisson RRateLimiter），超出返回 HTTP 429。
  429 属预期业务防护（防刷单），用 catch_response 标记为成功以免污染失败率，
  但单独计入 Counters.ratelimited，在 test_stop 如实披露。
- test_stop 时直连 DB 核对库存一致性：初始设定库存 - 剩余库存 == 成功下单数，
  证明无超卖、无丢失。

运行（需先启动后端 dev，端口见 TARGET_HOST）：
  locust -f perf/locustfile_w.py -u 50  -r 5 -t 60s --headless \
         -H http://localhost:8089 --html perf/w_50.html  --csv perf/w_50
  （u=100 / 200 同理，产出 w_100 / w_200）
"""
import os
import time
import pymysql
from locust import HttpUser, task, between, events

TARGET_HOST = os.getenv("TARGET_HOST", "http://localhost:8089")
PID_TP = os.getenv("PID_TP", "pd004")                 # 吞吐目标 SKU（压测前已调库存至 50000）
PID_TP_INIT = int(os.getenv("PID_TP_INIT", "50000"))  # 吞吐目标 SKU 初始设定库存
TEST_USER = os.getenv("TEST_USER", "perftest_main")
TEST_PWD = os.getenv("TEST_PWD", "perf123")

DB_CFG = dict(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=3306,
    user="root",
    passwd=os.getenv("DB_PWD", "1234"),
    db=os.getenv("DB_NAME", "qinghedb"),
    charset="utf8mb4",
)

# 自定义计数器（单进程 headless 下安全）
class C:
    success = 0
    ratelimited = 0
    bizfail = 0

T0 = time.time()


class OrderUser(HttpUser):
    host = TARGET_HOST
    wait_time = between(0.1, 0.5)  # 用户思考时间；意图速率远高于限流，用于暴露 20/s 限流行为

    def on_start(self):
        # CSRF 防护要求带合法 Origin（白名单含前端 5173），否则 403
        self.client.headers.update({"Origin": "http://localhost:5173"})
        # 仅登录预注册账号（注册在压测外已完成，避免 on_start 高频注册触发 5/s 注册限流）
        self.client.post(
            "/api/user/login",
            data={"userName": TEST_USER, "pwd": TEST_PWD},
            name="login",
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
                # 限流拦截：预期业务防护，不计入失败率，单独统计
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
    print("\n==================== 写链路压测 · 库存一致性校验 ====================")
    print("吞吐目标 SKU      : %s (初始设定库存=%d)" % (PID_TP, PID_TP_INIT))
    print("压测时长(s)       : %.1f" % elapsed)
    print("Locust 成功下单   : %d" % C.success)
    print("Locust 限流拦截   : %d  (HTTP 429，预期业务防护，非系统故障)")
    print("Locust 业务失败   : %d" % C.bizfail)
    if elapsed > 0:
        print("成功下单速率(req/s): %.2f" % (C.success / elapsed))
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
    print("====================================================================")
