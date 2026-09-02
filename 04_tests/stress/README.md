# 压力测试执行说明

已完成的正式记录：[`results/performance-2026-09-02/README.md`](results/performance-2026-09-02/README.md)。该记录包含单体与微服务两个查询场景各 3 次的 k6 原始结果、资源采样和中位数分析。

## 1. 场景与依据

依据《5组-软件详细设计说明》3.12 性能验证要求：同一提交、同一数据量、同一硬件、同一脚本；每场景预热后连续运行 3 次，报告给出每次结果和中位数。

| 脚本 | 接口 | 验证目标 |
| :--- | :--- | :--- |
| `flight-search.js` | `GET /api/flight/search` | 查询吞吐与 P95 响应 |
| `hotel-search.js` | `GET /api/hotel/search` | 查询吞吐与 P95 响应 |
| `hotel-order.js` | `POST /api/hotel/order/create` | **高并发防超卖**（成功订单数 ≤ 初始库存） |

三个场景对应任务书「2 到 3 个主要接口」的单体 vs 微服务性能对比要求，也是 HPA 自动扩缩容实验的负载源。

HPA 自动实验入口：

```powershell
.\scripts\experiments\Invoke-MicroserviceHpaExperiment.ps1
```

该脚本要求微服务已通过 `scripts/cd/Deploy-Microservices.ps1` 部署，并且 Metrics Server 与 k6 可用。它会保存扩容前、中、后状态；正式结论填写 `HPA实验记录模板.md`，未实际触发扩缩容前不能标记通过。

## 2. 环境

- k6 >= 0.50（安装：`winget install grafana.k6` 或 https://k6.io/docs/get-started/installation/）
- 后端已启动且完成 Flyway 迁移（单体：`http://127.0.0.1:8080`）
- 记录：机器型号、CPU、内存、JDK 版本、后端提交号（`git rev-parse HEAD`）

## 3. 执行步骤

每个场景连续跑 3 次（RUN=1/2/3），原始结果落 `results/`：

```powershell
# 场景一：航班查询
k6 run -e BASE_URL=http://127.0.0.1:8080 -e RUN=1 --summary-export=results/flight-search-run1.json flight-search.js
k6 run -e BASE_URL=http://127.0.0.1:8080 -e RUN=2 --summary-export=results/flight-search-run2.json flight-search.js
k6 run -e BASE_URL=http://127.0.0.1:8080 -e RUN=3 --summary-export=results/flight-search-run3.json flight-search.js

# 场景二：酒店查询（同上，换 hotel-search.js）

# 场景三：酒店下单（并发抢库存）
# 压测前先查库记录目标房型可用库存：
#   SELECT available_rooms FROM tm_hotel_room WHERE id = <ROOM_ID>;
k6 run -e BASE_URL=http://127.0.0.1:8080 -e HOTEL_ID=6 -e ROOM_ID=14 `
      -e INITIAL_STOCK=1 -e VU_COUNT=50 -e RUN=1 `
      --summary-export=results/hotel-order-run1.json hotel-order.js
# 重复 RUN=2/3；每次压测前将库存重置回 INITIAL_STOCK 并清理压测账号产生的订单
```

## 4. 下单场景的库存一致性核对

脚本 teardown 自动断言「成功订单数 ≤ INITIAL_STOCK」并输出 `ASSERTION PASSED/FAILED`。
压测后必须人工核对数据库（双重验证）：

```sql
-- 库存不为负
SELECT id, available_rooms FROM tm_hotel_room WHERE id = <ROOM_ID>;
-- 有效订单数 = 成功订单数（订单数与库存扣减量一致）
SELECT COUNT(*) FROM tm_hotel_order WHERE room_id = <ROOM_ID> AND status NOT IN (3, 5);
```

满足「通过」条件：`available_rooms >= 0` 且 `成功订单数 = 初始库存 - available_rooms`。

## 5. 采集指标

每次运行 `--summary-export` 输出 JSON 原始结果，指标包括：

- 并发数（VUs）、总请求数、吞吐量（http_reqs rate）
- 平均响应时间（http_req_duration avg）、P95（p(95)）
- 错误率（http_req_failed rate）
- CPU/内存：压测期间另开终端采样（如 `Get-Process java | Select-Object CPU, WorkingSet` 每秒记录）

## 6. 单体 vs 微服务对比

同机、同数据、同脚本，分别在两个版本上各跑 3 次；微服务版本 `BASE_URL` 指向网关或任一服务入口。结果表模板：

| 场景 | 版本 | Run | 并发 | 吞吐(req/s) | 平均(ms) | P95(ms) | 错误率 | CPU% | 内存(MB) |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |

中位数行加粗/标注。如实报告差异及原因分析，不虚报提升。
