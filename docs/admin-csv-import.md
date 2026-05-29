# 管理后台 CSV 批量导入说明

成员 E 管理后台支持在航班、火车、酒店、房型、景点、城市资源页上传 CSV。第一行必须是字段名，时间字段使用 ISO 格式，如 `2026-06-01T08:00:00`。

## 航班 flights

```csv
flightNo,airline,departureCity,arrivalCity,departureTime,arrivalTime,economyPrice,businessPrice,totalSeats,availableSeats,status
CA1001,中国国际航空,北京,上海,2026-06-01T08:00:00,2026-06-01T10:00:00,680,2180,200,120,1
```

## 火车 trains

```csv
trainNo,trainType,departureStation,arrivalStation,departureTime,arrivalTime,firstClassPrice,secondClassPrice,firstClassSeats,secondClassSeats,status
G1001,G,北京南,上海虹桥,2026-06-01T08:00:00,2026-06-01T12:30:00,880,553,80,420,1
```

## 酒店 hotels

```csv
name,city,address,description,coverImg,starRating,avgPrice,score,status
城市花园酒店,上海,上海市黄浦区示例路1号,近地铁商务酒店,https://example.com/hotel.jpg,4,520,4.6,1
```

## 房型 rooms

```csv
hotelId,roomType,bedType,area,price,totalRooms,availableRooms,status
1,豪华大床房,1张大床,38,688,20,12,1
```

## 景点 attractions

```csv
name,city,address,description,coverImg,adultPrice,childPrice,totalTickets,availableTickets,openTime,lat,lng,officialUrl,sourceName,dataCheckedDate,status
示例景区,杭州,杭州市示例路1号,城市观光景区,https://example.com/scenic.jpg,80,40,1000,800,08:00-18:00,30.2741,120.1551,https://example.com,景区官网,2026-05-23,1
```

## 城市 destinations

`slug` 重复时会覆盖更新原城市资料；`keywords` 和 `highlights` 可用竖线分隔。

```csv
slug,name,country,tag,keywords,img,desc,intro,highlights,culture,bestSeason,transport,sourceName,sourceUrl,sortOrder,status
dali,大理,中国,风花雪月,洱海|古城|苍山,/images/seed/dali.svg,苍山洱海与古城生活交织，适合慢旅行。,大理适合围绕洱海、古城和苍山安排两到三天行程。,洱海适合骑行或自驾|大理古城适合夜间散步|苍山适合晴天登高,大理旅行节奏宜慢，适合把自然风景和古城街区拆开安排。,春秋季气候舒适，夏季注意雨水。,可通过大理站衔接高铁，市内建议打车或租车分区游览。,公开旅游资料,https://example.com,90,1
```
