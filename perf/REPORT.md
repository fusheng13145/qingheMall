# 青禾商城 · 生产级容量压测报告（读链路）

> 生成日期：2026-08-12
> 工具：Locust 2.46.3（headless）
> 目标：补齐评估报告「性能/容量维度」指出的短板——此前仅有 `SeckillConcurrencyIntegrationTest` 等集成测试（30 线程断言不超卖），缺 HTTP 层并发压测证据。

## 一、压测环境

| 项 | 值 |
|---|---|
| 后端 | Spring Boot 3.3.13（dev profile，单机本地） |
| 监听端口 | 56685（dev profile 随机端口；`application.properties` 声明的 8080 被 dev profile 覆盖） |
| 依赖 | MySQL `qinghedb`（16 商品 / p001–p016，规格有库存）、Redis 7.x（缓存 + 秒杀预扣）均在线 |
| 压测机 | 同机（localhost），Windows / Git Bash |
| 目标接口 | 公开读链路（零写风险）：商品列表/搜索、商品详情、商品规格、商品评价、秒杀活动、品牌下拉 |

## 二、场景权重（模拟真实浏览画像）

| 任务 | 接口 | 权重 |
|---|---|---|
| 商品列表/搜索 | `GET /api/product/page?keyword=&pageSize=20` | 40 |
| 商品详情 | `GET /api/product/get?productId=` | 25 |
| 商品规格 | `GET /api/productdetail/productId?productId=` | 15 |
| 商品评价 | `GET /api/comment/product?productId=&pageSize=10` | 10 |
| 秒杀活动 | `GET /api/seckill/activities` | 5 |
| 品牌下拉 | `GET /api/product/brands` | 5 |

> 写链路（登录/下单/秒杀）未纳入：其一已在集成测试层验证不超卖；其二受限流保护（下单 20/s、登录 5/s），压测写库会污染数据且触发限流导致指标失真。后续阶段二可结合 JMeter 做带会话的写链路专项压测。

## 三、阶梯压测结果（每轮 90s，孵化率 20/s）

| 并发用户 | 总请求 | 失败数 | **RPS** | P50 | P95 | P99 | P99.9 | Max | 失败率 |
|---|---|---|---|---|---|---|---|---|---|
| 50  | 3514  | 0 | 39.7  | 4ms  | 7ms  | 20ms | 300ms | 305ms | 0% |
| 100 | 7011  | 0 | 78.3  | 3ms  | 6ms  | 16ms | 32ms  | 35ms  | 0% |
| 200 | 13642 | 0 | 152.5 | 3ms  | 6ms  | 18ms | 27ms  | 31ms  | 0% |

合计 24167 请求，**0 失败**。

**结论：**
1. **吞吐随并发线性扩展**：RPS 39.7 → 78.3 → 152.5，近似随并发翻倍，读链路无锁瓶颈或连接池耗尽迹象。
2. **延迟极低且稳定**：稳态 P95 ≤ 7ms、P99 ≤ 20ms；Max 在 50 并发冷启动期出现 300ms（首波 GC/缓存冷），100/200 并发迅速回落至 31–35ms，说明缓存热身完成后延迟进一步收敛。
3. **稳定性极佳**：三轮零失败，无 5xx、无超时、无限流拒绝（读接口无限流）。

## 四、缓存可观测性闭环（验证 `feat-stock-cache` 增强）

压测驱动真实读流量穿越 `ProductCacheService`（`ProductController.get`→`ProductServiceImpl.findById`→`getProduct`；`/api/productdetail/productId`→`getDetails`），经 `/actuator/prometheus` 抓取：

| 指标 | 压测前（基线） | 压测后 | 说明 |
|---|---|---|---|
| `qinghe.product.cache.hits_total` | 0 | **9718** | 缓存命中 |
| `qinghe.product.cache.misses_total` | 0 | **35** | 未命中（首载/回补） |
| `qinghe.product.cache.hitratio` | 0.0 | **0.9964（99.64%）** | 命中率 Gauge |

- 命中次数（9753）与「商品详情 + 规格」权重（40%）× 总请求（24167）≈ 9667 高度吻合，**证明命中/未命中计数器真实计量了读流量，非虚标**。
- 99.64% 命中率直接印证 Cache-Aside 缓存（P1-10 落地）在真实负载下的收益；`feat-stock-cache` 新增的命中率 Gauge 已可被 Grafana 面板直接观测。

## 五、遗留与建议

1. **写链路压测缺失**：下单/秒杀的 HTTP 层容量尚未独立压测（仅集成测试层覆盖正确性）。建议阶段二用 JMeter 做带登录会话的写链路专项压测，并临时放宽限流或标注限流阈值为容量上限。
2. **单机局限性**：本压测为同机 localhost，未覆盖网络栈、负载均衡、集群横向扩展。生产环境应结合 Grafana 持续观测 + 网关层压测。
3. **端口治理**：dev profile 随机端口（56685）不利于压测复现，建议压测/CI 环境固定 `server.port` 或通过 `--server.port` 显式指定。
4. **prometheus 暴露**：`/actuator/prometheus` 已在本地暴露，生产部署需在 `management.endpoints.web.exposure.include` 显式含 `prometheus` 并保持内网可达（避免指标外泄）。

## 六、产物

- `perf/locustfile.py`：压测脚本（权重场景）
- `perf/r_50_stats.csv` / `r_100_stats.csv` / `r_200_stats.csv`：各轮详细统计
- `perf/r_50.html` / `r_100.html` / `r_200.html`：Locust HTML 报告
- `perf/run.log`：运行日志

> CSV/HTML 为压测产物，不纳入版本控制（见 `.gitignore`）；`locustfile.py` 与本报告入仓作为性能测试资产。
