# 管理后台 CSV 批量导入说明

管理后台支持导入航班、火车、酒店、房型、景点和城市资源。入口在各资源页的“导入 CSV”按钮，弹窗内可下载当前资源模板。

## 通用规则

- 文件编码使用 UTF-8，支持 UTF-8 BOM。
- 第一行必须是字段名，字段名建议直接使用下方英文名。
- 时间支持 `2026-06-01T08:00:00` 或 `2026-06-01 08:00:00`，日期使用 `2026-05-30`。
- 支持标准 CSV 引号、逗号转义和多行字段。
- “只预检，不写入数据库”会完整校验表头和每一行数据，但不会插入或更新。
- “仅新增”会逐行插入；“重复则更新”会按业务唯一键更新已有记录。
- 导入会返回总行数、成功行数、失败行数、新增/更新/预检数量，并列出最多 50 条失败原因。

## 重复更新规则

- `flights`：按 `flightNo` 匹配。
- `trains`：按 `trainNo` 匹配。
- `hotels`：按 `name + city + address` 匹配。
- `rooms`：按 `hotelId + roomType` 匹配。
- `attractions`：按 `name + city` 匹配。
- `destinations`：按 `slug` 匹配。

## 航班 flights

必填字段：`flightNo, airline, departureCity, arrivalCity, departureTime, arrivalTime, economyPrice, businessPrice, totalSeats, availableSeats`

可选字段：`status`

```csv
flightNo,airline,departureCity,arrivalCity,departureTime,arrivalTime,economyPrice,businessPrice,totalSeats,availableSeats,status
CA1001,中国国际航空,北京,上海,2026-06-01T08:00:00,2026-06-01T10:00:00,680,2180,200,120,1
```

## 火车 trains

必填字段：`trainNo, trainType, departureStation, arrivalStation, departureTime, arrivalTime, firstClassPrice, secondClassPrice, firstClassSeats, secondClassSeats`

可选字段：`durationMinutes, status`

```csv
trainNo,trainType,departureStation,arrivalStation,departureTime,arrivalTime,durationMinutes,firstClassPrice,secondClassPrice,firstClassSeats,secondClassSeats,status
G1001,G,北京南,上海虹桥,2026-06-01T08:00:00,2026-06-01T12:30:00,270,880,553,80,420,1
```

## 酒店 hotels

必填字段：`name, city, address, starRating, avgPrice`

可选字段：`description, coverImg, lat, lng, score, status`

```csv
name,city,address,description,coverImg,lat,lng,starRating,avgPrice,score,status
城市花园酒店,上海,上海市黄浦区示例路1号,近地铁商务酒店,https://example.com/hotel.jpg,31.2304,121.4737,4,520,4.6,1
```

## 房型 rooms

必填字段：`hotelId, roomType, bedType, price, totalRooms, availableRooms`

可选字段：`area, images, facilities, status`

```csv
hotelId,roomType,bedType,area,price,totalRooms,availableRooms,images,facilities,status
1,豪华大床房,1张大床,38,688,20,12,"[""/images/room.jpg""]","[""早餐"",""洗衣房""]",1
```

## 景点 attractions

必填字段：`name, city, address, adultPrice, childPrice, totalTickets, availableTickets`

可选字段：`description, coverImg, openTime, lat, lng, officialUrl, sourceName, dataCheckedDate, status`

```csv
name,city,address,description,coverImg,adultPrice,childPrice,totalTickets,availableTickets,openTime,lat,lng,officialUrl,sourceName,dataCheckedDate,status
示例景区,杭州,杭州市示例路1号,城市观光景区,https://example.com/scenic.jpg,80,40,1000,800,08:00-18:00,30.2741,120.1551,https://example.com,景区官网,2026-05-30,1
```

## 城市 destinations

必填字段：`slug, name, tag, img, desc, intro`

可选字段：`country, keywords, highlights, culture, bestSeason, transport, sourceName, sourceUrl, sortOrder, status`

```csv
slug,name,country,tag,keywords,img,desc,intro,highlights,culture,bestSeason,transport,sourceName,sourceUrl,sortOrder,status
dali,大理,中国,风花雪月,洱海|古城|苍山,/images/seed/dali.svg,苍山洱海与古城生活交织,适合慢旅行的城市,洱海骑行|古城夜游,白族文化,春秋季,高铁到大理站,公开旅游资料,https://example.com,90,1
```
