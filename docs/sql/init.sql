-- 数据库创建
CREATE DATABASE IF NOT EXISTS `travelmate` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `travelmate`;

-- 1. 用户表 (User)
CREATE TABLE `tm_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `role` TINYINT(1) DEFAULT '0' COMMENT '角色: 0-普通用户, 1-超级管理员',
  `status` TINYINT(1) DEFAULT '1' COMMENT '状态: 0-禁用, 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 航班信息模拟表 (Flight)
CREATE TABLE `tm_flight` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `flight_no` VARCHAR(20) NOT NULL COMMENT '航班号',
  `airline` VARCHAR(50) NOT NULL COMMENT '航司名称',
  `departure_city` VARCHAR(50) NOT NULL COMMENT '出发城市',
  `arrival_city` VARCHAR(50) NOT NULL COMMENT '到达城市',
  `departure_time` DATETIME NOT NULL COMMENT '起飞时间',
  `arrival_time` DATETIME NOT NULL COMMENT '降落时间',
  `economy_price` DECIMAL(10,2) NOT NULL COMMENT '经济舱价格',
  `business_price` DECIMAL(10,2) NOT NULL COMMENT '公务舱价格',
  `total_seats` INT NOT NULL DEFAULT '200' COMMENT '总座位数',
  `available_seats` INT NOT NULL DEFAULT '200' COMMENT '余票',
  `status` TINYINT(1) DEFAULT '1' COMMENT '1-正常 0-取消',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='航班信息表';

-- 3. 酒店信息表 (Hotel)
CREATE TABLE `tm_hotel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '酒店名称',
  `city` VARCHAR(50) NOT NULL COMMENT '所在城市',
  `address` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `star_rating` TINYINT(1) DEFAULT '3' COMMENT '星级',
  `description` TEXT COMMENT '酒店介绍',
  `cover_img` VARCHAR(255) COMMENT '封面图',
  `lat` DECIMAL(10,6) COMMENT '纬度',
  `lng` DECIMAL(10,6) COMMENT '经度',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店基础信息表';

-- 4. 火车票信息表 (Train) 成员A负责
CREATE TABLE `tm_train` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `train_no` VARCHAR(20) NOT NULL COMMENT '车次号',
  `train_type` VARCHAR(20) NOT NULL COMMENT '列车类型(高铁/动车/普快)',
  `departure_station` VARCHAR(50) NOT NULL COMMENT '出发站',
  `arrival_station` VARCHAR(50) NOT NULL COMMENT '到达站',
  `departure_time` DATETIME NOT NULL COMMENT '出发时间',
  `arrival_time` DATETIME NOT NULL COMMENT '到达时间',
  `duration_minutes` INT COMMENT '历时(分钟)',
  `first_class_price` DECIMAL(10,2) COMMENT '一等座价格',
  `second_class_price` DECIMAL(10,2) NOT NULL COMMENT '二等座价格',
  `first_class_seats` INT DEFAULT '50' COMMENT '一等座余票',
  `second_class_seats` INT DEFAULT '300' COMMENT '二等座余票',
  `status` TINYINT(1) DEFAULT '1' COMMENT '1-正常 0-停运',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='火车票信息表';

-- 5. 交通票务订单表 (Traffic Order) 成员A负责
CREATE TABLE `tm_traffic_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `order_type` TINYINT(1) NOT NULL COMMENT '0-机票, 1-火车票',
  `ticket_id` BIGINT NOT NULL COMMENT '关联航班/火车ID',
  `seat_type` VARCHAR(20) NOT NULL COMMENT '舱位/席别',
  `passenger_name` VARCHAR(50) NOT NULL COMMENT '乘车人姓名',
  `passenger_id_card` VARCHAR(20) NOT NULL COMMENT '乘车人身份证',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `status` TINYINT(1) DEFAULT '0' COMMENT '状态: 0-待支付, 1-出票中, 2-已出票, 3-已取消, 4-已退票',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大交通票务订单表';

-- 6. 乘车人/旅客信息表 (Passenger) 成员A负责
CREATE TABLE `tm_passenger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `id_card` VARCHAR(20) NOT NULL COMMENT '身份证号/护照号',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `type` TINYINT(1) DEFAULT '0' COMMENT '0-成人, 1-儿童',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='常用旅客信息表';

-- 7. 机票/火车票价格趋势表 (Price History) 成员A负责
CREATE TABLE `tm_price_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket_id` BIGINT NOT NULL COMMENT '关联航班/火车ID',
  `ticket_type` TINYINT(1) NOT NULL COMMENT '0-机票, 1-火车票',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `lowest_price` DECIMAL(10,2) NOT NULL COMMENT '当日最低价',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_date` (`ticket_id`, `ticket_type`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史价格趋势表';

-- 后续可以根据每个组员的负责模块，继续横向水平扩充诸如表：
-- tm_hotel_order (酒店订单表 - 成员B负责)
-- tm_post (社区游记表 - 成员D负责)
-- tm_ai_plan (AI行程规划单表 - 成员C负责)
