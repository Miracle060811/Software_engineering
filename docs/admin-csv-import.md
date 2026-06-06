# 管理后台 CSV 批量导入说明

本文档对应当前 `AdminDashboard.vue` 与 `AdminController` 的实现，用于维护管理后台资源批量导入功能。

## 功能范围

管理后台支持导入 6 类运营资源：

| type | 资源 | 管理入口 |
| --- | --- | --- |
| `flights` | 航班资源 | 管理后台 - 航班资源 |
| `trains` | 火车资源 | 管理后台 - 火车资源 |
| `hotels` | 酒店资源 | 管理后台 - 酒店资源 |
| `rooms` | 酒店房型 | 管理后台 - 酒店资源 - 房型管理 |
| `attractions` | 景点资源 | 管理后台 - 景点资源 |
| `destinations` | 城市资源 | 管理后台 - 城市资源 |

前端入口是各资源页的“导入 CSV”按钮。弹窗内可选择资源类型、写入方式、是否只预检，并可下载当前资源模板。

## 接口与权限

- 模板下载：`GET /api/admin/import/{type}/template`
- 批量导入：`POST /api/admin/import/{type}`
- 请求格式：`multipart/form-data`
- 上传字段：`file`
- 查询参数：
  - `dryRun`：`true` 只校验不写库，`false` 实际写库，默认 `false`
  - `mode`：`insert` 仅新增，`upsert` 重复则更新，默认 `insert`

所有接口都会调用 `checkAdmin()`，只有 `role = 1` 的管理员用户可用；未登录或非管理员会被拦截或返回“无管理员权限”。

## 通用规则

- 文件编码使用 UTF-8，支持 UTF-8 BOM。
- 文件大小不能超过 5MB。
- 第一行必须是字段名。建议使用本文档中的英文名。
- 表头匹配会忽略 BOM、大小写、下划线、短横线和空格，例如 `flight_no`、`flight-no`、`flightNo` 都可匹配到 `flightNo`。
- 数据行字段按表头名称解析，不依赖列顺序。
- 空行会被跳过；至少需要 1 行有效数据。
- 支持标准 CSV 引号、逗号转义和多行字段。
- 数字字段支持去掉 `￥`、`¥` 和千分位逗号后解析。
- 时间字段支持：
  - `2026-06-01T08:00:00`
  - `2026-06-01 08:00:00`
  - `2026-06-01 08:00`
- 日期字段使用 `2026-05-30`。
- `dryRun=true` 会完整校验表头和每一行数据，但不会插入或更新数据库。
- 导入不是全局事务：某一行失败不会回滚其他成功行。前端会展示成功/失败统计和失败行原因。
- 后端最多返回 50 条失败原因，超过部分只计入失败数。

返回数据字段：

| 字段 | 含义 |
| --- | --- |
| `type` | 导入资源类型 |
| `mode` | 实际写入方式：`insert` 或 `upsert` |
| `dryRun` | 是否只预检 |
| `total` | 有效数据行总数 |
| `success` | 成功行数 |
| `failed` | 失败行数 |
| `inserted` | 新增行数 |
| `updated` | 更新行数 |
| `validated` | 预检通过行数 |
| `failures` | 失败行列表，含 `line` 和 `reason` |
| `failureLimit` | 失败原因返回上限，当前为 50 |

## 写入与重复更新规则

| type | `insert` | `upsert` 匹配规则 |
| --- | --- | --- |
| `flights` | 直接插入 | 按 `flightNo` 查找，存在则更新 |
| `trains` | 直接插入 | 按 `trainNo` 查找，存在则更新 |
| `hotels` | 直接插入 | 按 `name + city + address` 查找，存在则更新 |
| `rooms` | 直接插入 | 按 `hotelId + roomType` 查找，存在则更新 |
| `attractions` | 直接插入 | 按 `name + city` 查找，存在则更新 |
| `destinations` | 按 `slug` 查找，存在则更新，不存在则插入 | 同 `insert` |

注意：

- 城市资源 `destinations` 当前始终按 `slug` 做新增或更新，前端选择“仅新增”也不会阻止更新已有城市。
- 房型导入成功后会调用 `hotelRoomStockService.syncWithDatabase(roomId)` 同步 Redis 房态库存。
- 房型导入要求 `hotelId` 已存在；建议先导入酒店，再导入房型。

## 字段默认值与校验

通用校验：

- 必填字段为空会失败。
- 金额、库存、座位、票数、评分等数字格式不合法会失败。
- 价格、库存、座位、票数不能为负数。
- `status` 未提供时默认 `1`。

资源级校验：

