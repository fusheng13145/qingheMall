"""
青禾商城 · 生产级容量压测脚本（Locust）

目标：对商城最高频的「公开读链路」做 HTTP 层并发压测，补齐评估报告
（性能/容量维度）指出的「仅本地集成测试达标、缺生产级压测证据」短板。

覆盖场景（权重模拟真实浏览画像，全部为公开接口、零写风险）：
  - 商品列表/搜索 /api/product/page           权重 40
  - 商品详情     /api/product/get             权重 25
  - 商品规格     /api/productdetail/productId 权重 15
  - 商品评价     /api/comment/product         权重 10
  - 秒杀活动列表 /api/seckill/activities      权重 5
  - 品牌下拉     /api/product/brands          权重 5

说明：登录/下单/秒杀写链路已在 SeckillConcurrencyIntegrationTest 等集成测试层
验证（30 线程不超卖），且受限流保护（下单 20/s、登录 5/s），本轮压测聚焦读容量，
避免在压测中写库污染与触发限流导致指标失真。

运行（headless，阶梯并发）：
  locust -f locustfile.py --headless -u 100 -r 20 -t 90s --csv=perf/r_100 --html=perf/r_100.html
端口默认 56685（spring-boot:run dev profile 随机端口），可用 QH_PORT 覆盖。
"""
import os
import random

from locust import HttpUser, between, task

PORT = os.environ.get("QH_PORT", "56685")
HOST = f"http://127.0.0.1:{PORT}"

# 种子商品 p001..p016（来自 product 表，压测前已确认存在且有库存）
PRODUCT_IDS = [f"p{idx:03d}" for idx in range(1, 17)]
BRANDS = ["Nike", "Adidas", "New Balance", "Converse", "Vans", "Jordan", "Puma"]


class QingheShopUser(HttpUser):
    host = HOST
    wait_time = between(0.5, 2.0)

    @task(40)
    def product_list_search(self):
        # 约半数带关键词搜索，半数纯分页浏览
        if random.random() < 0.5:
            kw = random.choice(BRANDS)
            self.client.get(
                f"/api/product/page?pageSize=20&keyword={kw}",
                name="/api/product/page?keyword",
            )
        else:
            self.client.get("/api/product/page?pageSize=20", name="/api/product/page")

    @task(25)
    def product_detail(self):
        pid = random.choice(PRODUCT_IDS)
        self.client.get(f"/api/product/get?productId={pid}", name="/api/product/get")

    @task(15)
    def product_spec(self):
        pid = random.choice(PRODUCT_IDS)
        self.client.get(
            f"/api/productdetail/productId?productId={pid}",
            name="/api/productdetail/productId",
        )

    @task(10)
    def comment_list(self):
        pid = random.choice(PRODUCT_IDS)
        self.client.get(
            f"/api/comment/product?productId={pid}&pageSize=10",
            name="/api/comment/product",
        )

    @task(5)
    def seckill_activities(self):
        self.client.get("/api/seckill/activities", name="/api/seckill/activities")

    @task(5)
    def brands(self):
        self.client.get("/api/product/brands", name="/api/product/brands")