- 航班/火车：到达时间必须晚于出发时间。
- 航班：`availableSeats` 不能大于 `totalSeats`。
- 火车：`durationMinutes` 可选；不填时按出发/到达时间自动计算。
- 酒店：`starRating` 必须在 `1-5`；`score` 默认 `4.5`，提供时必须在 `0-5`。
- 房型：`area` 默认 `30`；`availableRooms` 不能大于 `totalRooms`。
- 景点：`availableTickets` 不能大于 `totalTickets`。
- 城市：`country` 默认 `中国`，`sortOrder` 默认 `100`。

## 航班 `flights`

必填字段：`flightNo, airline, departureCity, arrivalCity, departureTime, arrivalTime, economyPrice, businessPrice, totalSeats, availableSeats`

可选字段：`status`

```csv
flightNo,airline,departureCity,arrivalCity,departureTime,arrivalTime,economyPrice,businessPrice,totalSeats,availableSeats,status
CA1001,中国国际航空,北京,上海,2026-06-01T08:00:00,2026-06-01T10:00:00,680,2180,200,120,1
```

## 火车 `trains`

必填字段：`trainNo, trainType, departureStation, arrivalStation, departureTime, arrivalTime, firstClassPrice, secondClassPrice, firstClassSeats, secondClassSeats`

可选字段：`durationMinutes, status`

```csv
trainNo,trainType,departureStation,arrivalStation,departureTime,arrivalTime,firstClassPrice,secondClassPrice,firstClassSeats,secondClassSeats,durationMinutes,status
G1001,G,北京南,上海虹桥,2026-06-01T08:00:00,2026-06-01T12:30:00,880,553,80,420,270,1
```

## 酒店 `hotels`

必填字段：`name, city, address, starRating, avgPrice`

可选字段：`description, coverImg, lat, lng, score, status`

```csv
name,city,address,starRating,avgPrice,description,coverImg,lat,lng,score,status
城市花园酒店,上海,上海市黄浦区示例路1号,4,520,近地铁商务酒店,https://example.com/hotel.jpg,31.2304,121.4737,4.6,1
```

## 房型 `rooms`

必填字段：`hotelId, roomType, bedType, price, totalRooms, availableRooms`

可选字段：`area, images, facilities, status`

`images` 和 `facilities` 当前按字符串保存，可使用 JSON 字符串。字段内包含双引号时需要按 CSV 规则转义。

```csv
hotelId,roomType,bedType,price,totalRooms,availableRooms,area,images,facilities,status
1,豪华大床房,1张大床,688,20,12,38,"[""/images/seed/hotel.svg""]","[""早餐"",""洗衣房""]",1
```

## 景点 `attractions`

必填字段：`name, city, address, adultPrice, childPrice, totalTickets, availableTickets`

可选字段：`description, coverImg, openTime, lat, lng, officialUrl, sourceName, dataCheckedDate, status`

```csv
name,city,address,adultPrice,childPrice,totalTickets,availableTickets,description,coverImg,openTime,lat,lng,officialUrl,sourceName,dataCheckedDate,status
示例景区,杭州,杭州市示例路1号,80,40,1000,800,城市观光景区,https://example.com/scenic.jpg,08:00-18:00,30.2741,120.1551,https://example.com,景区官网,2026-05-30,1
```

## 城市 `destinations`

必填字段：`slug, name, tag, img, desc, intro`

可选字段：`country, keywords, highlights, culture, bestSeason, transport, sourceName, sourceUrl, sortOrder, status`

`keywords`、`highlights` 等列表型内容当前按字符串保存，项目中常用 `|` 分隔。

```csv
slug,name,tag,img,desc,intro,country,keywords,highlights,culture,bestSeason,transport,sourceName,sourceUrl,sortOrder,status
dali,大理,风花雪月,/images/seed/lake.svg,苍山洱海与古城生活交织,适合慢旅行的城市,中国,洱海|古城|苍山,洱海骑行|古城夜游,白族文化,春秋季,高铁到大理站,公开旅游资料,https://example.com,90,1
```

## 维护注意事项

- 新增导入类型时，需要同步维护：
  - `AdminController.CSV_IMPORT_TYPES`
  - `AdminController.CSV_REQUIRED_HEADERS`
  - `AdminController.importCsvRow()`
  - `AdminController.csvHeadersFor()`
  - `AdminController.csvTemplateSample()`
  - `frontend/src/views/admin/AdminDashboard.vue` 中的 `importTypeOptions` 和 `downloadCsvTemplate()`
- 修改字段名或必填规则后，要同步更新本文档和前端弹窗的字段提示。
- 模板列顺序可以调整，但必须保证表头名称能被后端识别。
- 图片字段优先使用真实可访问 URL 或项目内 `/images/...`、`/uploads/...` 路径，不要引入 `picsum.photos` 这类随机占位图。
- 大批量导入前建议先勾选“只预检，不写入数据库”，确认失败行为 0 后再实际导入。
