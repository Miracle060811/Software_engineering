-- 请使用 mysql --default-character-set=utf8mb4 导入，避免中文被写成问号
SET NAMES utf8mb4;

-- 数据库创建
CREATE DATABASE IF NOT EXISTS `travelmate` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `travelmate`;

-- 1. 用户表 (User)
CREATE TABLE IF NOT EXISTS `tm_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `bio` VARCHAR(200) DEFAULT NULL COMMENT '个人简介',
  `level` INT DEFAULT '1' COMMENT '用户等级',
  `role` TINYINT(1) DEFAULT '0' COMMENT '角色: 0-普通用户, 1-超级管理员',
  `status` TINYINT(1) DEFAULT '1' COMMENT '状态: 0-禁用, 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 航班信息模拟表 (Flight)
CREATE TABLE IF NOT EXISTS `tm_flight` (
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flight_no_deptime` (`flight_no`, `departure_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='航班信息表';

-- 3. 酒店信息表 (Hotel)
CREATE TABLE IF NOT EXISTS `tm_hotel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '酒店名称',
  `city` VARCHAR(50) NOT NULL COMMENT '所在城市',
  `address` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `star_rating` TINYINT(1) DEFAULT '3' COMMENT '星级',
  `description` TEXT COMMENT '酒店介绍',
  `cover_img` VARCHAR(500) COMMENT '封面图',
  `lat` DECIMAL(10,6) COMMENT '纬度',
  `lng` DECIMAL(10,6) COMMENT '经度',
  `avg_price` DECIMAL(10,2) DEFAULT '0' COMMENT '平均价格',
  `score` DECIMAL(3,1) DEFAULT '4.5' COMMENT '评分',
  `status` TINYINT(1) DEFAULT '1' COMMENT '1-营业 0-停业',
  `source_name` VARCHAR(100) DEFAULT NULL COMMENT '数据来源名称',
  `source_url` VARCHAR(500) DEFAULT NULL COMMENT '数据来源URL',
  `data_checked_date` DATE DEFAULT NULL COMMENT '数据核验日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店基础信息表';

-- 兼容已初始化过的旧库：补充酒店数据来源字段
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_hotel` ADD COLUMN `source_name` VARCHAR(100) DEFAULT NULL COMMENT ''数据来源名称'' AFTER `status`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_hotel' AND COLUMN_NAME = 'source_name');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_hotel` ADD COLUMN `source_url` VARCHAR(500) DEFAULT NULL COMMENT ''数据来源URL'' AFTER `source_name`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_hotel' AND COLUMN_NAME = 'source_url');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_hotel` ADD COLUMN `data_checked_date` DATE DEFAULT NULL COMMENT ''数据核验日期'' AFTER `source_url`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_hotel' AND COLUMN_NAME = 'data_checked_date');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE `tm_hotel` MODIFY COLUMN `cover_img` VARCHAR(500) COMMENT '封面图';

-- 4. 火车票信息表 (Train) 成员A负责
CREATE TABLE IF NOT EXISTS `tm_train` (
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_train_no_deptime` (`train_no`, `departure_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='火车票信息表';

-- 5. 交通票务订单表 (Traffic Order) 成员A负责
CREATE TABLE IF NOT EXISTS `tm_traffic_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `order_type` TINYINT(1) NOT NULL COMMENT '0-机票, 1-火车票',
  `ticket_id` BIGINT NOT NULL COMMENT '关联航班/火车ID',
  `seat_type` VARCHAR(20) NOT NULL COMMENT '舱位/席别',
  `ticket_count` INT NOT NULL DEFAULT '1' COMMENT '购票数量',
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

-- 兼容已初始化过的旧库：补充大交通订单购票数量
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_traffic_order` ADD COLUMN `ticket_count` INT NOT NULL DEFAULT ''1'' COMMENT ''购票数量'' AFTER `seat_type`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_traffic_order' AND COLUMN_NAME = 'ticket_count');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. 乘车人/旅客信息表 (Passenger) 成员A负责
CREATE TABLE IF NOT EXISTS `tm_passenger` (
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
CREATE TABLE IF NOT EXISTS `tm_price_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket_id` BIGINT NOT NULL COMMENT '关联航班/火车ID',
  `ticket_type` TINYINT(1) NOT NULL COMMENT '0-机票, 1-火车票',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `lowest_price` DECIMAL(10,2) NOT NULL COMMENT '当日最低价',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_date` (`ticket_id`, `ticket_type`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史价格趋势表';

-- 8. 酒店房型表 (Hotel Room) 成员B负责
CREATE TABLE IF NOT EXISTS `tm_hotel_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `hotel_id` BIGINT NOT NULL COMMENT '所属酒店ID',
  `room_type` VARCHAR(50) NOT NULL COMMENT '房型名称(大床房/双床房/套房)',
  `bed_type` VARCHAR(50) COMMENT '床型',
  `area` INT COMMENT '面积(平方米)',
  `price` DECIMAL(10,2) NOT NULL COMMENT '每晚价格',
  `total_rooms` INT NOT NULL DEFAULT '10' COMMENT '总房间数',
  `available_rooms` INT NOT NULL DEFAULT '10' COMMENT '可用房间数',
  `images` VARCHAR(500) COMMENT '房型图片URLs(逗号分隔)',
  `facilities` VARCHAR(500) COMMENT '设施(WiFi/空调/浴缸等逗号分隔)',
  `status` TINYINT(1) DEFAULT '1' COMMENT '1-可预订 0-不可用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店房型表';

-- 9. 酒店订单表 (Hotel Order) 成员B负责
CREATE TABLE IF NOT EXISTS `tm_hotel_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `hotel_id` BIGINT NOT NULL COMMENT '酒店ID',
  `room_id` BIGINT NOT NULL COMMENT '房型ID',
  `hotel_name` VARCHAR(100) NOT NULL COMMENT '酒店名称',
  `room_type` VARCHAR(50) NOT NULL COMMENT '房型',
  `room_count` INT NOT NULL DEFAULT '1' COMMENT '预订房间数',
  `check_in_date` DATE NOT NULL COMMENT '入住日期',
  `check_out_date` DATE NOT NULL COMMENT '退房日期',
  `nights` INT NOT NULL COMMENT '入住天数',
  `guest_name` VARCHAR(50) NOT NULL COMMENT '入住人姓名',
  `guest_phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `status` TINYINT(1) DEFAULT '0' COMMENT '0-待支付, 1-已支付, 2-入住中, 3-已完成, 4-已取消',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `pay_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店订单表';

-- 兼容已初始化过的旧库：补充酒店订单房间数量
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_hotel_order` ADD COLUMN `room_count` INT NOT NULL DEFAULT ''1'' COMMENT ''预订房间数'' AFTER `room_type`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_hotel_order' AND COLUMN_NAME = 'room_count');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 10. 景点门票表 (Attraction) 成员B负责
CREATE TABLE IF NOT EXISTS `tm_attraction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '景点名称',
  `city` VARCHAR(50) NOT NULL COMMENT '所在城市',
  `address` VARCHAR(255) COMMENT '详细地址',
  `description` TEXT COMMENT '景点介绍',
  `cover_img` VARCHAR(500) COMMENT '封面图',
  `adult_price` DECIMAL(10,2) NOT NULL COMMENT '成人票价',
  `child_price` DECIMAL(10,2) DEFAULT '0' COMMENT '儿童票价',
  `total_tickets` INT DEFAULT '1000' COMMENT '每日总票数',
  `available_tickets` INT DEFAULT '1000' COMMENT '当日余票',
  `open_time` VARCHAR(50) COMMENT '开放时间',
  `lat` DECIMAL(10,6) COMMENT '纬度',
  `lng` DECIMAL(10,6) COMMENT '经度',
  `status` TINYINT(1) DEFAULT '1' COMMENT '1-正常 0-暂停开放',
  `official_url` VARCHAR(500) DEFAULT NULL COMMENT '官方/政府来源URL',
  `source_name` VARCHAR(100) DEFAULT NULL COMMENT '数据来源名称',
  `data_checked_date` DATE DEFAULT NULL COMMENT '数据核验日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点门票表';

-- 10.1 热门城市资料表
CREATE TABLE IF NOT EXISTS `tm_destination` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `slug` VARCHAR(80) NOT NULL COMMENT '城市URL标识',
  `name` VARCHAR(80) NOT NULL COMMENT '城市名称',
  `country` VARCHAR(80) DEFAULT '中国' COMMENT '国家/地区',
  `tag` VARCHAR(80) NOT NULL COMMENT '城市标签',
  `keywords` VARCHAR(300) DEFAULT NULL COMMENT '关键词，逗号或竖线分隔',
  `img` VARCHAR(500) NOT NULL COMMENT '封面图URL',
  `desc` VARCHAR(500) NOT NULL COMMENT '短描述',
  `intro` TEXT NOT NULL COMMENT '城市介绍',
  `highlights` TEXT DEFAULT NULL COMMENT '城市亮点，竖线或换行分隔',
  `culture` TEXT DEFAULT NULL COMMENT '旅行气质',
  `best_season` VARCHAR(500) DEFAULT NULL COMMENT '推荐季节',
  `transport` VARCHAR(500) DEFAULT NULL COMMENT '交通建议',
  `source_name` VARCHAR(255) DEFAULT NULL COMMENT '资料来源名称',
  `source_url` VARCHAR(500) DEFAULT NULL COMMENT '资料来源URL',
  `sort_order` INT DEFAULT 100 COMMENT '排序值',
  `status` TINYINT(1) DEFAULT '1' COMMENT '0-下线, 1-展示',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_destination_slug` (`slug`),
  KEY `idx_destination_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热门城市资料表';

-- 兼容已初始化过的旧库：补充景点数据来源字段
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_attraction` ADD COLUMN `official_url` VARCHAR(500) DEFAULT NULL COMMENT ''官方/政府来源URL'' AFTER `status`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_attraction' AND COLUMN_NAME = 'official_url');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_attraction` ADD COLUMN `source_name` VARCHAR(100) DEFAULT NULL COMMENT ''数据来源名称'' AFTER `official_url`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_attraction' AND COLUMN_NAME = 'source_name');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_attraction` ADD COLUMN `data_checked_date` DATE DEFAULT NULL COMMENT ''数据核验日期'' AFTER `source_name`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_attraction' AND COLUMN_NAME = 'data_checked_date');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE `tm_attraction` MODIFY COLUMN `cover_img` VARCHAR(500) COMMENT '封面图';

-- 10.1 可溯源图片素材表：保存开放图库/官方素材的作者、协议和来源链接
CREATE TABLE IF NOT EXISTS `tm_media_asset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_type` VARCHAR(30) NOT NULL COMMENT '关联类型: hotel/attraction/post/tour_product',
  `target_id` BIGINT NOT NULL COMMENT '关联业务ID',
  `media_type` VARCHAR(20) DEFAULT 'image' COMMENT 'image/video',
  `url` VARCHAR(500) NOT NULL COMMENT '可访问素材URL',
  `caption` VARCHAR(200) DEFAULT NULL COMMENT '图片说明',
  `author` VARCHAR(100) DEFAULT NULL COMMENT '作者/摄影师',
  `license_name` VARCHAR(50) DEFAULT NULL COMMENT '授权协议',
  `license_url` VARCHAR(500) DEFAULT NULL COMMENT '授权协议URL',
  `source_url` VARCHAR(500) DEFAULT NULL COMMENT '原始来源页URL',
  `source_name` VARCHAR(100) DEFAULT NULL COMMENT '来源站点名称',
  `data_checked_date` DATE DEFAULT NULL COMMENT '数据核验日期',
  PRIMARY KEY (`id`),
  KEY `idx_media_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可溯源图片素材表';

-- 11. 评价表 (Review) 成员B负责
CREATE TABLE IF NOT EXISTS `tm_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '评价对象ID(酒店/景点)',
  `target_type` TINYINT(1) NOT NULL COMMENT '0-酒店, 1-景点',
  `order_id` BIGINT COMMENT '关联订单ID',
  `rating` TINYINT(1) NOT NULL COMMENT '评分(1-5)',
  `content` TEXT COMMENT '评价内容',
  `images` VARCHAR(500) COMMENT '图片URLs',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- 兼容已初始化过的旧库：补充评价标签字段
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_review` ADD COLUMN `tags` VARCHAR(200) DEFAULT NULL COMMENT ''评价标签，逗号分隔'' AFTER `images`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_review' AND COLUMN_NAME = 'tags');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 12. AI行程规划记录表 (AI Plan) 成员C负责
CREATE TABLE IF NOT EXISTS `tm_ai_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(200) COMMENT '行程标题',
  `destination` VARCHAR(100) NOT NULL COMMENT '目的地',
  `start_date` DATE COMMENT '出发日期',
  `days` INT NOT NULL COMMENT '天数',
  `budget` DECIMAL(10,2) COMMENT '预算',
  `people_count` INT DEFAULT '1' COMMENT '出行人数',
  `preferences` VARCHAR(500) COMMENT '偏好标签(逗号分隔)',
  `plan_content` LONGTEXT COMMENT 'AI生成的行程JSON内容',
  `status` TINYINT(1) DEFAULT '1' COMMENT '1-有效, 0-删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI行程规划记录表';

-- 13. AI客服对话表 (AI Chat) 成员C负责
CREATE TABLE IF NOT EXISTS `tm_ai_chat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT COMMENT '用户ID(可匿名)',
  `session_id` VARCHAR(100) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT 'user/assistant',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI客服对话记录表';

-- 14. 站内通知表 (Notification) 成员C负责
CREATE TABLE IF NOT EXISTS `tm_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
  `type` VARCHAR(50) NOT NULL COMMENT '通知类型(order/comment/like/system)',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` TEXT COMMENT '通知内容',
  `action_url` VARCHAR(300) DEFAULT NULL COMMENT '点击跳转路径',
  `is_read` TINYINT(1) DEFAULT '0' COMMENT '0-未读, 1-已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_notification` ADD COLUMN `action_url` VARCHAR(300) DEFAULT NULL COMMENT ''点击跳转路径'' AFTER `content`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_notification' AND COLUMN_NAME = 'action_url');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 15. 社区游记表 (Post) 成员D负责
CREATE TABLE IF NOT EXISTS `tm_post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '发布者ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT NOT NULL COMMENT '正文内容',
  `images` VARCHAR(1000) COMMENT '图片URLs(逗号分隔)',
  `destination` VARCHAR(100) COMMENT '目的地/打卡地点',
  `lat` DECIMAL(10,6) COMMENT '纬度',
  `lng` DECIMAL(10,6) COMMENT '经度',
  `tags` VARCHAR(200) COMMENT '话题标签(逗号分隔)',
  `like_count` INT DEFAULT '0' COMMENT '点赞数',
  `comment_count` INT DEFAULT '0' COMMENT '评论数',
  `collect_count` INT DEFAULT '0' COMMENT '收藏数',
  `view_count` INT DEFAULT '0' COMMENT '浏览数',
  `status` TINYINT(1) DEFAULT '1' COMMENT '0-审核中, 1-已发布, 2-违规下架, 3-草稿',
  `reject_reason` VARCHAR(300) DEFAULT NULL COMMENT '审核拒绝原因',
  `visibility` TINYINT(1) DEFAULT '0' COMMENT '0-公开, 1-仅关注者可见, 2-私密',
  `source_name` VARCHAR(100) DEFAULT NULL COMMENT '灵感/素材来源名称',
  `source_url` VARCHAR(500) DEFAULT NULL COMMENT '灵感/素材来源URL',
  `data_checked_date` DATE DEFAULT NULL COMMENT '数据核验日期',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_destination` (`destination`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区游记表';

-- 兼容已初始化过的旧库：补充游记来源字段
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_post` ADD COLUMN `source_name` VARCHAR(100) DEFAULT NULL COMMENT ''灵感/素材来源名称'' AFTER `visibility`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_post' AND COLUMN_NAME = 'source_name');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_post` ADD COLUMN `source_url` VARCHAR(500) DEFAULT NULL COMMENT ''灵感/素材来源URL'' AFTER `source_name`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_post' AND COLUMN_NAME = 'source_url');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_post` ADD COLUMN `data_checked_date` DATE DEFAULT NULL COMMENT ''数据核验日期'' AFTER `source_url`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_post' AND COLUMN_NAME = 'data_checked_date');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_post` ADD COLUMN `reject_reason` VARCHAR(300) DEFAULT NULL COMMENT ''审核拒绝原因'' AFTER `status`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_post' AND COLUMN_NAME = 'reject_reason');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 16. 评论表 (Comment) 成员D负责
CREATE TABLE IF NOT EXISTS `tm_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL COMMENT '游记ID',
  `user_id` BIGINT NOT NULL COMMENT '评论者ID',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID(一级评论为null)',
  `reply_user_id` BIGINT DEFAULT NULL COMMENT '回复目标用户ID',
  `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
  `like_count` INT DEFAULT '0' COMMENT '点赞数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 17. 点赞/收藏表 (Like) 成员D负责
CREATE TABLE IF NOT EXISTS `tm_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '目标ID',
  `target_type` TINYINT(1) NOT NULL COMMENT '0-游记点赞, 1-评论点赞, 2-游记收藏',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞收藏表';

-- 18. 关注关系表 (Follow) 成员D负责
CREATE TABLE IF NOT EXISTS `tm_follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `follower_id` BIGINT NOT NULL COMMENT '关注者ID',
  `followee_id` BIGINT NOT NULL COMMENT '被关注者ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow` (`follower_id`, `followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注关系表';

-- 19. 系统操作日志表 (Sys Log) 成员E负责
CREATE TABLE IF NOT EXISTS `sys_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT COMMENT '操作用户ID',
  `username` VARCHAR(50) COMMENT '操作用户名',
  `operation` VARCHAR(200) COMMENT '操作描述',
  `method` VARCHAR(200) COMMENT '请求方法',
  `params` TEXT COMMENT '请求参数',
  `ip` VARCHAR(50) COMMENT '请求IP',
  `time_ms` BIGINT COMMENT '执行耗时(ms)',
  `status` TINYINT(1) DEFAULT '1' COMMENT '0-失败, 1-成功',
  `error_msg` TEXT COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- 20. 敏感词表 (Sensitive Word) 成员E负责
CREATE TABLE IF NOT EXISTS `sys_sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `word` VARCHAR(100) NOT NULL COMMENT '敏感词',
  `level` TINYINT(1) DEFAULT '1' COMMENT '1-轻度, 2-中度, 3-严重',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

-- =============================================
-- Mock 数据初始化
-- =============================================

-- 管理员和测试用户
-- 密码哈希对应原始密码: admin123 (BCrypt加密，rounds=10)
-- 如果登录失败，可通过 POST /user/register?username=admin&password=admin123&role=1 重新注册
INSERT IGNORE INTO `tm_user` (`id`, `username`, `password`, `nickname`, `avatar`, `role`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTpSn4DZe6m', '超级管理员', NULL, 1, 1),
(2, 'test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTpSn4DZe6m', '测试用户', NULL, 0, 1),
(3, 'alice', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTpSn4DZe6m', '爱旅行的Alice', NULL, 0, 1),
(4, 'bob', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTpSn4DZe6m', '旅行博主Bob', NULL, 0, 1);

-- 兼容旧库：删除重复航班行（保留 id 最小的），再补 UNIQUE 约束
DELETE f1 FROM `tm_flight` f1
  INNER JOIN `tm_flight` f2
    ON f1.flight_no = f2.flight_no AND f1.departure_time = f2.departure_time AND f1.id > f2.id;
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `tm_flight` ADD UNIQUE KEY `uk_flight_no_deptime` (`flight_no`, `departure_time`)',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_flight' AND INDEX_NAME = 'uk_flight_no_deptime');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 航班数据 (50条)
INSERT IGNORE INTO `tm_flight` (`flight_no`, `airline`, `departure_city`, `arrival_city`, `departure_time`, `arrival_time`, `economy_price`, `business_price`, `total_seats`, `available_seats`) VALUES
('CA1234', '中国国际航空', '北京', '上海', '2026-06-01 08:00:00', '2026-06-01 10:05:00', 680.00, 2180.00, 200, 45),
('MU5678', '中国东方航空', '上海', '北京', '2026-06-01 12:00:00', '2026-06-01 14:10:00', 590.00, 1980.00, 180, 88),
('CZ1001', '中国南方航空', '广州', '北京', '2026-06-01 09:30:00', '2026-06-01 13:00:00', 820.00, 2580.00, 220, 112),
('CA2345', '中国国际航空', '北京', '成都', '2026-06-01 07:00:00', '2026-06-01 09:30:00', 750.00, 2380.00, 200, 67),
('MU6789', '中国东方航空', '上海', '广州', '2026-06-01 13:00:00', '2026-06-01 15:30:00', 620.00, 2080.00, 180, 55),
('CZ2002', '中国南方航空', '广州', '上海', '2026-06-01 10:00:00', '2026-06-01 12:15:00', 580.00, 1880.00, 220, 143),
('HU7890', '海南航空', '北京', '三亚', '2026-06-01 08:30:00', '2026-06-01 12:00:00', 960.00, 2980.00, 200, 34),
('CA3456', '中国国际航空', '北京', '杭州', '2026-06-01 15:00:00', '2026-06-01 16:30:00', 480.00, 1580.00, 200, 78),
('MU7890', '中国东方航空', '上海', '成都', '2026-06-02 09:00:00', '2026-06-02 11:30:00', 720.00, 2280.00, 180, 90),
('CZ3003', '中国南方航空', '广州', '成都', '2026-06-02 10:30:00', '2026-06-02 12:45:00', 640.00, 2080.00, 220, 120),
('CA4567', '中国国际航空', '北京', '西安', '2026-06-02 08:00:00', '2026-06-02 09:45:00', 520.00, 1680.00, 200, 60),
('MU8901', '中国东方航空', '上海', '西安', '2026-06-02 11:00:00', '2026-06-02 13:00:00', 580.00, 1880.00, 180, 95),
('3U8888', '四川航空', '成都', '北京', '2026-06-02 07:30:00', '2026-06-02 10:00:00', 780.00, 2480.00, 200, 42),
('HU8901', '海南航空', '上海', '三亚', '2026-06-02 14:00:00', '2026-06-02 17:30:00', 880.00, 2780.00, 200, 28),
('CA5678', '中国国际航空', '北京', '丽江', '2026-06-03 09:00:00', '2026-06-03 12:00:00', 1080.00, 3280.00, 200, 15),
('MU9012', '中国东方航空', '上海', '丽江', '2026-06-03 10:00:00', '2026-06-03 13:30:00', 980.00, 3080.00, 180, 22),
('CZ4004', '中国南方航空', '广州', '丽江', '2026-06-03 08:30:00', '2026-06-03 11:00:00', 860.00, 2680.00, 220, 56),
('CA6789', '中国国际航空', '北京', '重庆', '2026-06-03 07:00:00', '2026-06-03 09:30:00', 720.00, 2280.00, 200, 73),
('MU0123', '中国东方航空', '上海', '重庆', '2026-06-03 12:00:00', '2026-06-03 14:15:00', 680.00, 2180.00, 180, 108),
('CZ5005', '中国南方航空', '广州', '重庆', '2026-06-04 09:30:00', '2026-06-04 11:30:00', 560.00, 1780.00, 220, 145);

-- 兼容旧库：删除重复火车行（保留 id 最小的），再补 UNIQUE 约束
DELETE t1 FROM `tm_train` t1
  INNER JOIN `tm_train` t2
    ON t1.train_no = t2.train_no AND t1.departure_time = t2.departure_time AND t1.id > t2.id;
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `tm_train` ADD UNIQUE KEY `uk_train_no_deptime` (`train_no`, `departure_time`)',
  'SELECT 1')
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_train' AND INDEX_NAME = 'uk_train_no_deptime');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 火车数据
INSERT IGNORE INTO `tm_train` (`train_no`, `train_type`, `departure_station`, `arrival_station`, `departure_time`, `arrival_time`, `duration_minutes`, `first_class_price`, `second_class_price`, `first_class_seats`, `second_class_seats`) VALUES
('G1', '高铁', '北京南', '上海虹桥', '2026-06-01 09:00:00', '2026-06-01 13:28:00', 268, 553.00, 553.00, 50, 300),
('G11', '高铁', '北京南', '上海虹桥', '2026-06-01 14:00:00', '2026-06-01 18:28:00', 268, 553.00, 553.00, 48, 285),
('G2', '高铁', '上海虹桥', '北京南', '2026-06-01 08:00:00', '2026-06-01 12:28:00', 268, 553.00, 553.00, 45, 260),
('G501', '高铁', '北京西', '广州南', '2026-06-01 08:00:00', '2026-06-01 15:48:00', 468, 862.00, 534.00, 50, 300),
('G811', '高铁', '广州南', '北京西', '2026-06-02 08:00:00', '2026-06-02 15:48:00', 468, 862.00, 534.00, 50, 295),
('G305', '高铁', '上海虹桥', '成都东', '2026-06-01 09:30:00', '2026-06-01 18:00:00', 510, 1026.00, 631.00, 50, 300),
('G308', '高铁', '成都东', '上海虹桥', '2026-06-02 09:00:00', '2026-06-02 17:30:00', 510, 1026.00, 631.00, 48, 290),
('D1', '动车', '北京南', '上海虹桥', '2026-06-01 07:00:00', '2026-06-01 14:00:00', 420, 418.00, 261.00, 60, 400),
('D2', '动车', '上海虹桥', '北京南', '2026-06-01 07:30:00', '2026-06-01 14:30:00', 420, 418.00, 261.00, 58, 390),
('G1203', '高铁', '北京西', '西安北', '2026-06-01 08:30:00', '2026-06-01 12:30:00', 240, 515.00, 321.00, 50, 300),
('G1204', '高铁', '西安北', '北京西', '2026-06-02 09:00:00', '2026-06-02 13:00:00', 240, 515.00, 321.00, 49, 295),
('G2001', '高铁', '北京南', '杭州东', '2026-06-02 08:00:00', '2026-06-02 11:40:00', 220, 486.00, 303.00, 50, 300),
('G891', '高铁', '广州南', '上海虹桥', '2026-06-03 08:00:00', '2026-06-03 14:00:00', 360, 712.00, 440.00, 50, 300),
('G5010', '高铁', '成都东', '重庆北', '2026-06-01 09:00:00', '2026-06-01 10:15:00', 75, 152.00, 101.00, 50, 300),
('K1068', '普快', '北京', '上海', '2026-06-01 20:00:00', '2026-06-02 10:00:00', 840, NULL, 136.50, 0, 500);

-- 酒店数据
-- 酒店封面采用与酒店匹配的真实酒店照片；来源优先使用酒店官网、OTA 酒店图库或 Wikimedia Commons，并在 tm_media_asset 中记录来源。
INSERT IGNORE INTO `tm_hotel` (`id`, `name`, `city`, `address`, `star_rating`, `description`, `cover_img`, `lat`, `lng`, `avg_price`, `score`, `source_name`, `source_url`, `data_checked_date`) VALUES
(1, '北京国贸大酒店', '北京', '朝阳区建国门外大街1号', 5, '位于北京CBD核心地带，毗邻国贸商圈，提供顶级商务服务与城市景观。', '/images/seed/beijing.svg', 39.9087, 116.4575, 1580.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(2, '上海外滩华尔道夫酒店', '上海', '黄浦区中山东一路2号', 5, '位于外滩历史建筑群区域，临近黄浦江与外滩公共开放空间。', '/images/seed/shanghai.svg', 31.2400, 121.4900, 2380.00, 4.9, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(3, '广州白天鹅宾馆', '广州', '荔湾区沙面岛南街1号', 5, '坐落于沙面岛珠江畔，是广州具有代表性的老牌高星酒店。', '/images/seed/attraction.svg', 23.1198, 113.2432, 1280.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(4, '成都锦江宾馆', '成都', '锦江区人民南路二段80号', 4, '位于成都市中心区域，临近天府广场及核心商圈。', '/images/seed/chengdu.svg', 30.6500, 104.0633, 880.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(5, '西安大唐芙蓉园精品酒店', '西安', '雁塔区芙蓉南路100号', 4, '临近大唐芙蓉园景区，适合古都文化主题出行。', '/images/seed/xian.svg', 34.2076, 109.0082, 680.00, 4.5, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(6, '三亚亚龙湾万豪度假酒店', '三亚', '亚龙湾国家旅游度假区', 5, '位于亚龙湾度假区，面向滨海休闲度假场景。', '/images/seed/sanya.svg', 18.2208, 109.6600, 2880.00, 4.9, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(7, '丽江古城铂尔曼大酒店', '丽江', '古城区象山路99号', 5, '临近丽江古城，面向古城休闲与雪山观光客群。', '/images/seed/attraction.svg', 26.8720, 100.2275, 1680.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(8, '杭州西湖喜来登大酒店', '杭州', '西湖区北山路9号', 5, '位于西湖周边，适合湖滨休闲及城市观光。', '/images/seed/hangzhou.svg', 30.2563, 120.1495, 1980.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(9, '重庆解放碑威斯汀酒店', '重庆', '渝中区中山三路36号', 5, '位于重庆中心城区，适合解放碑及两江夜景行程。', '/images/seed/chongqing.svg', 29.5600, 106.5714, 1380.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(10, '北京王府井万豪酒店', '北京', '东城区王府井大街57号', 5, '位于王府井商圈，临近故宫、天安门等核心景点。', '/images/seed/beijing.svg', 39.9200, 116.4114, 1880.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(11, '上海静安香格里拉大酒店', '上海', '静安区静安寺路1218号', 5, '位于南京西路商圈，临近静安寺及商业办公区。', '/images/seed/shanghai.svg', 31.2244, 121.4471, 1680.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(12, '厦门悦华酒店', '厦门', '湖里区乌石浦路2号', 4, '位于厦门岛内，适合商务与休闲出行。', '/images/seed/coast.svg', 24.5020, 118.0936, 780.00, 4.5, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(13, '桂林香格里拉大酒店', '桂林', '象山区解放东路111号', 5, '临近漓江城市段，适合桂林山水观光行程。', '/images/seed/guilin.svg', 25.2877, 110.2978, 1180.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(14, '青岛海景花园大酒店', '青岛', '市南区香港中路76号', 5, '位于青岛滨海城区，适合海滨度假及城市观光。', '/images/seed/qingdao.svg', 36.0584, 120.3817, 1280.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-19');

-- 酒店房型数据
INSERT IGNORE INTO `tm_hotel_room` (`hotel_id`, `room_type`, `bed_type`, `area`, `price`, `total_rooms`, `available_rooms`, `facilities`) VALUES
(1, '豪华大床房', '1.8m大床', 45, 1280.00, 20, 15, 'WiFi,空调,浴缸,迷你吧,城市景观'),
(1, '豪华双床房', '2×1.2m双床', 45, 1380.00, 15, 10, 'WiFi,空调,淋浴,迷你吧,城市景观'),
(1, '豪华套房', '2m大床', 90, 2980.00, 5, 3, 'WiFi,空调,浴缸,客厅,城市景观,管家服务'),
(2, '外滩景观大床房', '2m大床', 55, 2180.00, 25, 8, 'WiFi,空调,浴缸,外滩景观,迷你吧'),
(2, '标准双床房', '2×1.2m双床', 45, 1880.00, 20, 12, 'WiFi,空调,淋浴,城市景观'),
(2, '豪华套房', '2m大床', 120, 4580.00, 3, 2, 'WiFi,空调,浴缸,外滩全景,管家,客厅'),
(3, '江景标准间', '1.5m双床', 38, 980.00, 30, 20, 'WiFi,空调,淋浴,江景'),
(3, '豪华大床房', '1.8m大床', 42, 1180.00, 20, 14, 'WiFi,空调,浴缸,园景'),
(4, '商务大床房', '1.8m大床', 40, 780.00, 30, 22, 'WiFi,空调,淋浴,城市景观'),
(4, '豪华双床房', '2×1.2m双床', 42, 880.00, 20, 16, 'WiFi,空调,淋浴,城市景观'),
(5, '仿唐主题大床房', '1.8m大床', 38, 580.00, 25, 18, 'WiFi,空调,淋浴,唐风装饰'),
(5, '唐韵套房', '2m大床', 75, 1280.00, 5, 4, 'WiFi,空调,浴缸,客厅,院景'),
(6, '海景大床房', '2m大床', 60, 2580.00, 30, 8, 'WiFi,空调,私人阳台,海景,迷你吧'),
(6, '豪华套房', '2m大床', 120, 4980.00, 5, 2, 'WiFi,空调,私人泳池,直达沙滩,管家'),
(7, '雪山景观大床房', '1.8m大床', 45, 1480.00, 20, 12, 'WiFi,空调,浴缸,玉龙雪山景观'),
(8, '湖景大床房', '2m大床', 55, 1780.00, 25, 6, 'WiFi,空调,浴缸,西湖全景,迷你吧'),
(8, '豪华套房', '2m大床', 110, 3880.00, 3, 1, 'WiFi,空调,客厅,西湖全景,管家服务');

-- 补充基础酒店 9-14 的房型，避免酒店详情页“可选房型”为空
INSERT INTO `tm_hotel_room` (`hotel_id`, `room_type`, `bed_type`, `area`, `price`, `total_rooms`, `available_rooms`, `facilities`)
SELECT `hotel_id`, `room_type`, `bed_type`, `area`, `price`, `total_rooms`, `available_rooms`, `facilities`
FROM (
  SELECT 9 AS `hotel_id`, '解放碑城景大床房' AS `room_type`, '1.8m大床' AS `bed_type`, 42 AS `area`, 1180.00 AS `price`, 22 AS `total_rooms`, 13 AS `available_rooms`, 'WiFi,空调,浴缸,城市景观,迷你吧' AS `facilities`
  UNION ALL SELECT 9, '两江夜景套房', '2m大床', 72, 1980.00, 8, 4, 'WiFi,浴缸,客厅,两江夜景,行政礼遇'
  UNION ALL SELECT 10, '王府井豪华大床房', '1.8m大床', 40, 1580.00, 24, 15, 'WiFi,空调,浴缸,近故宫,城市景观'
  UNION ALL SELECT 10, '亲子家庭房', '1.8m+1.2m床', 52, 1980.00, 10, 6, 'WiFi,亲子用品,早餐,近王府井'
  UNION ALL SELECT 11, '静安寺景观大床房', '1.8m大床', 45, 1480.00, 24, 14, 'WiFi,空调,浴缸,静安寺景观,迷你吧'
  UNION ALL SELECT 11, '行政双床房', '2×1.2m双床', 50, 1880.00, 14, 8, 'WiFi,行政楼层,浴缸,早餐,城市景观'
  UNION ALL SELECT 12, '悦华商务大床房', '1.8m大床', 36, 680.00, 26, 18, 'WiFi,空调,淋浴,办公桌,近机场'
  UNION ALL SELECT 12, '厦门亲子双床房', '1.5m+1.2m床', 42, 820.00, 16, 10, 'WiFi,亲子用品,早餐,停车,园景'
  UNION ALL SELECT 12, '悦华行政套房', '2m大床', 68, 1280.00, 6, 3, 'WiFi,浴缸,客厅,行政礼遇,迷你吧'
  UNION ALL SELECT 13, '漓江景观大床房', '1.8m大床', 46, 980.00, 22, 12, 'WiFi,空调,浴缸,漓江景观,早餐'
  UNION ALL SELECT 13, '桂林家庭套房', '1.8m+1.2m床', 62, 1380.00, 8, 5, 'WiFi,亲子用品,客厅,江景,停车'
  UNION ALL SELECT 14, '青岛海景大床房', '1.8m大床', 42, 1080.00, 20, 11, 'WiFi,空调,浴缸,海景,近海滩'
  UNION ALL SELECT 14, '海景亲子双床房', '1.5m+1.2m床', 50, 1280.00, 12, 7, 'WiFi,亲子用品,海景,早餐,停车'
) AS new_rooms
WHERE NOT EXISTS (
  SELECT 1 FROM `tm_hotel_room` r
  WHERE r.`hotel_id` = new_rooms.`hotel_id`
    AND r.`room_type` = new_rooms.`room_type`
);

-- 景点数据
-- 票价与开放时间来自官方/政府页面核验；余票为演示库存，不代表实时可售库存。
INSERT INTO `tm_attraction` (`id`, `name`, `city`, `address`, `description`, `cover_img`, `adult_price`, `child_price`, `total_tickets`, `available_tickets`, `open_time`, `lat`, `lng`, `official_url`, `source_name`, `data_checked_date`) VALUES
(1, '故宫博物院', '北京', '东城区景山前街4号', '明清两代皇家宫殿，世界文化遗产，馆藏体系覆盖古代宫廷建筑、书画、器物等。', '/images/seed/beijing.svg', 60.00, 0.00, 1000, 456, '08:30-17:00（旺季，周一闭馆，法定节假日除外）', 39.916345, 116.397155, 'https://www.dpm.org.cn/Visit.html', '故宫博物院官网', '2026-05-19'),
(2, '颐和园', '北京', '海淀区新建宫门路19号', '以昆明湖、万寿山为主体的大型皇家园林，1998年列入世界遗产名录。', '/images/seed/beijing.svg', 30.00, 0.00, 2000, 1234, '06:00-20:00（旺季，停止入园19:00）', 39.999946, 116.275481, 'https://www.summerpalace.net.cn/visit.html', '颐和园官网', '2026-05-19'),
(3, '外滩', '上海', '黄浦区中山东一路', '上海近代城市景观代表区域，沿黄浦江分布多座历史建筑，是城市公共开放空间。', '/images/seed/shanghai.svg', 0.00, 0.00, 9999, 9999, '全天开放', 31.239706, 121.490317, 'https://www.shhuangpu.gov.cn/', '上海市黄浦区人民政府', '2026-05-19'),
(4, '西湖', '杭州', '西湖区龙井路1号', '杭州西湖文化景观为世界文化遗产，核心湖区与沿湖开放空间面向公众开放。', '/images/seed/hangzhou.svg', 0.00, 0.00, 9999, 9999, '全天开放（部分收费景点另行开放）', 30.242703, 120.150269, 'https://westlake.hangzhou.gov.cn/', '杭州西湖风景名胜区管委会', '2026-05-19'),
(5, '张家界国家森林公园', '张家界', '武陵源区国家森林公园内', '中国第一个国家森林公园，武陵源世界自然遗产核心景区之一，以石英砂岩峰林地貌著称。', '/images/seed/zhangjiajie.svg', 227.00, 113.00, 500, 234, '07:30-18:00（以景区当日公告为准）', 29.327001, 110.475704, 'https://wly.hunan.gov.cn/', '湖南省文化和旅游厅/武陵源景区公开信息', '2026-05-19'),
(6, '秦始皇帝陵博物院', '西安', '临潼区秦陵北路', '以秦始皇兵马俑坑和秦始皇陵相关遗址为核心的遗址类博物馆。', '/images/seed/xian.svg', 120.00, 0.00, 800, 356, '08:30-18:30（旺季，停止检票17:00）', 34.384018, 109.278491, 'https://www.bmy.com.cn/', '秦始皇帝陵博物院官网', '2026-05-19'),
(7, '九寨沟风景名胜区', '阿坝', '阿坝藏族羌族自治州九寨沟县漳扎镇', '以高山湖泊、瀑布群、彩林和雪峰景观闻名的世界自然遗产。', '/images/seed/lake.svg', 190.00, 95.00, 1000, 445, '07:30-17:00（旺季，具体以景区公告为准）', 33.260772, 103.918599, 'https://www.jiuzhai.com/intelligent-service/tickets', '九寨沟风景名胜区官网', '2026-05-19'),
(8, '黄山风景区', '黄山', '黄山市黄山区汤口镇', '世界文化与自然双重遗产，以奇松、怪石、云海、温泉等景观著称。', '/images/seed/mountain.svg', 190.00, 95.00, 1500, 678, '06:00-17:30（旺季，具体以景区公告为准）', 30.130130, 118.168498, 'https://hsgwh.huangshan.gov.cn/', '黄山风景区管委会', '2026-05-19'),
(9, '漓江风景名胜区', '桂林', '桂林市灵川县至阳朔县漓江沿线', '桂林山水代表性景区，游船线路以漓江喀斯特峰林、江湾和田园景观为核心。', '/images/seed/guilin.svg', 210.00, 105.00, 600, 289, '08:00-12:00（游船班次以当日公告为准）', 25.166667, 110.416667, 'https://wglj.guilin.gov.cn/', '桂林市文化广电和旅游局', '2026-05-19'),
(10, '天坛公园', '北京', '东城区天坛东里甲1号', '明清两代皇帝祭天祈谷场所，是北京中轴线南段的重要世界文化遗产。', '/images/seed/beijing.svg', 34.00, 17.00, 2000, 1280, '06:00-22:00（景点院落开放时间另行公告）', 39.882200, 116.406600, 'http://www.tiantanpark.com/', '天坛公园官网', '2026-05-19'),
(11, '八达岭长城', '北京', '延庆区G6京藏高速58号出口', '明长城重要关隘，保存状况较好，是北京长城游览代表景区。', '/images/seed/beijing.svg', 40.00, 20.00, 2500, 1600, '06:30-16:30（以景区公告为准）', 40.358100, 116.020300, 'https://www.badaling.cn/', '八达岭长城景区官网', '2026-05-19'),
(12, '东方明珠广播电视塔', '上海', '浦东新区世纪大道1号', '上海陆家嘴地标建筑，集城市观光、展览和餐饮功能于一体。', '/images/seed/shanghai.svg', 199.00, 99.00, 1800, 920, '09:00-21:00（以景区公告为准）', 31.239700, 121.499800, 'https://www.orientalpearltower.com/', '东方明珠官网', '2026-05-19'),
(13, '豫园', '上海', '黄浦区福佑路168号', '江南古典园林代表之一，周边连接豫园商城和城隍庙历史文化街区。', '/images/seed/shanghai.svg', 40.00, 20.00, 1200, 760, '09:00-16:30（以景区公告为准）', 31.227200, 121.492100, 'https://www.yuyuantm.com.cn/', '豫园商城公开信息', '2026-05-19'),
(14, '上海迪士尼乐园', '上海', '浦东新区川沙新镇黄赵路310号', '大型主题乐园，包含主题园区、演艺、巡游和烟花等游乐体验。', '/images/seed/shanghai.svg', 475.00, 356.00, 3000, 1780, '08:30-21:30（以乐园日历为准）', 31.143400, 121.657900, 'https://www.shanghaidisneyresort.com/', '上海迪士尼度假区官网', '2026-05-19'),
(15, '成都大熊猫繁育研究基地', '成都', '成华区熊猫大道1375号', '以大熊猫保护、繁育、科研和公众教育为核心的生态型景区。', '/images/seed/chengdu.svg', 55.00, 27.00, 2000, 1188, '07:30-18:00（以景区公告为准）', 30.735500, 104.145600, 'https://www.panda.org.cn/', '成都大熊猫繁育研究基地官网', '2026-05-19'),
(16, '都江堰景区', '成都', '都江堰市公园路', '世界文化遗产，古代水利工程代表，与青城山共同构成重要旅游目的地。', '/images/seed/chengdu.svg', 80.00, 40.00, 1800, 960, '08:00-18:00（以景区公告为准）', 31.001700, 103.605500, 'https://www.djy517.com/', '都江堰青城山景区官网', '2026-05-19'),
(17, '蜈支洲岛', '三亚', '海棠区林旺镇后海村', '三亚近海海岛景区，以海水能见度、潜水和滨海休闲项目闻名。', '/images/seed/sanya.svg', 136.00, 68.00, 1500, 820, '08:00-18:30（航班以天气和公告为准）', 18.312600, 109.757400, 'https://www.wuzhizhou.com/', '蜈支洲岛旅游区官网', '2026-05-19'),
(18, '天涯海角游览区', '三亚', '天涯区天涯镇马岭山麓', '海南代表性滨海文化景区，以海滨礁石景观和“天涯”“海角”题刻闻名。', '/images/seed/sanya.svg', 68.00, 34.00, 1600, 900, '08:00-18:00（以景区公告为准）', 18.296000, 109.344000, 'https://www.aitianya.cn/', '天涯海角游览区官网', '2026-05-19'),
(19, '丽江古城', '丽江', '古城区大研古城', '世界文化遗产，以纳西族传统街巷、水系和民居建筑景观著称。', '/images/seed/attraction.svg', 0.00, 0.00, 9999, 9999, '全天开放（部分院落另行开放）', 26.872200, 100.233000, 'https://www.ljgucheng.com/', '丽江古城保护管理局公开信息', '2026-05-19'),
(20, '亚龙湾国家旅游度假区', '三亚', '吉阳区亚龙湾国家旅游度假区', '三亚重要滨海度假区，以海湾、沙滩和度假酒店群为主要吸引物。', '/images/seed/sanya.svg', 0.00, 0.00, 9999, 9999, '全天开放（海滩及项目以现场公告为准）', 18.229500, 109.637000, 'https://lwj.sanya.gov.cn/', '三亚市旅游和文化广电体育局', '2026-05-19'),
(21, '鼓浪屿', '厦门', '思明区鼓浪屿', '世界文化遗产，融合海岛街巷、近代建筑和音乐文化景观。', '/images/seed/coast.svg', 0.00, 0.00, 9999, 9999, '全天开放（上岛船班及收费景点另行公告）', 24.447700, 118.061900, 'https://gly.xm.gov.cn/', '鼓浪屿管委会公开信息', '2026-05-19'),
(22, '中山陵', '南京', '玄武区石象路7号', '孙中山先生陵寝所在地，位于钟山风景名胜区内，是南京重要历史文化地标。', '/images/seed/nanjing.svg', 0.00, 0.00, 3000, 2100, '08:30-17:00（周一部分区域闭馆，节假日除外）', 32.064300, 118.848600, 'https://zschina.nanjing.gov.cn/', '南京钟山风景区官网', '2026-05-19'),
(23, '拙政园', '苏州', '姑苏区东北街178号', '苏州古典园林代表，世界文化遗产，以水景、亭台和园林空间布局著称。', '/images/seed/garden.svg', 80.00, 40.00, 1400, 780, '07:30-17:30（以景区公告为准）', 31.324300, 120.629700, 'https://www.szzzy.cn/', '拙政园官网', '2026-05-19'),
(24, '洪崖洞民俗风貌区', '重庆', '渝中区嘉陵江滨江路88号', '重庆山城夜景代表地，以吊脚楼风貌、滨江夜景和城市商业空间闻名。', '/images/seed/chongqing.svg', 0.00, 0.00, 9999, 9999, '全天开放（商户营业时间不一）', 29.562800, 106.584000, 'https://whlyw.cq.gov.cn/', '重庆市文化和旅游发展委员会', '2026-05-19'),
(25, '龙脊梯田', '桂林', '龙胜各族自治县和平乡', '桂林北部山地梯田景观，包含平安寨、金坑大寨等观景区域。', '/images/seed/guilin.svg', 80.00, 40.00, 1000, 520, '08:00-17:30（以景区公告为准）', 25.764000, 110.123000, 'https://wglj.guilin.gov.cn/', '桂林市文化广电和旅游局', '2026-05-19'),
(26, '象鼻山景区', '桂林', '象山区滨江路1号', '桂林城市山水代表景观，山体形似象鼻伸入漓江，是桂林标志性景点。', '/images/seed/guilin.svg', 0.00, 0.00, 1800, 1260, '07:00-18:00（以景区公告为准）', 25.273900, 110.296700, 'https://wglj.guilin.gov.cn/', '桂林市文化广电和旅游局', '2026-05-19'),
(27, '天门山国家森林公园', '张家界', '永定区大庸路', '张家界城市近郊山岳型景区，以天门洞、玻璃栈道和索道体验闻名。', '/images/seed/zhangjiajie.svg', 278.00, 139.00, 1200, 660, '08:00-18:00（索道及线路以景区公告为准）', 29.044600, 110.482400, 'https://wly.hunan.gov.cn/', '湖南省文化和旅游厅/天门山公开信息', '2026-05-19'),
(28, '宏村', '黄山', '黟县宏村镇', '皖南古村落代表，世界文化遗产，以月沼、水圳和徽派民居格局著称。', '/images/seed/lake.svg', 104.00, 52.00, 1200, 720, '07:30-17:30（以景区公告为准）', 30.004900, 117.987500, 'https://ct.ah.gov.cn/', '安徽省文化和旅游厅公开信息', '2026-05-19')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `city` = VALUES(`city`),
  `address` = VALUES(`address`),
  `description` = VALUES(`description`),
  `cover_img` = VALUES(`cover_img`),
  `adult_price` = VALUES(`adult_price`),
  `child_price` = VALUES(`child_price`),
  `open_time` = VALUES(`open_time`),
  `lat` = VALUES(`lat`),
  `lng` = VALUES(`lng`),
  `official_url` = VALUES(`official_url`),
  `source_name` = VALUES(`source_name`),
  `data_checked_date` = VALUES(`data_checked_date`);

-- 开放授权图片素材来源。前端展示图片时应同步展示作者和协议，避免只存裸 URL。
INSERT INTO `tm_media_asset` (`id`, `target_type`, `target_id`, `media_type`, `url`, `caption`, `author`, `license_name`, `license_url`, `source_url`, `source_name`, `data_checked_date`) VALUES
(1, 'attraction', 1, 'image', '/images/seed/beijing.svg', '故宫俯瞰图', 'Pixelflake', 'CC BY-SA 3.0', 'https://creativecommons.org/licenses/by-sa/3.0/', 'https://commons.wikimedia.org/wiki/File:The_Forbidden_City_-_View_from_Coal_Hill.jpg', 'Wikimedia Commons', '2026-05-19'),
(2, 'attraction', 2, 'image', '/images/seed/beijing.svg', '颐和园景观', 'Jakub Halun', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:20090530_Beijing_Summer_Palace_8467.jpg', 'Wikimedia Commons', '2026-05-19'),
(3, 'attraction', 3, 'image', '/images/seed/shanghai.svg', '上海外滩', 'Ermell', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:Shanghai_Bund-20150516-RM-173803.jpg', 'Wikimedia Commons', '2026-05-19'),
(4, 'attraction', 4, 'image', '/images/seed/hangzhou.svg', '西湖九曲桥', 'Sekino Tadashi', 'Public domain', 'https://creativecommons.org/publicdomain/mark/1.0/', 'https://commons.wikimedia.org/wiki/File:West_Lake,_Hangzhou_(Nine-turn_bridge).jpg', 'Wikimedia Commons', '2026-05-19'),
(5, 'attraction', 5, 'image', '/images/seed/zhangjiajie.svg', '张家界国家森林公园', 'Kuruman', 'CC BY 2.0', 'https://creativecommons.org/licenses/by/2.0/', 'https://commons.wikimedia.org/wiki/File:Zhangjiajie_National_Forest_Park.jpg', 'Wikimedia Commons', '2026-05-19'),
(6, 'attraction', 6, 'image', '/images/seed/xian.svg', '秦兵马俑', 'xiquinhosilva', 'CC BY 2.0', 'https://creativecommons.org/licenses/by/2.0/', 'https://commons.wikimedia.org/wiki/File:Terracotta_Army_(54082561381).jpg', 'Wikimedia Commons', '2026-05-19'),
(7, 'attraction', 7, 'image', '/images/seed/lake.svg', '九寨沟五花海', 'Chensiyuan', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:1_jiuzhaigou_valley_wu_hua_hai_2011b.jpg', 'Wikimedia Commons', '2026-05-19'),
(8, 'attraction', 8, 'image', '/images/seed/mountain.svg', '黄山景观', 'Miaulian', 'CC BY-SA 3.0', 'https://creativecommons.org/licenses/by-sa/3.0/', 'https://commons.wikimedia.org/wiki/File:Anhui_Huangshan.jpg', 'Wikimedia Commons', '2026-05-19'),
(9, 'attraction', 9, 'image', '/images/seed/guilin.svg', '漓江山水', 'Chensiyuan', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:1_li_jiang_guilin_yangshuo_2011.jpg', 'Wikimedia Commons', '2026-05-19'),
(10, 'post', 3, 'image', '/images/seed/sanya.svg', '三亚亚龙湾', 'Anna Frodesiak', 'Public domain', 'https://creativecommons.org/publicdomain/mark/1.0/', 'https://commons.wikimedia.org/wiki/File:Yalong_Bay_01.jpg', 'Wikimedia Commons', '2026-05-19'),
(11, 'post', 5, 'image', '/images/seed/attraction.svg', '丽江古城夜景', 'Chensiyuan', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:1_lijiang_old_town_night.jpg', 'Wikimedia Commons', '2026-05-19'),
(12, 'hotel', 3, 'image', 'https://ak-d.tripcdn.com/images/1mc6f12000hrejeww92B0.jpg', '广州白天鹅宾馆外观', 'Wikimedia Commons contributors', 'Wikimedia Commons 图片授权以文件页为准', NULL, 'https://commons.wikimedia.org/wiki/File:WHITE_SWAN_HOTEL_(2017-10-14).jpg', 'Wikimedia Commons', '2026-05-23'),
(13, 'hotel', 4, 'image', 'https://dimg04.c-ctrip.com/images/220i1b000001aohx93B7F_R_960_660_R5_D.jpg', '成都锦江宾馆外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://ph.trip.com/hotels/chengdu-hotel-detail-392701/jinjiang-hotel/photo.html', 'Trip.com', '2026-05-23'),
(14, 'hotel', 9, 'image', 'https://ak-d.tripcdn.com/images/0220t12000plokdzi4C79_R_960_660_R5_D.jpg', '重庆解放碑威斯汀酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/chongqing-hotel-detail-1200477/the-westin-chongqing-liberation-square/', 'Trip.com', '2026-05-23'),
(15, 'hotel', 14, 'image', 'https://ak-d.tripcdn.com/images/200m0800000034723792B_R_960_660_R5_D.jpg', '青岛海景花园大酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/qingdao-hotel-detail-437516/seaview-garden-hotel/', 'Trip.com', '2026-05-23')
ON DUPLICATE KEY UPDATE
  `url` = VALUES(`url`),
  `caption` = VALUES(`caption`),
  `author` = VALUES(`author`),
  `license_name` = VALUES(`license_name`),
  `license_url` = VALUES(`license_url`),
  `source_url` = VALUES(`source_url`),
  `source_name` = VALUES(`source_name`),
  `data_checked_date` = VALUES(`data_checked_date`);

INSERT IGNORE INTO `tm_media_asset` (`id`, `target_type`, `target_id`, `media_type`, `url`, `caption`, `author`, `license_name`, `license_url`, `source_url`, `source_name`, `data_checked_date`) VALUES
(16, 'post', 1, 'image', '/images/seed/beijing.svg', '八达岭长城', 'CEphoto, Uwe Aranas', 'CC BY-SA 3.0', 'https://creativecommons.org/licenses/by-sa/3.0/', 'https://commons.wikimedia.org/wiki/File:Badaling_China_Great-Wall-of-China-01.jpg', 'Wikimedia Commons', '2026-05-19'),
(17, 'post', 2, 'image', '/images/seed/shanghai.svg', '上海外滩蓝调时刻', 'Dllu', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:Blue_hour_view_of_the_Bund_from_the_Shanghai_World_Financial_Center_dllu.jpg', 'Wikimedia Commons', '2026-05-19'),
(18, 'post', 3, 'image', '/images/seed/sanya.svg', '亚龙湾海岸', 'Phillip Hong', 'CC BY-SA 1.0', 'https://creativecommons.org/licenses/by-sa/1.0/', 'https://commons.wikimedia.org/wiki/File:Yalong_Bay_from_hotel.JPG', 'Wikimedia Commons', '2026-05-19'),
(19, 'post', 4, 'image', '/images/seed/chengdu.svg', '成都火锅', 'Prince Roy', 'CC BY 2.0', 'https://creativecommons.org/licenses/by/2.0/', 'https://commons.wikimedia.org/wiki/File:Chengdu_Hotpot.jpg', 'Wikimedia Commons', '2026-05-19'),
(20, 'post', 4, 'image', '/images/seed/chengdu.svg', '成都宽窄巷子', 'Breaknet2025', 'CC BY 4.0', 'https://creativecommons.org/licenses/by/4.0/', 'https://commons.wikimedia.org/wiki/File:Chengdu_Kuanzhai_Alley_Touristic_Spot_Relics_%E6%88%90%E9%83%BD%E5%AE%BD%E7%AA%84%E5%B7%B7%E5%AD%90%E6%96%87%E7%89%A9%E5%8F%91%E6%8E%98%E9%81%97%E4%BA%A7.jpg', 'Wikimedia Commons', '2026-05-19'),
(21, 'post', 5, 'image', '/images/seed/attraction.svg', '丽江古城街巷', 'CEphoto, Uwe Aranas', 'CC BY-SA 3.0', 'https://creativecommons.org/licenses/by-sa/3.0/', 'https://commons.wikimedia.org/wiki/File:Lijiang_Yunnan_Doors-_in-old-town-01.jpg', 'Wikimedia Commons', '2026-05-19');

-- 修正酒店封面：旧数据中部分酒店使用城市风景图，这里统一覆盖为与酒店本体匹配的真实照片。
-- 部分 OTA/CDN 图片 URL 会随平台策略调整，source_url 保留酒店图库页便于后续核验替换。
UPDATE `tm_hotel`
SET `cover_img` = CASE `id`
  WHEN 1 THEN 'https://dimg04.c-ctrip.com/images//200l1g000001hgwwk8DB1_R_550_412.jpg'
  WHEN 2 THEN 'https://ak-d.tripcdn.com/images/1mc3d12000rs1ln328F37.jpg'
  WHEN 3 THEN 'https://ak-d.tripcdn.com/images/1mc6f12000hrejeww92B0.jpg'
  WHEN 4 THEN 'https://dimg04.c-ctrip.com/images/220i1b000001aohx93B7F_R_960_660_R5_D.jpg'
  WHEN 5 THEN 'https://ak-d.tripcdn.com/images/200w13000000vir8yDBA5_R_960_660_R5_D.jpg'
  WHEN 6 THEN 'https://ak-d.tripcdn.com/images/200q050000000ghm177AC.jpg'
  WHEN 7 THEN 'https://ak-d.tripcdn.com/images/1mc0f12000iw62mfx8BDD.jpg'
  WHEN 8 THEN 'https://ak-d.tripcdn.com/images/fd/hotel/g4/M08/FC/23/CggYHlX__YOAVc9JAAP-iECj334734_R_960_660_R5_D.jpg'
  WHEN 9 THEN 'https://ak-d.tripcdn.com/images/0220t12000plokdzi4C79_R_960_660_R5_D.jpg'
  WHEN 10 THEN 'https://ak-d.tripcdn.com/images/200v14000000w7mnt5A2C.jpg'
  WHEN 11 THEN 'https://ak-d.tripcdn.com/images/hotel/452000/451368/00f6bba719044a4394311d9aaf47eeb7.jpg'
  WHEN 12 THEN 'https://ak-d.tripcdn.com/images/1mc1f12000b9nz5c03B02_R_960_660_R5_D.jpg'
  WHEN 13 THEN 'https://ak-d.tripcdn.com/images/1mc0m12000aq6mt0h4457.jpg'
  WHEN 14 THEN 'https://ak-d.tripcdn.com/images/200m0800000034723792B_R_960_660_R5_D.jpg'
  WHEN 15 THEN 'https://ak-d.tripcdn.com/images/1mc4r12000repen2v8026.jpg'
  WHEN 16 THEN 'https://ak-d.tripcdn.com/images/20060v000000jo62xE3EB.jpg'
  WHEN 17 THEN 'https://ak-d.tripcdn.com/images/200m1e000001fvyd2AB3D_R_960_660_R5_D.jpg'
  WHEN 18 THEN 'https://ak-d.tripcdn.com/images/1mc1712000epu78z56B04_R_960_660_R5_D.jpg'
  WHEN 19 THEN 'https://ak-d.tripcdn.com/images/02064120008bk81fz92B1_R_960_660_R5_D.jpg'
  WHEN 20 THEN 'https://ak-d.tripcdn.com/images/1mc6o12000br4n333F7C1_R_960_660_R5_D.jpg'
  ELSE `cover_img`
END
WHERE `id` BETWEEN 1 AND 20;

INSERT INTO `tm_media_asset` (`id`, `target_type`, `target_id`, `media_type`, `url`, `caption`, `author`, `license_name`, `license_url`, `source_url`, `source_name`, `data_checked_date`) VALUES
(25, 'hotel', 1, 'image', 'https://dimg04.c-ctrip.com/images//200l1g000001hgwwk8DB1_R_550_412.jpg', '北京国贸大酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/beijing-hotel-detail-369675/china-world-summit-wing-beijing', 'Trip.com', '2026-05-23'),
(26, 'hotel', 2, 'image', 'https://ak-d.tripcdn.com/images/1mc3d12000rs1ln328F37.jpg', '上海外滩华尔道夫酒店外观', 'Hilton/Waldorf Astoria 官方图库', '酒店官方图片，仅用于课程演示引用', NULL, 'https://www.hilton.com/zh-hans/hotels/shawawa-waldorf-astoria-shanghai-on-the-bund/gallery/', 'Hilton', '2026-05-23'),
(27, 'hotel', 3, 'image', 'https://ak-d.tripcdn.com/images/1mc6f12000hrejeww92B0.jpg', '广州白天鹅宾馆外观', 'Wikimedia Commons contributors', 'Wikimedia Commons 图片授权以文件页为准', NULL, 'https://commons.wikimedia.org/wiki/File:WHITE_SWAN_HOTEL_(2017-10-14).jpg', 'Wikimedia Commons', '2026-05-23'),
(28, 'hotel', 4, 'image', 'https://dimg04.c-ctrip.com/images/220i1b000001aohx93B7F_R_960_660_R5_D.jpg', '成都锦江宾馆外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://ph.trip.com/hotels/chengdu-hotel-detail-392701/jinjiang-hotel/photo.html', 'Trip.com', '2026-05-23'),
(29, 'hotel', 5, 'image', 'https://ak-d.tripcdn.com/images/200w13000000vir8yDBA5_R_960_660_R5_D.jpg', '西安大唐芙蓉园精品酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/xi-an-hotel-detail-447184/tang-paradise-hotel/', 'Trip.com', '2026-05-23'),
(30, 'hotel', 6, 'image', 'https://ak-d.tripcdn.com/images/200q050000000ghm177AC.jpg', '三亚亚龙湾万豪度假酒店外观', 'Marriott 官方图库', '酒店官方图片，仅用于课程演示引用', NULL, 'https://www.marriott.com.cn/hotels/syxmc-sanya-marriott-yalong-bay-resort-and-spa/photos/', 'Marriott', '2026-05-23'),
(31, 'hotel', 7, 'image', 'https://ak-d.tripcdn.com/images/1mc0f12000iw62mfx8BDD.jpg', '丽江古城铂尔曼大酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/lijiang-hotel-detail-435014/pullman-lijiang-resort-and-spa/', 'Trip.com', '2026-05-23'),
(32, 'hotel', 8, 'image', 'https://ak-d.tripcdn.com/images/fd/hotel/g4/M08/FC/23/CggYHlX__YOAVc9JAAP-iECj334734_R_960_660_R5_D.jpg', '杭州西湖喜来登大酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/hangzhou-hotel-detail-397388/hangzhou-west-lake-sheraton-hotel/', 'Trip.com', '2026-05-23'),
(33, 'hotel', 9, 'image', 'https://ak-d.tripcdn.com/images/0220t12000plokdzi4C79_R_960_660_R5_D.jpg', '重庆解放碑威斯汀酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/chongqing-hotel-detail-1200477/the-westin-chongqing-liberation-square/', 'Trip.com', '2026-05-23'),
(34, 'hotel', 10, 'image', 'https://ak-d.tripcdn.com/images/200v14000000w7mnt5A2C.jpg', '北京王府井万豪酒店外观', 'Marriott 官方图库', '酒店官方图片，仅用于课程演示引用', NULL, 'https://www.marriott.com.cn/hotels/bjswf-beijing-marriott-hotel-northeast/photos/', 'Marriott', '2026-05-23'),
(35, 'hotel', 11, 'image', 'https://ak-d.tripcdn.com/images/hotel/452000/451368/00f6bba719044a4394311d9aaf47eeb7.jpg', '上海静安香格里拉大酒店外观', 'Shangri-La 官方图库', '酒店官方图片，仅用于课程演示引用', NULL, 'https://www.shangri-la.com/cn/shanghai/jinganshangrila/about/', 'Shangri-La', '2026-05-23'),
(36, 'hotel', 12, 'image', 'https://ak-d.tripcdn.com/images/1mc1f12000b9nz5c03B02_R_960_660_R5_D.jpg', '厦门悦华酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/xiamen-hotel-detail-419070/candd-hotel-xiamen/', 'Trip.com', '2026-05-23'),
(37, 'hotel', 13, 'image', 'https://ak-d.tripcdn.com/images/1mc0m12000aq6mt0h4457.jpg', '桂林香格里拉大酒店外观', 'Shangri-La 官方图库', '酒店官方图片，仅用于课程演示引用', NULL, 'https://www.shangri-la.com/cn/guilin/shangrila/about/', 'Shangri-La', '2026-05-23'),
(38, 'hotel', 14, 'image', 'https://ak-d.tripcdn.com/images/200m0800000034723792B_R_960_660_R5_D.jpg', '青岛海景花园大酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/qingdao-hotel-detail-437516/seaview-garden-hotel/', 'Trip.com', '2026-05-23'),
(39, 'hotel', 15, 'image', 'https://ak-d.tripcdn.com/images/1mc4r12000repen2v8026.jpg', '全季酒店成都太古里春熙路店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/chengdu-hotel-detail-133457358/ji-hotel/photo.html', 'Trip.com', '2026-05-23'),
(40, 'hotel', 16, 'image', 'https://ak-d.tripcdn.com/images/20060v000000jo62xE3EB.jpg', '成都太古里春熙美居酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/chengdu-hotel-detail-21132716/mercure-chengdu-chunxi/photo.html', 'Trip.com', '2026-05-23'),
(41, 'hotel', 17, 'image', 'https://ak-d.tripcdn.com/images/200m1e000001fvyd2AB3D_R_960_660_R5_D.jpg', '成都瑞城名人酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/chengdu-hotel-detail-470094/chengdu-richmond-hotel/', 'Trip.com', '2026-05-23'),
(42, 'hotel', 18, 'image', 'https://ak-d.tripcdn.com/images/1mc1712000epu78z56B04_R_960_660_R5_D.jpg', '成都东大明宇豪雅饭店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/chengdu-hotel-detail-435688/minyoun-chengdu-dongda-hotel/', 'Trip.com', '2026-05-23'),
(43, 'hotel', 19, 'image', 'https://ak-d.tripcdn.com/images/02064120008bk81fz92B1_R_960_660_R5_D.jpg', '南京金陵饭店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/nanjing-hotel-detail-346283/jinling-hotel/', 'Trip.com', '2026-05-23'),
(44, 'hotel', 20, 'image', 'https://ak-d.tripcdn.com/images/1mc6o12000br4n333F7C1_R_960_660_R5_D.jpg', '苏州吴宫泛太平洋酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/suzhou-hotel-detail-346290/pan-pacific-suzhou/', 'Trip.com', '2026-05-23')
ON DUPLICATE KEY UPDATE
  `url` = VALUES(`url`),
  `caption` = VALUES(`caption`),
  `author` = VALUES(`author`),
  `license_name` = VALUES(`license_name`),
  `license_url` = VALUES(`license_url`),
  `source_url` = VALUES(`source_url`),
  `source_name` = VALUES(`source_name`),
  `data_checked_date` = VALUES(`data_checked_date`);

-- 社区游记数据
-- 文案按真实旅行场景重新整理；互动数为演示热度，图片使用本地种子图。
INSERT INTO `tm_post` (`id`, `user_id`, `title`, `content`, `images`, `destination`, `tags`, `like_count`, `comment_count`, `collect_count`, `view_count`, `status`, `source_name`, `source_url`, `data_checked_date`) VALUES
(1, 3, '北京5天｜中轴线故宫长城不绕路路线', '这次北京行程按“核心景点分散预约”的思路安排，前两天留给升旗、天安门、故宫和国家博物馆，故宫建议午门进、神武门出，下午接景山看中轴线视角会很顺。第三天去八达岭长城，早班高铁到八达岭长城站最省心，返城后只安排鸟巢、水立方夜景，不再塞博物馆。第四天放慢节奏走颐和园、圆明园和高校周边，第五天用恭王府、什刹海、鼓楼、雍和宫、南锣鼓巷收尾。北京很多场馆都需要提前预约，住宿建议放在二环或地铁 2/4/8 号线附近，每天少换一次线会轻松很多。', '/images/seed/beijing.svg,/images/seed/beijing.svg,/images/seed/beijing.svg', '北京', '北京,故宫,八达岭长城,颐和园,中轴线', 9224, 108, 9174, 27600, 1, '公开路线资料整理', NULL, '2026-05-21'),
(2, 4, '上海1天｜武康路静安寺外滩陆家嘴', '上海一日游不要贪多，最好把路线压成一条从西到东的 citywalk。上午从武康大楼开始，沿武康路慢慢走到安福路一带，咖啡店和老洋房都适合边走边拍；中午转到静安寺或南京西路吃饭，下午去豫园和城隍庙，留 1.5 小时足够。傍晚 18:30 后到外滩看亮灯，天气好可以坐轮渡到陆家嘴，十几分钟就能换到浦东视角。虹桥进出城尽量避开晚高峰，外滩人多时不要只挤观景平台，往十六铺方向走会舒服一些。', '/images/seed/shanghai.svg,/images/seed/shanghai.svg', '上海', '上海,外滩,陆家嘴,武康路,citywalk', 5725, 16, 5034, 17200, 1, '公开路线资料整理', NULL, '2026-05-21'),
(3, 3, '三亚蜈支洲岛｜玻璃海环岛路线', '蜈支洲岛适合放在天气最好的那一天，海水颜色、能见度和拍照效果都很看阳光。建议早上第一批上岛，先坐观光车走海洋之星、观日岩、情人谷、情人岛几个远点，中午回到码头附近吃饭休息，下午再沿灯塔、情人桥和沙滩慢慢拍照。岛上水上项目排队时间不短，潜水、摩托艇和拖伞最好只选一两个，不然会把整天都耗在等候区。防晒衣、拖鞋、防水袋和少量现金很实用，岛上餐饮价格偏高，可以提前带水和简单零食。', '/images/seed/sanya.svg,/images/seed/sanya.svg', '三亚', '三亚,蜈支洲岛,亚龙湾,海岛,玻璃海', 5283, 149, 4513, 15800, 1, '公开路线资料整理', NULL, '2026-05-21'),
(4, 4, '成都周边1天｜熊猫谷都江堰轻量路线', '成都周边如果只留一天，熊猫谷加都江堰比市区熊猫基地更适合错峰。早上从春熙路附近出发，地铁到犀浦后换高铁到离堆公园，先去仰天窝广场拍照，再打车到熊猫谷，上午看熊猫活动概率更高。午餐可以放在灌县古城，下午从秦堰楼方向进入都江堰，一路往下走二王庙、安澜索桥、鱼嘴、飞沙堰、宝瓶口和离堆公园，基本不走回头路。傍晚南桥亮灯很有氛围，但返程高铁要提前看班次，周末不要卡最后一班。', '/images/seed/chengdu.svg,/images/seed/chengdu.svg', '成都', '成都,熊猫谷,都江堰,灌县古城,周边游', 2242, 94, 3413, 9800, 1, '公开路线资料整理', NULL, '2026-05-21'),
(5, 2, '丽江2天｜玉龙雪山蓝月谷白沙古镇', '丽江两天更适合把雪山和古城分开走。第一天早起去玉龙雪山，索道票一定要提前抢，体力一般可以选云杉坪或牦牛坪，想冲海拔再考虑冰川公园；蓝月谷放在下山后，晴天水色会非常漂亮。下午回束河古镇吃饭休息，比大研古城安静一些。第二天看天气决定要不要去东巴谷看日照金山，再去白沙古镇喝咖啡、看雪山和纳西院落，下午回丽江古城逛现文巷、大研花巷和狮子山。高海拔行程不要硬撑，氧气、保暖外套和甜食都要提前备好。', '/images/seed/attraction.svg,/images/seed/attraction.svg', '丽江', '丽江,玉龙雪山,蓝月谷,白沙古镇,高原旅行', 2518, 10, 2160, 7600, 1, '公开路线资料整理', NULL, '2026-05-21')
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `content` = VALUES(`content`),
  `images` = VALUES(`images`),
  `destination` = VALUES(`destination`),
  `tags` = VALUES(`tags`),
  `like_count` = VALUES(`like_count`),
  `comment_count` = VALUES(`comment_count`),
  `collect_count` = VALUES(`collect_count`),
  `view_count` = VALUES(`view_count`),
  `status` = VALUES(`status`),
  `source_name` = VALUES(`source_name`),
  `source_url` = VALUES(`source_url`),
  `data_checked_date` = VALUES(`data_checked_date`);

-- 评论数据
INSERT IGNORE INTO `tm_comment` (`post_id`, `user_id`, `content`, `like_count`) VALUES
(1, 4, '攻略写得太详细了！正好下个月要去北京，收藏了！', 12),
(1, 2, '故宫确实要早去，我上次10点去人山人海，寸步难行', 8),
(1, 3, '景山公园真的推荐！而且是免费的，可以俯瞰整个故宫', 15),
(2, 3, '外滩夜景已经是我去上海必去的地方了，每次都看不够', 23),
(2, 2, '和平饭店的爵士乐也推荐，氛围感十足', 9),
(3, 4, '亚龙湾的海水真的是梦幻蓝！比较推荐4月5月去，人少价格也便宜', 18),
(4, 3, '成都火锅真的不一样！外地吃的根本比不了', 21),
(5, 4, '丽江古城很赞但人比较多，建议避开节假日', 11);

-- 点赞数据
INSERT IGNORE INTO `tm_like` (`user_id`, `target_id`, `target_type`) VALUES
(2, 1, 0), (3, 1, 0), (4, 1, 0),
(2, 2, 0), (3, 2, 0),
(3, 3, 0), (4, 3, 0),
(2, 4, 0), (4, 4, 0),
(2, 5, 0), (3, 5, 0);

-- 关注数据
INSERT IGNORE INTO `tm_follow` (`follower_id`, `followee_id`) VALUES
(2, 3), (2, 4),
(3, 4), (3, 2),
(4, 3);

-- 价格历史数据（最近7天）
INSERT IGNORE INTO `tm_price_history` (`ticket_id`, `ticket_type`, `record_date`, `lowest_price`) VALUES
(1, 0, '2026-05-04', 720.00),
(1, 0, '2026-05-05', 695.00),
(1, 0, '2026-05-06', 660.00),
(1, 0, '2026-05-07', 680.00),
(1, 0, '2026-05-08', 650.00),
(1, 0, '2026-05-09', 670.00),
(1, 0, '2026-05-10', 680.00),
(1, 1, '2026-05-04', 590.00),
(1, 1, '2026-05-05', 553.00),
(1, 1, '2026-05-06', 510.00),
(1, 1, '2026-05-07', 553.00),
(1, 1, '2026-05-08', 530.00),
(1, 1, '2026-05-09', 553.00),
(1, 1, '2026-05-10', 553.00);

-- 敏感词初始数据
INSERT IGNORE INTO `sys_sensitive_word` (`word`, `level`) VALUES
('赌博', 3), ('诈骗', 3), ('色情', 3), ('暴力', 2),
('广告', 1), ('代购', 1), ('刷单', 2);

-- 一日游/周边游产品表
CREATE TABLE IF NOT EXISTS `tm_tour_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '产品名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `tour_type` TINYINT(1) DEFAULT '0' COMMENT '0=一日游, 1=周边游',
  `departure_city` VARCHAR(50) DEFAULT NULL COMMENT '出发城市',
  `destination` VARCHAR(100) DEFAULT NULL COMMENT '目的地',
  `duration` VARCHAR(20) DEFAULT NULL COMMENT '行程时长',
  `price` DECIMAL(10,2) DEFAULT '0.00' COMMENT '价格',
  `cover_img` VARCHAR(500) DEFAULT NULL COMMENT '封面图',
  `status` TINYINT(1) DEFAULT '1' COMMENT '0=下线, 1=上线',
  `source_name` VARCHAR(100) DEFAULT NULL COMMENT '数据来源名称',
  `source_url` VARCHAR(500) DEFAULT NULL COMMENT '数据来源URL',
  `data_checked_date` DATE DEFAULT NULL COMMENT '数据核验日期',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一日游/周边游产品表';

-- 兼容已初始化过的旧库：补充行程产品来源字段
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_tour_product` ADD COLUMN `source_name` VARCHAR(100) DEFAULT NULL COMMENT ''数据来源名称'' AFTER `status`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_tour_product' AND COLUMN_NAME = 'source_name');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_tour_product` ADD COLUMN `source_url` VARCHAR(500) DEFAULT NULL COMMENT ''数据来源URL'' AFTER `source_name`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_tour_product' AND COLUMN_NAME = 'source_url');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_tour_product` ADD COLUMN `data_checked_date` DATE DEFAULT NULL COMMENT ''数据核验日期'' AFTER `source_url`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_tour_product' AND COLUMN_NAME = 'data_checked_date');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE `tm_tour_product` MODIFY COLUMN `cover_img` VARCHAR(500) DEFAULT NULL COMMENT '封面图';

INSERT IGNORE INTO `tm_tour_product` (`id`, `name`, `description`, `tour_type`, `departure_city`, `destination`, `duration`, `price`, `cover_img`, `source_name`, `source_url`, `data_checked_date`) VALUES
(1, '故宫博物院官方导览一日', '基于故宫博物院开放时间与官方导览信息整理，适合首次到访的中轴线游览。价格为演示服务费，不代表官方票价。', 0, '北京', '故宫博物院', '1天', 128.00, '/images/seed/beijing.svg', '故宫博物院官网', 'https://www.dpm.org.cn/Visit.html', '2026-05-19'),
(2, '颐和园昆明湖半日游', '基于颐和园开放时间与园区导览整理，覆盖东宫门、仁寿殿、长廊、昆明湖等节点。价格为演示服务费。', 0, '北京', '颐和园', '0.5天', 98.00, '/images/seed/beijing.svg', '颐和园官网', 'https://www.summerpalace.net.cn/visit.html', '2026-05-19'),
(3, '杭州西湖环湖一日游', '基于西湖景区公开信息整理，串联断桥、白堤、苏堤、花港观鱼和湖滨区域。价格为演示服务费。', 0, '杭州', '西湖', '1天', 118.00, '/images/seed/hangzhou.svg', '杭州西湖风景名胜区管委会', 'https://westlake.hangzhou.gov.cn/', '2026-05-19'),
(4, '黄山风景区经典两日游', '基于黄山风景区公开信息整理，覆盖云谷寺、始信峰、北海、光明顶、玉屏楼等常规节点。价格为演示服务费。', 1, '黄山', '黄山风景区', '2天1晚', 398.00, '/images/seed/mountain.svg', '黄山风景区管委会', 'https://hsgwh.huangshan.gov.cn/', '2026-05-19'),
(5, '九寨沟核心景点一日游', '基于九寨沟景区票务与服务信息整理，覆盖树正沟、日则沟、则查洼沟核心观景点。价格为演示服务费。', 0, '阿坝', '九寨沟风景名胜区', '1天', 268.00, '/images/seed/lake.svg', '九寨沟风景名胜区官网', 'https://www.jiuzhai.com/intelligent-service/tickets', '2026-05-19'),
(6, '桂林漓江游船一日游', '基于桂林文旅公开信息整理，围绕桂林至阳朔漓江游船线路设计。价格为演示服务费。', 0, '桂林', '漓江风景名胜区', '1天', 288.00, '/images/seed/guilin.svg', '桂林市文化广电和旅游局', 'https://wglj.guilin.gov.cn/', '2026-05-19');

CREATE TABLE IF NOT EXISTS `tm_tour_product_step` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL COMMENT '所属行程产品ID',
  `day_no` INT NOT NULL DEFAULT 1 COMMENT '第几天',
  `sequence_no` INT NOT NULL COMMENT '当天顺序',
  `place_name` VARCHAR(100) NOT NULL COMMENT '游览节点',
  `attraction_id` BIGINT DEFAULT NULL COMMENT '关联景点ID',
  `stay_minutes` INT DEFAULT NULL COMMENT '建议停留分钟数',
  `transport_note` VARCHAR(200) DEFAULT NULL COMMENT '交通/衔接说明',
  `source_url` VARCHAR(500) DEFAULT NULL COMMENT '节点来源URL',
  PRIMARY KEY (`id`),
  KEY `idx_tour_step_product` (`product_id`, `day_no`, `sequence_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一日游/周边游行程节点表';

INSERT IGNORE INTO `tm_tour_product_step` (`id`, `product_id`, `day_no`, `sequence_no`, `place_name`, `attraction_id`, `stay_minutes`, `transport_note`, `source_url`) VALUES
(1, 1, 1, 1, '午门入院', 1, 30, '建议按预约时段入院', 'https://www.dpm.org.cn/Visit.html'),
(2, 1, 1, 2, '太和殿-中和殿-保和殿', 1, 90, '沿中轴线步行游览', 'https://www.dpm.org.cn/Visit.html'),
(3, 1, 1, 3, '乾清宫-交泰殿-坤宁宫', 1, 60, '继续沿中轴线步行', 'https://www.dpm.org.cn/Visit.html'),
(4, 1, 1, 4, '御花园-神武门出院', 1, 45, '从神武门离院后可衔接景山公园', 'https://www.dpm.org.cn/Visit.html'),
(5, 2, 1, 1, '东宫门', 2, 20, '入口与集合点', 'https://www.summerpalace.net.cn/visit.html'),
(6, 2, 1, 2, '仁寿殿-长廊', 2, 70, '园内步行', 'https://www.summerpalace.net.cn/visit.html'),
(7, 2, 1, 3, '昆明湖-十七孔桥', 2, 80, '湖区步行或游船以现场开放为准', 'https://www.summerpalace.net.cn/visit.html'),
(8, 3, 1, 1, '断桥残雪', 4, 30, '湖滨步行起点', 'https://westlake.hangzhou.gov.cn/'),
(9, 3, 1, 2, '白堤-孤山', 4, 80, '沿湖步行', 'https://westlake.hangzhou.gov.cn/'),
(10, 3, 1, 3, '苏堤-花港观鱼', 4, 120, '环湖公交/步行衔接', 'https://westlake.hangzhou.gov.cn/'),
(11, 4, 1, 1, '云谷寺索道上山', 8, 60, '索道开放以景区公告为准', 'https://hsgwh.huangshan.gov.cn/'),
(12, 4, 1, 2, '始信峰-北海', 8, 180, '山上步道游览', 'https://hsgwh.huangshan.gov.cn/'),
(13, 4, 2, 1, '光明顶日出', 8, 90, '需结合天气和住宿位置', 'https://hsgwh.huangshan.gov.cn/'),
(14, 4, 2, 2, '玉屏楼-迎客松', 8, 150, '步行后下山', 'https://hsgwh.huangshan.gov.cn/'),
(15, 5, 1, 1, '诺日朗游客中心', 7, 30, '景区观光车换乘', 'https://www.jiuzhai.com/intelligent-service/tickets'),
(16, 5, 1, 2, '五花海-珍珠滩', 7, 150, '日则沟核心景点', 'https://www.jiuzhai.com/intelligent-service/tickets'),
(17, 5, 1, 3, '长海-五彩池', 7, 120, '则查洼沟观光车衔接', 'https://www.jiuzhai.com/intelligent-service/tickets'),
(18, 6, 1, 1, '磨盘山/竹江码头登船', 9, 30, '码头和班次以当日通知为准', 'https://wglj.guilin.gov.cn/'),
(19, 6, 1, 2, '漓江游船航段', 9, 240, '桂林至阳朔典型游船线路', 'https://wglj.guilin.gov.cn/'),
(20, 6, 1, 3, '阳朔下船散步', 9, 90, '可衔接西街或返程交通', 'https://wglj.guilin.gov.cn/');

-- 测试乘客数据
INSERT IGNORE INTO `tm_passenger` (`user_id`, `name`, `id_card`, `phone`, `type`) VALUES
(2, '张三', '110101199001011234', '13800138001', 0),
(2, '李四', '110101199002022345', '13800138002', 0),
(3, 'Alice', '440101199003033456', '13800138003', 0);

-- 优惠券表
CREATE TABLE IF NOT EXISTS `tm_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '优惠券名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `category` VARCHAR(20) DEFAULT 'all' COMMENT '使用类别：all=通用, flight=机票, train=火车票, hotel=酒店',
  `discount_type` TINYINT(1) DEFAULT '0' COMMENT '0=满减, 1=折扣',
  `discount_value` DECIMAL(10,2) DEFAULT '0.00' COMMENT '减免金额或折扣比例',
  `min_amount` DECIMAL(10,2) DEFAULT '0.00' COMMENT '最低消费金额',
  `expire_date` DATETIME DEFAULT NULL COMMENT '过期时间',
  `stock` INT DEFAULT '100' COMMENT '可领取数量',
  `status` TINYINT(1) DEFAULT '0' COMMENT '0=有效, 1=已过期',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_coupon` ADD COLUMN `category` VARCHAR(20) DEFAULT ''all'' COMMENT ''使用类别：all=通用, flight=机票, train=火车票, hotel=酒店'' AFTER `description`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_coupon' AND COLUMN_NAME = 'category');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 示例优惠券数据
INSERT IGNORE INTO `tm_coupon` (`name`, `description`, `category`, `discount_type`, `discount_value`, `min_amount`, `expire_date`, `stock`, `status`) VALUES
('新用户专享', '新用户首单立减50元', 'all', 0, 50.00, 200.00, '2026-12-31 23:59:59', 200, 0),
('机票满减券', '机票订单满500减30', 'flight', 0, 30.00, 500.00, '2026-12-31 23:59:59', 150, 0),
('酒店9折券', '酒店订单享9折优惠', 'hotel', 1, 0.90, 0.00, '2026-12-31 23:59:59', 100, 0),
('火车票85折', '火车票订单享85折', 'train', 1, 0.85, 100.00, '2026-12-31 23:59:59', 80, 0);

-- ============================================================
-- 用户已领取优惠券表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tm_user_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
  `status` TINYINT(1) DEFAULT '0' COMMENT '0=未使用, 1=已使用, 2=已过期',
  `received_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `used_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_coupon_id` (`coupon_id`),
  UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券关联表';

DELETE uc1 FROM `tm_user_coupon` uc1
JOIN `tm_user_coupon` uc2
  ON uc1.user_id = uc2.user_id
 AND uc1.coupon_id = uc2.coupon_id
 AND uc1.id > uc2.id;

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_user_coupon` ADD UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`)', 'SELECT 1') FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_user_coupon' AND INDEX_NAME = 'uk_user_coupon');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 评价回复表（商家回复）
-- ============================================================
CREATE TABLE IF NOT EXISTS `tm_reply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `review_id` BIGINT NOT NULL COMMENT '评价ID',
  `user_id` BIGINT NOT NULL COMMENT '回复者ID',
  `content` VARCHAR(1000) NOT NULL COMMENT '回复内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '0=正常, 1=已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_review_id` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价回复表';

-- ============================================================
-- 评价举报表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tm_review_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `review_id` BIGINT NOT NULL COMMENT '评价ID',
  `reporter_id` BIGINT NOT NULL COMMENT '举报者ID',
  `reason` VARCHAR(200) DEFAULT NULL COMMENT '举报原因',
  `status` TINYINT(1) DEFAULT '0' COMMENT '0=待处理, 1=已处理',
  `handle_remark` VARCHAR(300) DEFAULT NULL COMMENT '处理备注',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_review_id` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价举报表';

SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_review_report` ADD COLUMN `handle_remark` VARCHAR(300) DEFAULT NULL COMMENT ''处理备注'' AFTER `status`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_review_report' AND COLUMN_NAME = 'handle_remark');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `tm_review_report` ADD COLUMN `handle_time` DATETIME DEFAULT NULL COMMENT ''处理时间'' AFTER `handle_remark`', 'SELECT 1') FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tm_review_report' AND COLUMN_NAME = 'handle_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 补充演示数据（2026-05-21 检索整理）
-- 来源说明：
-- 1. 酒店名称、地址、坐标、评分来自携程公开搜索结果；价格为演示均价，不代表实时房价。
-- 2. 游记标题和互动量用于演示热度；正文按真实旅行场景整理。
-- 3. 酒店图片改为酒店本体真实照片；游记图片继续采用开放授权素材或同城景观图，仅用于演示。
-- ============================================================

INSERT IGNORE INTO `tm_hotel` (`id`, `name`, `city`, `address`, `star_rating`, `description`, `cover_img`, `lat`, `lng`, `avg_price`, `score`, `source_name`, `source_url`, `data_checked_date`) VALUES
(15, '全季酒店(成都太古里春熙路店)', '成都', '交通路8号中环广场2座', 4, '位于春熙路与太古里商圈周边，适合城市观光、购物和商务短住。', '/images/seed/chengdu.svg', 30.661950, 104.078639, 520.00, 4.8, '携程酒店搜索', 'https://hotels.ctrip.com/hotels/detail/?hotelid=133457358', '2026-05-21'),
(16, '成都太古里春熙美居酒店', '成都', '暑袜北三街20号东楼', 4, '靠近成都太古里与春熙路，适合轻量 citywalk 和夜间餐饮行程。', '/images/seed/chengdu.svg', 30.664968, 104.082064, 680.00, 4.8, '携程酒店搜索', 'https://hotels.ctrip.com/hotels/detail/?hotelid=21132716', '2026-05-21'),
(17, '成都瑞城名人酒店', '成都', '人民中路二段68号', 4, '临近文殊院和宽窄巷子，适合老城休闲与亲子出行。', '/images/seed/chengdu.svg', 30.676451, 104.072363, 460.00, 4.7, '携程酒店搜索', 'https://hotels.ctrip.com/hotels/detail/?hotelid=470094', '2026-05-21'),
(18, '成都东大明宇豪雅饭店(春熙路太古里店)', '成都', '东大街紫东楼段39号', 5, '位于东大街商圈，适合高星商务、太古里购物和市区美食动线。', '/images/seed/chengdu.svg', 30.652909, 104.097655, 980.00, 4.8, '携程酒店搜索', 'https://hotels.ctrip.com/hotels/detail/?hotelid=435688', '2026-05-21'),
(19, '南京金陵饭店', '南京', '新街口汉中路2号', 5, '南京新街口地标型酒店，适合城市中心商务、购物和博物馆游线。', '/images/seed/nanjing.svg', 32.042447, 118.782824, 920.00, 4.8, '携程目的地/酒店联想', 'https://hotels.ctrip.com/hotels/detail/?hotelid=346283', '2026-05-21'),
(20, '苏州吴宫泛太平洋酒店', '苏州', '姑苏区新市路259号', 5, '临近盘门和姑苏古城片区，适合园林游览与江南度假。', '/images/seed/garden.svg', 31.290418, 120.617053, 880.00, 4.7, '携程目的地/酒店联想', 'https://hotels.ctrip.com/hotels/detail/?hotelid=346290', '2026-05-21');

INSERT IGNORE INTO `tm_hotel_room` (`hotel_id`, `room_type`, `bed_type`, `area`, `price`, `total_rooms`, `available_rooms`, `facilities`) VALUES
(15, '高级大床房', '1.8m大床', 28, 458.00, 30, 21, 'WiFi,空调,淋浴,办公桌,近地铁'),
(15, '商务双床房', '2×1.2m双床', 30, 518.00, 20, 14, 'WiFi,空调,淋浴,城市景观'),
(16, '嘉宾大床房', '1.8m大床', 32, 628.00, 28, 17, 'WiFi,空调,淋浴,迷你吧,近太古里'),
(16, '家庭双床房', '1.5m+1.2m床', 38, 728.00, 12, 8, 'WiFi,空调,亲子用品,城市景观'),
(17, '文殊院舒适大床房', '1.8m大床', 30, 398.00, 25, 18, 'WiFi,空调,淋浴,早餐'),
(18, '豪华城景大床房', '2m大床', 48, 898.00, 22, 12, 'WiFi,浴缸,迷你吧,城市景观,健身房'),
(18, '行政套房', '2m大床', 82, 1680.00, 6, 3, 'WiFi,浴缸,客厅,行政礼遇'),
(19, '金陵经典大床房', '1.8m大床', 36, 820.00, 24, 15, 'WiFi,空调,浴缸,新街口景观'),
(19, '行政双床房', '2×1.2m双床', 42, 1080.00, 16, 9, 'WiFi,浴缸,行政楼层,早餐'),
(20, '姑苏园景大床房', '1.8m大床', 40, 760.00, 20, 13, 'WiFi,园景,浴缸,近盘门'),
(20, '亲子家庭房', '1.8m+1.2m床', 50, 980.00, 10, 6, 'WiFi,亲子用品,园景,停车');

INSERT IGNORE INTO `tm_media_asset` (`id`, `target_type`, `target_id`, `media_type`, `url`, `caption`, `author`, `license_name`, `license_url`, `source_url`, `source_name`, `data_checked_date`) VALUES
(22, 'hotel', 15, 'image', 'https://ak-d.tripcdn.com/images/1mc4r12000repen2v8026.jpg', '全季酒店成都太古里春熙路店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/chengdu-hotel-detail-133457358/ji-hotel/photo.html', 'Trip.com', '2026-05-23'),
(23, 'hotel', 19, 'image', 'https://ak-d.tripcdn.com/images/02064120008bk81fz92B1_R_960_660_R5_D.jpg', '南京金陵饭店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/nanjing-hotel-detail-346283/jinling-hotel/', 'Trip.com', '2026-05-23'),
(24, 'hotel', 20, 'image', 'https://ak-d.tripcdn.com/images/1mc6o12000br4n333F7C1_R_960_660_R5_D.jpg', '苏州吴宫泛太平洋酒店外观', 'Trip.com 酒店图库', '平台图片，仅用于课程演示引用', NULL, 'https://www.trip.com/hotels/suzhou-hotel-detail-346290/pan-pacific-suzhou/', 'Trip.com', '2026-05-23');

UPDATE `tm_hotel`
SET `cover_img` = CASE `id`
  WHEN 15 THEN 'https://ak-d.tripcdn.com/images/1mc4r12000repen2v8026.jpg'
  WHEN 16 THEN 'https://ak-d.tripcdn.com/images/20060v000000jo62xE3EB.jpg'
  WHEN 17 THEN 'https://ak-d.tripcdn.com/images/200m1e000001fvyd2AB3D_R_960_660_R5_D.jpg'
  WHEN 18 THEN 'https://ak-d.tripcdn.com/images/1mc1712000epu78z56B04_R_960_660_R5_D.jpg'
  WHEN 19 THEN 'https://ak-d.tripcdn.com/images/02064120008bk81fz92B1_R_960_660_R5_D.jpg'
  WHEN 20 THEN 'https://ak-d.tripcdn.com/images/1mc6o12000br4n333F7C1_R_960_660_R5_D.jpg'
  ELSE `cover_img`
END
WHERE `id` BETWEEN 15 AND 20;

INSERT INTO `tm_post` (`id`, `user_id`, `title`, `content`, `images`, `destination`, `tags`, `like_count`, `comment_count`, `collect_count`, `view_count`, `status`, `source_name`, `source_url`, `data_checked_date`) VALUES
(6, 3, '重庆3天2晚｜洪崖洞山城步道轻松版', '重庆三天两晚建议住解放碑、较场口或临江门附近，吃饭、看夜景和坐轨道交通都方便。第一天把解放碑、八一好吃街、洪崖洞和千厮门大桥放在一起，白天不用太赶，傍晚再去洪崖洞外侧找机位，亮灯后从桥上看江景更完整。第二天走鹅岭二厂、李子坝、山城步道、十八梯和白象居，几个点都要上下坡，鞋子一定要舒服，中间可以在观音桥或较场口安排一顿火锅。第三天按返程时间选择磁器口或观音桥，行李最好寄存在酒店或地铁站。重庆看地图距离不远，但爬坡、排队和换乘都很耗体力，每天留一点空档会舒服很多。', '/images/seed/chongqing.svg,/images/seed/chongqing.svg', '重庆', '重庆,洪崖洞,山城步道,三天两晚,夜景', 1759, 86, 1480, 8200, 1, '公开路线资料整理', NULL, '2026-05-21'),
(7, 4, '重庆特种兵路线｜轻轨夜景火锅都要有', '这条重庆周末路线适合只有两天一晚、又想把经典点位都看一眼的人。第一天上午先去李子坝看轻轨穿楼，随后到鹅岭二厂、鹅岭公园一带慢慢走，午后切到湖广会馆和白象居，老建筑、江面和居民楼层次很适合拍照。傍晚去来福士或朝天门看两江交汇，晚上再到洪崖洞和千厮门大桥，灯光比白天更有山城氛围。第二天可以睡到自然醒，吃一顿小面或江湖菜，再去山城步道、十八梯补一段 citywalk。热门机位不用死排，重庆的惊喜经常在路边楼梯、天桥和转角。', '/images/seed/chongqing.svg,/images/seed/chongqing.svg', '重庆', '重庆,李子坝,火锅,夜景,周末游', 1245, 54, 1190, 6900, 1, '公开路线资料整理', NULL, '2026-05-21'),
(8, 2, '厦门3天2晚｜鼓浪屿沙坡尾环岛路', '厦门三天两晚可以按“老城、海岛、海边”来分。第一天下午到达后先去中山路和八市，海鲜小吃和骑楼街景都集中，傍晚转到沙坡尾、演武大桥观景平台和双子塔周边，晚上不必再跑太远。第二天预留给鼓浪屿，船票要提前约，建议从三丘田码头进岛，沿着最美转角、日光岩周边、菽庄花园和海边步道慢慢走，岛上岔路多，留半天以上才不会只顾赶路。第三天走南普陀、厦大白城、胡里山炮台和环岛路，天气好可以租车或骑行一小段。厦门节奏适合慢下来，轮渡时间、海风和阵雨都要提前看。', '/images/seed/coast.svg', '厦门', '厦门,鼓浪屿,沙坡尾,环岛路,海边', 7270, 211, 6380, 26800, 1, '公开路线资料整理', NULL, '2026-05-21'),
(9, 3, '江南串线｜杭州绍兴乌镇苏州上海', '江南多城串线建议从杭州进、上海出，中间尽量用高铁和短途打车衔接，不要每天拖着行李换太远。杭州可以留一天半，白天走西湖、柳浪闻莺和湖滨，晚上看钱塘江或武林夜景；第二站到绍兴，鲁迅故里、仓桥直街、书圣故里和黄酒小馆都集中，节奏比杭州慢。乌镇适合住一晚，傍晚进西栅看夜景，第二天早上再拍清净的桥和河道。之后到苏州，把拙政园、苏州博物馆、平江路和山塘街放在同一天，最后用上海外滩、陆家嘴和武康路收尾。城市多时最容易累，酒店尽量选在高铁站或地铁换乘点附近，少折返就是省体力。', '/images/seed/hangzhou.svg,/images/seed/garden.svg,/images/seed/shanghai.svg', '江南', '杭州,绍兴,乌镇,苏州,上海,江南路线', 587, 32, 520, 4100, 1, '公开路线资料整理', NULL, '2026-05-21'),
(10, 4, '暑期热门旅行地图｜9城灵感清单', '暑期选目的地可以先按旅行目标分组，而不是只看热度。第一次带家人出游，北京适合看故宫、长城和博物馆，西安适合兵马俑、城墙和大唐不夜城，南京适合中山陵、南京博物院和秦淮河，文化线比较完整。想看海可以选厦门、青岛、三亚，厦门轻松，青岛城市和海岸线结合得好，三亚更适合度假酒店和海岛项目。喜欢吃喝和城市烟火气，成都、重庆、长沙都很稳，但夏天要注意高温和排队。真正订票前建议先看天气、门票预约、酒店位置和每天交通余量，把必去点控制在 2 到 3 个，体验会比满地图打卡更真实。', '/images/seed/beijing.svg,/images/seed/qingdao.svg,/images/seed/chongqing.svg', '全国', '暑期旅行,城市清单,亲子,海边,旅行灵感', 1397, 74, 1280, 7600, 1, '公开路线资料整理', NULL, '2026-05-21')
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `content` = VALUES(`content`),
  `images` = VALUES(`images`),
  `destination` = VALUES(`destination`),
  `tags` = VALUES(`tags`),
  `like_count` = VALUES(`like_count`),
  `comment_count` = VALUES(`comment_count`),
  `collect_count` = VALUES(`collect_count`),
  `view_count` = VALUES(`view_count`),
  `status` = VALUES(`status`),
  `source_name` = VALUES(`source_name`),
  `source_url` = VALUES(`source_url`),
  `data_checked_date` = VALUES(`data_checked_date`);

INSERT IGNORE INTO `tm_tour_product` (`id`, `name`, `description`, `tour_type`, `departure_city`, `destination`, `duration`, `price`, `cover_img`, `source_name`, `source_url`, `data_checked_date`) VALUES
(7, '重庆山城夜景三日游', '结合重庆公开文旅信息和真实游客常用动线整理，覆盖解放碑、洪崖洞、李子坝、山城步道等节点。价格为演示服务费。', 1, '重庆', '重庆主城', '3天2晚', 468.00, '/images/seed/chongqing.svg', '重庆市文旅委公开信息/路线场景整理', 'https://whlyw.cq.gov.cn/', '2026-05-21'),
(8, '厦门鼓浪屿三日慢游', '结合鼓浪屿管委会公开信息和厦门经典慢游动线整理，覆盖鼓浪屿、沙坡尾、环岛路等节点。价格为演示服务费。', 1, '厦门', '厦门', '3天2晚', 498.00, '/images/seed/coast.svg', '鼓浪屿管委会公开信息/路线场景整理', 'https://gly.xm.gov.cn/', '2026-05-21'),
(9, '南京博物馆中山陵一日游', '基于携程南京目的地联想和南京钟山风景区公开信息整理，适合城市历史文化轻量游。价格为演示服务费。', 0, '南京', '南京', '1天', 158.00, '/images/seed/nanjing.svg', '携程目的地搜索/南京钟山风景区官网', 'https://zschina.nanjing.gov.cn/', '2026-05-21'),
(10, '苏州园林平江路一日游', '基于携程苏州目的地联想和拙政园公开信息整理，覆盖古典园林、平江路和山塘街。价格为演示服务费。', 0, '苏州', '苏州古城', '1天', 168.00, '/images/seed/garden.svg', '携程目的地搜索/拙政园官网', 'https://www.szzzy.cn/', '2026-05-21');

INSERT IGNORE INTO `tm_tour_product_step` (`id`, `product_id`, `day_no`, `sequence_no`, `place_name`, `attraction_id`, `stay_minutes`, `transport_note`, `source_url`) VALUES
(21, 7, 1, 1, '解放碑-八一好吃街', NULL, 120, '市中心步行，适合抵达日', 'https://whlyw.cq.gov.cn/'),
(22, 7, 1, 2, '洪崖洞-千厮门大桥夜景', 24, 120, '傍晚后步行或轨道交通前往', 'https://whlyw.cq.gov.cn/'),
(23, 7, 2, 1, '李子坝-鹅岭二厂', NULL, 150, '轨道交通二号线衔接', 'https://www.xiaohongshu.com/search_result/69527e29000000001e0104e3?xsec_token=AB6Nmx2NPTIida2wNAZKoe5hcIEHgoQBrmGY5RMJvBJF4=&xsec_source='),
(24, 7, 2, 2, '山城步道-十八梯', NULL, 180, '坡道较多，建议轻装', 'https://whlyw.cq.gov.cn/'),
(25, 8, 1, 1, '中山路-八市-沙坡尾', NULL, 240, '厦门岛内步行和公交衔接', 'https://www.xiaohongshu.com/search_result/69d2089a000000002103baff?xsec_token=ABa2URARwU2dy6rXtRtvuFSOOlogh_zRR6bspLh5r2M-8=&xsec_source='),
(26, 8, 2, 1, '鼓浪屿街巷慢游', 21, 300, '轮渡票和上岛时间需提前确认', 'https://gly.xm.gov.cn/'),
(27, 8, 3, 1, '南普陀-白城沙滩-环岛路', NULL, 240, '公交/骑行衔接，注意天气', 'https://www.xiaohongshu.com/search_result/69d2089a000000002103baff?xsec_token=ABa2URARwU2dy6rXtRtvuFSOOlogh_zRR6bspLh5r2M-8=&xsec_source='),
(28, 9, 1, 1, '南京博物院', NULL, 180, '热门展馆建议提前预约', 'https://you.ctrip.com/sight/%E5%8D%97%E4%BA%AC12/4190936.html'),
(29, 9, 1, 2, '中山陵', 22, 150, '景区内步行，注意闭馆日', 'https://zschina.nanjing.gov.cn/'),
(30, 10, 1, 1, '拙政园', 23, 150, '早到避开客流高峰', 'https://www.szzzy.cn/'),
(31, 10, 1, 2, '平江路', NULL, 120, '古城步行街区', 'https://you.ctrip.com/place/%E8%8B%8F%E5%B7%9E14.html'),
(32, 10, 1, 3, '山塘街夜游', NULL, 120, '傍晚后游览更适合拍照', 'https://you.ctrip.com/sight/%E8%8B%8F%E5%B7%9E14/8168464.html');

INSERT IGNORE INTO `tm_review` (`id`, `user_id`, `target_id`, `target_type`, `order_id`, `rating`, `content`, `images`, `tags`) VALUES
(1, 2, 15, 0, NULL, 5, '位置很适合春熙路和太古里行程，晚上回酒店也方便，房间干净度不错。', NULL, '位置方便,干净卫生,适合短住'),
(2, 3, 16, 0, NULL, 5, '步行到太古里很顺，早餐和前台服务都在线，适合第一次来成都住市中心。', NULL, '服务好,商圈方便,早餐不错'),
(3, 4, 19, 0, NULL, 5, '新街口出行太方便了，去南京博物院和中山陵打车都不远，整体很稳。', NULL, '地铁方便,老牌酒店,商务出行'),
(4, 2, 20, 0, NULL, 4, '酒店园林感很强，离古城景点近，房间略老但氛围很好。', NULL, '园林风格,亲子友好,近景区'),
(5, 3, 24, 1, NULL, 5, '洪崖洞夜景确实很出片，但人多的时候要提前找好返程路线。', '/images/seed/chongqing.svg', '夜景好看,人气高,拍照出片'),
(6, 4, 21, 1, NULL, 5, '鼓浪屿适合慢慢走，别把点位排太满，傍晚海边风很舒服。', '/images/seed/coast.svg', '适合慢游,海岛,亲子友好');

INSERT IGNORE INTO `tm_reply` (`id`, `review_id`, `user_id`, `content`) VALUES
(1, 1, 1, '感谢反馈，春熙路商圈房源后续会继续补充不同价位选择。'),
(2, 3, 1, '谢谢分享，南京城市文化线路会继续补充夫子庙和老门东组合玩法。'),
(3, 5, 1, '已收到夜景返程建议，后续行程详情会增加轨道交通和错峰提示。');

INSERT IGNORE INTO `tm_review_report` (`id`, `review_id`, `reporter_id`, `reason`, `status`) VALUES
(1, 5, 2, '疑似包含过时排队信息，请管理员核验', 0);

INSERT IGNORE INTO `tm_user_coupon` (`id`, `user_id`, `coupon_id`, `status`, `received_time`, `used_time`) VALUES
(1, 2, 1, 0, '2026-05-21 09:10:00', NULL),
(2, 2, 3, 0, '2026-05-21 09:12:00', NULL),
(3, 3, 2, 0, '2026-05-21 10:05:00', NULL),
(4, 4, 4, 0, '2026-05-21 11:20:00', NULL);

-- ============================================================
-- 继续补充演示数据：更多城市、酒店、路线与社区互动
-- ============================================================

INSERT IGNORE INTO `tm_hotel` (`id`, `name`, `city`, `address`, `star_rating`, `description`, `cover_img`, `lat`, `lng`, `avg_price`, `score`, `source_name`, `source_url`, `data_checked_date`) VALUES
(21, '重庆解放碑来福士雅诗阁服务公寓', '重庆', '渝中区接圣街6号', 5, '位于朝天门和来福士商圈，适合家庭、长住和两江夜景行程。', '/images/seed/chongqing.svg', 29.566900, 106.588300, 1180.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-21'),
(22, '南京夫子庙亚朵酒店', '南京', '秦淮区建康路附近', 4, '靠近夫子庙和秦淮河片区，适合夜游、家庭和周末短途。', '/images/seed/nanjing.svg', 32.023805, 118.791212, 560.00, 4.7, '携程目的地/酒店联想', 'https://hotels.ctrip.com/hotels/detail/?hotelid=8838971', '2026-05-21'),
(23, '苏州观前平江美居酒店', '苏州', '姑苏区观前平江片区', 4, '位于观前街和平江路周边，适合园林游、古城步行和美食探索。', '/images/seed/garden.svg', 31.308031, 120.635090, 620.00, 4.7, '携程目的地/酒店联想', 'https://hotels.ctrip.com/hotels/detail/?hotelid=64962190', '2026-05-21'),
(24, '厦门鼓浪屿海景度假酒店', '厦门', '思明区鼓浪屿内厝澳片区', 4, '面向鼓浪屿慢游和海岛度假场景，适合住岛看日落。', '/images/seed/coast.svg', 24.447900, 118.061600, 720.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-21'),
(25, '长沙五一广场国金中心酒店', '长沙', '芙蓉区五一广场商圈', 4, '临近五一广场、IFS和黄兴路步行街，适合美食与夜生活行程。', 'https://ak-d.tripcdn.com/images/1mc0q12000mffam9bF93D.jpg', 28.193300, 112.976900, 520.00, 4.6, 'Trip.com 酒店图库/演示价格', 'https://sa.trip.com/hotels/changsha-hotel-detail-114935658/quan-ji-jiu-dian/photo.html', '2026-05-23'),
(26, '青岛八大关海景酒店', '青岛', '市南区八大关风景区附近', 4, '靠近八大关、第二海水浴场和栈桥片区，适合海滨城市度假。', '/images/seed/qingdao.svg', 36.055300, 120.343600, 860.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-21');

INSERT IGNORE INTO `tm_hotel_room` (`hotel_id`, `room_type`, `bed_type`, `area`, `price`, `total_rooms`, `available_rooms`, `facilities`) VALUES
(21, '两江景观大床房', '1.8m大床', 45, 980.00, 18, 9, 'WiFi,江景,洗衣机,厨房,浴缸'),
(21, '家庭双卧套房', '2间卧室', 88, 1680.00, 8, 4, 'WiFi,江景,厨房,客厅,洗衣机'),
(22, '秦淮舒适大床房', '1.8m大床', 30, 498.00, 26, 18, 'WiFi,空调,淋浴,近夫子庙'),
(22, '秦淮亲子房', '1.8m+1.2m床', 38, 658.00, 12, 7, 'WiFi,亲子用品,早餐,近地铁'),
(23, '平江路大床房', '1.8m大床', 32, 568.00, 22, 13, 'WiFi,空调,淋浴,近园林'),
(23, '观前双床房', '2×1.2m双床', 34, 628.00, 18, 10, 'WiFi,空调,早餐,步行街'),
(24, '海岛庭院大床房', '1.8m大床', 30, 680.00, 16, 8, 'WiFi,庭院,近码头,早餐'),
(24, '鼓浪屿海景房', '1.8m大床', 36, 920.00, 10, 5, 'WiFi,海景,阳台,早餐'),
(25, '五一广场城景房', '1.8m大床', 32, 468.00, 28, 20, 'WiFi,空调,城市景观,近地铁'),
(25, '黄兴路双床房', '2×1.2m双床', 35, 528.00, 20, 12, 'WiFi,空调,淋浴,步行街'),
(26, '八大关海景大床房', '1.8m大床', 38, 780.00, 18, 9, 'WiFi,海景,浴缸,近海滩'),
(26, '亲子海景套房', '1.8m+1.2m床', 55, 1180.00, 8, 3, 'WiFi,亲子用品,海景,客厅');

INSERT INTO `tm_attraction` (`id`, `name`, `city`, `address`, `description`, `cover_img`, `adult_price`, `child_price`, `total_tickets`, `available_tickets`, `open_time`, `lat`, `lng`, `official_url`, `source_name`, `data_checked_date`) VALUES
(29, '夫子庙秦淮风光带', '南京', '秦淮区夫子庙贡院街', '南京代表性历史文化街区，夜游秦淮河、江南贡院和夫子庙商圈集中分布。', '/images/seed/nanjing.svg', 0.00, 0.00, 9999, 9999, '全天开放（游船和场馆另行开放）', 32.020600, 118.788900, 'https://wlj.nanjing.gov.cn/', '南京市文化和旅游局公开信息', '2026-05-21'),
(30, '平江路历史街区', '苏州', '姑苏区平江路', '苏州古城代表性历史街区，保留河街并行格局，适合步行和夜游。', '/images/seed/garden.svg', 0.00, 0.00, 9999, 9999, '全天开放（商户营业时间不一）', 31.314700, 120.633700, 'https://wglj.suzhou.gov.cn/', '苏州市文化广电和旅游局公开信息', '2026-05-21'),
(31, '沙坡尾艺术西区', '厦门', '思明区沙坡尾', '厦门老港口与城市更新街区，集合咖啡、展览、市集和海边步行体验。', '/images/seed/coast.svg', 0.00, 0.00, 9999, 9999, '全天开放（商户营业时间不一）', 24.437200, 118.091400, 'https://wlj.xm.gov.cn/', '厦门市文化和旅游局公开信息', '2026-05-21'),
(32, '橘子洲景区', '长沙', '岳麓区橘子洲头', '湘江江心洲景区，是长沙城市观光和夜游烟花活动的重要地标。', '/images/seed/attraction.svg', 0.00, 0.00, 9999, 9999, '07:00-22:00（以景区公告为准）', 28.181900, 112.959500, 'https://wlj.changsha.gov.cn/', '长沙市文化旅游广电局公开信息', '2026-05-21'),
(33, '八大关风景区', '青岛', '市南区汇泉角东北部', '青岛海滨历史建筑街区，适合城市漫步、建筑观赏和海边拍照。', '/images/seed/qingdao.svg', 0.00, 0.00, 9999, 9999, '全天开放（部分建筑另行开放）', 36.050200, 120.347800, 'https://whlyj.qingdao.gov.cn/', '青岛市文化和旅游局公开信息', '2026-05-21')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `city` = VALUES(`city`),
  `address` = VALUES(`address`),
  `description` = VALUES(`description`),
  `cover_img` = VALUES(`cover_img`),
  `adult_price` = VALUES(`adult_price`),
  `child_price` = VALUES(`child_price`),
  `open_time` = VALUES(`open_time`),
  `lat` = VALUES(`lat`),
  `lng` = VALUES(`lng`),
  `official_url` = VALUES(`official_url`),
  `source_name` = VALUES(`source_name`),
  `data_checked_date` = VALUES(`data_checked_date`);

INSERT INTO `tm_post` (`id`, `user_id`, `title`, `content`, `images`, `destination`, `tags`, `like_count`, `comment_count`, `collect_count`, `view_count`, `status`, `source_name`, `source_url`, `data_checked_date`) VALUES
(11, 2, '南京2天1晚｜博物院夫子庙中山陵', '南京两天一晚适合住新街口、夫子庙或大行宫，地铁换乘方便，晚上吃饭也不愁。第一天上午留给南京博物院，展厅很大，最好提前预约并预留 3 小时；午后可以去总统府，或者转到老门东、中华门一带慢慢走。傍晚到夫子庙和秦淮河，不一定要排游船，沿河散步、看灯和找小吃也很有南京味。第二天早起去中山陵，台阶多但视野开阔，音乐台看鸽子要看当天开放和天气，时间宽裕再接玄武湖或鸡鸣寺。南京很多景点周末客流明显，博物馆、热门餐厅和中山陵接驳都要提前留时间。', '/images/seed/nanjing.svg', '南京', '南京,夫子庙,南京博物院,中山陵,周末游', 1830, 64, 1660, 9200, 1, '公开路线资料整理', NULL, '2026-05-21'),
(12, 3, '苏州1天｜拙政园平江路山塘街', '苏州一日游最舒服的方式是只抓住古城核心，不把同类园林塞太多。早上开园后先进拙政园，游客少、光线也柔和，出来后可以顺路看苏州博物馆外观或提前预约进馆。午后沿平江路慢慢走，茶馆、评弹、小桥和水巷都适合放慢速度，中间可以绕到双塔市集吃点小吃。傍晚去山塘街，看灯光倒映在河面上，比白天更有氛围。古城里打车常常不如步行和地铁稳定，行程尽量安排在同一片区，雨天带伞也比赶车更重要。', '/images/seed/garden.svg', '苏州', '苏州,拙政园,平江路,山塘街,江南', 1246, 38, 1090, 6100, 1, '公开路线资料整理', NULL, '2026-05-21'),
(13, 4, '长沙周末｜五一广场橘子洲岳麓山', '长沙周末更适合轻松吃喝，不要把每个网红店都排成任务。第一天下午到五一广场附近入住，先走黄兴路、坡子街和太平老街，晚上按队伍情况选湘菜、小吃或茶饮，热门店排队久就果断换一家。第二天上午去橘子洲，观光车能省不少脚力，夏天一定要防晒；下午到岳麓山和湖南大学，想轻松可以索道上、步行下，顺路看爱晚亭和校园街区。晚上回市区再补一顿夜宵。长沙地铁方便，但商圈人流密集，住宿选五一广场外一两站会安静很多。', '/images/seed/attraction.svg', '长沙', '长沙,橘子洲,岳麓山,五一广场,美食', 986, 41, 820, 5300, 1, '公开路线资料整理', NULL, '2026-05-21'),
(14, 2, '青岛3天｜栈桥八大关小麦岛看海', '青岛三天建议把老城、海岸线和崂山方向分开，不然每天都在跨城。第一天从栈桥、天主教堂、信号山和大学路开始，老城区坡路多但很适合慢慢走，傍晚去小鱼山或琴屿路看海。第二天安排八大关、第二海水浴场、太平角和小麦岛，天气晴的时候海水颜色很漂亮，下午风大要带外套。第三天看体力选择石老人、崂山或啤酒博物馆，想轻松就留在市区吃海鲜、逛市南老街。青岛看海很吃天气，晴天优先拍海边，阴天就换成老建筑和博物馆路线。', '/images/seed/qingdao.svg', '青岛', '青岛,栈桥,八大关,小麦岛,看海', 1468, 52, 1330, 7900, 1, '公开路线资料整理', NULL, '2026-05-21')
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `content` = VALUES(`content`),
  `images` = VALUES(`images`),
  `destination` = VALUES(`destination`),
  `tags` = VALUES(`tags`),
  `like_count` = VALUES(`like_count`),
  `comment_count` = VALUES(`comment_count`),
  `collect_count` = VALUES(`collect_count`),
  `view_count` = VALUES(`view_count`),
  `status` = VALUES(`status`),
  `source_name` = VALUES(`source_name`),
  `source_url` = VALUES(`source_url`),
  `data_checked_date` = VALUES(`data_checked_date`);

INSERT IGNORE INTO `tm_comment` (`post_id`, `user_id`, `content`, `like_count`) VALUES
(11, 3, '南京博物院真的要早点约，临时去很容易没票。', 18),
(11, 4, '夫子庙晚上氛围挺好，但节假日人会特别多。', 11),
(12, 2, '苏州古城建议穿舒服鞋，平江路慢慢逛很舒服。', 15),
(13, 3, '长沙排队时间真的要算进去，不然一天全在等。', 9),
(14, 4, '青岛看海天气太关键了，阴天和晴天完全两个城市。', 21);

-- ============================================================
-- 交通与酒店补充数据：机票、火车票、酒店
-- ============================================================

INSERT INTO `tm_flight` (`id`, `flight_no`, `airline`, `departure_city`, `arrival_city`, `departure_time`, `arrival_time`, `economy_price`, `business_price`, `total_seats`, `available_seats`, `status`) VALUES
(101, 'MU2517', '中国东方航空', '上海', '南京', '2026-06-05 08:20:00', '2026-06-05 09:30:00', 360.00, 1180.00, 160, 72, 1),
(102, 'HO1689', '吉祥航空', '上海', '厦门', '2026-06-05 10:15:00', '2026-06-05 12:05:00', 520.00, 1680.00, 170, 81, 1),
(103, '3U8766', '四川航空', '上海', '重庆', '2026-06-05 13:40:00', '2026-06-05 16:25:00', 720.00, 2260.00, 180, 64, 1),
(104, 'CZ3125', '中国南方航空', '广州', '长沙', '2026-06-05 09:10:00', '2026-06-05 10:25:00', 390.00, 1260.00, 190, 95, 1),
(105, 'SC4668', '山东航空', '北京', '青岛', '2026-06-05 07:45:00', '2026-06-05 09:05:00', 430.00, 1380.00, 165, 88, 1),
(106, 'CA1847', '中国国际航空', '北京', '厦门', '2026-06-05 11:20:00', '2026-06-05 14:25:00', 860.00, 2680.00, 200, 56, 1),
(107, 'MF8402', '厦门航空', '厦门', '北京', '2026-06-06 09:30:00', '2026-06-06 12:35:00', 820.00, 2580.00, 190, 66, 1),
(108, 'HU7358', '海南航空', '三亚', '上海', '2026-06-06 15:20:00', '2026-06-06 18:35:00', 930.00, 2980.00, 185, 41, 1),
(109, 'GS7892', '天津航空', '西安', '青岛', '2026-06-06 12:05:00', '2026-06-06 14:15:00', 610.00, 1880.00, 170, 74, 1),
(110, 'CA1429', '中国国际航空', '成都', '杭州', '2026-06-06 08:55:00', '2026-06-06 11:20:00', 690.00, 2180.00, 180, 83, 1),
(111, 'MU2473', '中国东方航空', '杭州', '成都', '2026-06-06 18:10:00', '2026-06-06 20:55:00', 730.00, 2280.00, 180, 79, 1),
(112, 'CZ6216', '中国南方航空', '重庆', '广州', '2026-06-07 16:35:00', '2026-06-07 18:35:00', 540.00, 1760.00, 190, 102, 1)
ON DUPLICATE KEY UPDATE
  `flight_no` = VALUES(`flight_no`),
  `airline` = VALUES(`airline`),
  `departure_city` = VALUES(`departure_city`),
  `arrival_city` = VALUES(`arrival_city`),
  `departure_time` = VALUES(`departure_time`),
  `arrival_time` = VALUES(`arrival_time`),
  `economy_price` = VALUES(`economy_price`),
  `business_price` = VALUES(`business_price`),
  `total_seats` = VALUES(`total_seats`),
  `available_seats` = VALUES(`available_seats`),
  `status` = VALUES(`status`);

INSERT INTO `tm_train` (`id`, `train_no`, `train_type`, `departure_station`, `arrival_station`, `departure_time`, `arrival_time`, `duration_minutes`, `first_class_price`, `second_class_price`, `first_class_seats`, `second_class_seats`, `status`) VALUES
(101, 'G7001', '高铁', '上海', '南京', '2026-06-05 08:00:00', '2026-06-05 09:39:00', 99, 229.00, 143.00, 46, 310, 1),
(102, 'G7136', '高铁', '上海虹桥', '苏州', '2026-06-05 09:12:00', '2026-06-05 09:42:00', 30, 59.00, 39.50, 40, 280, 1),
(103, 'G1655', '高铁', '上海虹桥', '厦门北', '2026-06-05 09:25:00', '2026-06-05 15:20:00', 355, 635.00, 397.00, 34, 240, 1),
(104, 'G1337', '高铁', '上海虹桥', '长沙南', '2026-06-05 08:21:00', '2026-06-05 13:05:00', 284, 758.00, 478.00, 38, 260, 1),
(105, 'G1974', '高铁', '重庆西', '成都东', '2026-06-05 10:15:00', '2026-06-05 11:31:00', 76, 154.00, 96.00, 50, 350, 1),
(106, 'G2058', '高铁', '青岛北', '北京南', '2026-06-05 14:36:00', '2026-06-05 18:45:00', 249, 536.00, 336.00, 42, 260, 1),
(107, 'D2281', '动车', '南京南', '厦门北', '2026-06-06 07:18:00', '2026-06-06 16:10:00', 532, 662.00, 414.00, 28, 220, 1),
(108, 'G1482', '高铁', '长沙南', '南京南', '2026-06-06 11:08:00', '2026-06-06 15:42:00', 274, 641.00, 402.00, 36, 250, 1),
(109, 'G7572', '高铁', '杭州东', '苏州', '2026-06-06 13:15:00', '2026-06-06 14:42:00', 87, 152.00, 95.00, 44, 300, 1),
(110, 'D2921', '动车', '青岛', '上海虹桥', '2026-06-07 07:40:00', '2026-06-07 14:29:00', 409, 481.00, 301.00, 35, 260, 1)
ON DUPLICATE KEY UPDATE
  `train_no` = VALUES(`train_no`),
  `train_type` = VALUES(`train_type`),
  `departure_station` = VALUES(`departure_station`),
  `arrival_station` = VALUES(`arrival_station`),
  `departure_time` = VALUES(`departure_time`),
  `arrival_time` = VALUES(`arrival_time`),
  `duration_minutes` = VALUES(`duration_minutes`),
  `first_class_price` = VALUES(`first_class_price`),
  `second_class_price` = VALUES(`second_class_price`),
  `first_class_seats` = VALUES(`first_class_seats`),
  `second_class_seats` = VALUES(`second_class_seats`),
  `status` = VALUES(`status`);

INSERT INTO `tm_hotel` (`id`, `name`, `city`, `address`, `star_rating`, `description`, `cover_img`, `lat`, `lng`, `avg_price`, `score`, `source_name`, `source_url`, `data_checked_date`) VALUES
(31, '北京前门建国饭店', '北京', '西城区永安路175号', 4, '临近前门、大栅栏和天安门南侧，适合中轴线和老城游览。', '/images/seed/beijing.svg', 39.889600, 116.393700, 760.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-21'),
(32, '上海人民广场南京东路珍宝酒店', '上海', '黄浦区人民广场南京东路片区', 4, '靠近人民广场、南京路步行街和外滩，适合上海 citywalk。', '/images/seed/shanghai.svg', 31.235800, 121.478900, 720.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-21'),
(33, '杭州湖滨银泰亚朵酒店', '杭州', '上城区湖滨银泰商圈', 4, '临近西湖湖滨和地铁站，适合周末短住与亲子出行。', '/images/seed/hangzhou.svg', 30.257200, 120.165400, 680.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-21'),
(34, '西安钟楼鼓楼美居酒店', '西安', '碑林区钟楼商圈', 4, '靠近钟楼、鼓楼和回民街，适合古城夜游与美食路线。', '/images/seed/xian.svg', 34.261100, 108.942100, 520.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-21'),
(35, '三亚海棠湾度假酒店', '三亚', '海棠区海棠湾国家海岸', 5, '面向亲子度假和免税购物场景，适合海棠湾慢旅行。', '/images/seed/sanya.svg', 18.307400, 109.736600, 1680.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-21'),
(36, '广州珠江新城雅致酒店', '广州', '天河区珠江新城商圈', 4, '临近花城广场、广州塔和珠江夜游动线，适合商务和城市观光。', '/images/seed/attraction.svg', 23.120900, 113.324400, 690.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-21')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `city` = VALUES(`city`),
  `address` = VALUES(`address`),
  `star_rating` = VALUES(`star_rating`),
  `description` = VALUES(`description`),
  `cover_img` = VALUES(`cover_img`),
  `lat` = VALUES(`lat`),
  `lng` = VALUES(`lng`),
  `avg_price` = VALUES(`avg_price`),
  `score` = VALUES(`score`),
  `source_name` = VALUES(`source_name`),
  `source_url` = VALUES(`source_url`),
  `data_checked_date` = VALUES(`data_checked_date`);

INSERT IGNORE INTO `tm_hotel_room` (`hotel_id`, `room_type`, `bed_type`, `area`, `price`, `total_rooms`, `available_rooms`, `facilities`) VALUES
(31, '前门舒适大床房', '1.8m大床', 30, 658.00, 24, 16, 'WiFi,空调,淋浴,近地铁'),
(31, '中轴线家庭房', '1.8m+1.2m床', 42, 858.00, 12, 7, 'WiFi,亲子用品,早餐,近前门'),
(32, '南京路景观大床房', '1.8m大床', 32, 698.00, 26, 17, 'WiFi,空调,城市景观,近外滩'),
(32, '人民广场双床房', '2×1.2m双床', 34, 758.00, 18, 11, 'WiFi,空调,淋浴,近地铁'),
(33, '湖滨雅致大床房', '1.8m大床', 31, 628.00, 22, 14, 'WiFi,空调,早餐,近西湖'),
(33, '西湖亲子房', '1.8m+1.2m床', 40, 828.00, 10, 5, 'WiFi,亲子用品,近湖滨,早餐'),
(34, '钟楼城景大床房', '1.8m大床', 30, 468.00, 24, 18, 'WiFi,空调,城市景观,近地铁'),
(34, '鼓楼家庭房', '1.8m+1.2m床', 38, 628.00, 12, 8, 'WiFi,亲子用品,近回民街'),
(35, '海棠湾园景房', '2m大床', 55, 1380.00, 24, 12, 'WiFi,阳台,泳池,亲子活动'),
(35, '海景亲子套房', '2m大床+儿童床', 82, 2380.00, 8, 3, 'WiFi,海景,浴缸,亲子用品,行政礼遇'),
(36, '珠江新城商务大床房', '1.8m大床', 34, 620.00, 26, 15, 'WiFi,办公桌,城市景观,近地铁'),
(36, '广州塔景观房', '1.8m大床', 42, 860.00, 12, 6, 'WiFi,塔景,浴缸,迷你吧');

-- ============================================================
-- 补充景点数据（景点 29–48）
-- 来源：武汉、西藏、新疆、甘肃、四川、山东、云南、福建等地区景点
-- 数据核验日期：2026-05-19
-- ============================================================
INSERT INTO `tm_attraction` (`id`, `name`, `city`, `address`, `description`, `cover_img`, `adult_price`, `child_price`, `total_tickets`, `available_tickets`, `open_time`, `lat`, `lng`, `official_url`, `source_name`, `data_checked_date`) VALUES
(29, '黄鹤楼', '武汉', '武昌区蛇山峰岭之上',
  '中国历史名楼之一，矗立于武汉蛇山，登楼可俯瞰长江与武汉三镇城市风貌，是湖北省标志性文化地标。',
  '/images/seed/temple.svg',
  70.00, 35.00, 2000, 1456, '08:00-18:00（旺季，停止入场17:30）',
  30.5447, 114.3029, 'https://www.yhl.com.cn/', '黄鹤楼公园官网', '2026-05-19'),

(30, '布达拉宫', '拉萨', '城关区布达拉宫广场',
  '矗立于拉萨红山之上，历代达赖喇嘛冬宫，世界文化遗产，藏传佛教圣地。每日严格限流，须至少提前3天预约。',
  '/images/seed/temple.svg',
  200.00, 0.00, 2300, 342, '09:00-15:00（每日限流，须提前预约）',
  29.6578, 91.1174, 'https://www.potalapalace.cn/', '布达拉宫管理处官网', '2026-05-19'),

(31, '纳木错', '拉萨', '当雄县纳木错湖区',
  '藏语意为"天湖"，海拔4718米，西藏最大湖泊，以圣湖美景和雪山倒影闻名。高海拔须注意高原反应。',
  '/images/seed/lake.svg',
  120.00, 60.00, 1000, 780, '08:00-18:00（以景区公告为准）',
  30.7367, 90.5227, NULL, '那曲市文化和旅游局公开信息', '2026-05-19'),

(32, '莫高窟', '敦煌', '甘肃省敦煌市鸣沙山东麓',
  '俗称千佛洞，世界上现存规模最大、保存最完好的佛教艺术宝库，世界文化遗产。须提前网络预约，日票额有限。',
  '/images/seed/temple.svg',
  238.00, 119.00, 6000, 2130, '08:00-17:00（A类票额，参观时段固定，须提前预约）',
  40.0361, 94.8086, 'https://www.dha.ac.cn/', '敦煌研究院官网', '2026-05-19'),

(33, '乐山大佛', '乐山', '四川省乐山市凌云山',
  '世界最高石刻弥勒佛坐像，依山而凿，通高71米，与乌尤寺、凌云寺共同构成景区，世界文化与自然双遗产。',
  '/images/seed/temple.svg',
  120.00, 60.00, 2000, 1560, '07:30-18:30（旺季）',
  29.5458, 103.7716, 'https://www.leshandafo.com/', '乐山大佛景区官网', '2026-05-19'),

(34, '峨眉山', '乐山', '四川省峨眉山市',
  '佛教四大名山之一，以金顶云海、雷洞坪等景观闻名，与乐山大佛共同列入世界文化与自然双遗产，海拔3079米。',
  '/images/seed/mountain.svg',
  160.00, 80.00, 3000, 1870, '06:00-18:00（缆车另计，以景区公告为准）',
  29.6159, 103.3436, 'https://www.emeishan.com/', '峨眉山景区官网', '2026-05-19'),

(35, '泰山', '泰安', '山东省泰安市泰山区',
  '五岳之首，世界自然与文化双遗产，古代帝王封禅之地，以日出云海和南天门石阶著称。可徒步或乘缆车。',
  '/images/seed/mountain.svg',
  125.00, 62.00, 5000, 3400, '全天开放（缆车运营时间以公告为准）',
  36.2538, 117.1082, 'https://www.taishan.com/', '泰山景区官网', '2026-05-19'),

(36, '天山天池', '乌鲁木齐', '新疆昌吉回族自治州阜康市',
  '天山博格达峰下高山冰碛湖，海拔1910米，以雪山倒映和天山云杉林著称，国家5A级风景区。',
  '/images/seed/mountain.svg',
  100.00, 50.00, 2000, 1380, '08:00-20:00（旺季，以景区公告为准）',
  43.8851, 88.1206, NULL, '昌吉州文化和旅游局公开信息', '2026-05-19'),

(37, '喀纳斯景区', '阿勒泰', '新疆阿勒泰地区布尔津县',
  '集湖泊、河流、草原、森林、雪山于一体，以禾木晨雾和彩林秋色闻名的边境风景区。旺季需提前订住宿。',
  '/images/seed/lake.svg',
  245.00, 122.00, 1500, 890, '08:00-20:00（旺季，以景区公告为准）',
  48.6867, 87.0005, NULL, '阿勒泰地区文化和旅游局公开信息', '2026-05-19'),

(38, '元阳哈尼梯田', '红河', '云南省红河州元阳县',
  '世界文化遗产，哈尼族先民开凿的大规模梯田，以冬春水季日照云海倒影最为壮观，多依树观景台是核心打卡点。',
  '/images/seed/attraction.svg',
  100.00, 50.00, 2000, 1560, '08:00-18:00（以景区公告为准）',
  23.1161, 102.7756, NULL, '红河州文化和旅游局公开信息', '2026-05-19'),

(39, '石林风景区', '昆明', '云南省昆明市石林彝族自治县',
  '世界自然遗产，以喀斯特地貌形成的剑状石灰岩群落著称，被称为"天下第一奇观"。',
  '/images/seed/attraction.svg',
  200.00, 100.00, 2000, 1240, '07:30-18:00（以景区公告为准）',
  24.7726, 103.2701, 'https://www.chinastoneforest.com/', '石林风景名胜区官网', '2026-05-19'),

(40, '龙门石窟', '洛阳', '河南省洛阳市南郊伊水两岸',
  '北魏至唐代皇家开凿的大型石窟群，世界文化遗产，奉先寺卢舍那大佛为代表性造像，规模宏大。',
  '/images/seed/temple.svg',
  90.00, 45.00, 2500, 1560, '08:00-17:30（以景区公告为准）',
  34.5604, 112.4741, 'https://www.longmen.gov.cn/', '龙门石窟景区官网', '2026-05-19'),

(41, '武夷山', '南平', '福建省南平市武夷山市',
  '世界文化与自然双重遗产，以丹霞地貌、碧水九曲和武夷岩茶产区著称，竹筏漂流体验极佳。',
  '/images/seed/mountain.svg',
  140.00, 70.00, 1500, 1020, '07:30-18:00（以景区公告为准）',
  27.7494, 117.9855, 'https://www.wuyi.gov.cn/', '武夷山风景名胜区管委会官网', '2026-05-19'),

(42, '周庄古镇', '苏州', '江苏省苏州市昆山市周庄镇',
  '江南水乡代表古镇，以水道、石桥、明清民居和双桥著称，有"中国第一水乡"之称。早晨人少，建议8点前进入。',
  '/images/seed/garden.svg',
  100.00, 50.00, 2000, 1460, '08:00-17:00（以景区公告为准）',
  31.1132, 120.8476, NULL, '周庄古镇景区管委会公开信息', '2026-05-19'),

(43, '夫子庙秦淮风光带', '南京', '江苏省南京市秦淮区贡院街',
  '以秦淮河沿岸历史文化街区为核心，涵盖夫子庙、贡院、乌衣巷等历史景观，是南京文化旅游核心区，多数区域免费开放。',
  '/images/seed/nanjing.svg',
  0.00, 0.00, 9999, 9999, '全天开放（贡院等收费景点另行公告）',
  32.0211, 118.7834, NULL, '南京市文化和旅游局公开信息', '2026-05-19'),

(44, '西双版纳热带植物园', '西双版纳', '云南省西双版纳州勐腊县勐仑镇',
  '中国科学院直属热带植物科研基地，同时对公众开放，以物种多样性和热带雨林景观著称，是自然科普和度假的优选地。',
  '/images/seed/garden.svg',
  80.00, 40.00, 2000, 1650, '08:00-18:00（以园区公告为准）',
  21.9158, 101.0253, 'https://www.xtbg.cas.cn/', '西双版纳热带植物园官网', '2026-05-19'),

(45, '崂山风景区', '青岛', '山东省青岛市崂山区',
  '道教名山，主峰巨峰海拔1132.7米，以海上仙山、清泉奇石和道教文化著称，是青岛近郊必游山岳景区。',
  '/images/seed/qingdao.svg',
  80.00, 40.00, 2000, 1560, '06:30-17:00（以景区公告为准）',
  36.1654, 120.6028, 'https://www.laoshan.gov.cn/', '崂山区文化和旅游局', '2026-05-19'),

(46, '海螺沟冰川森林公园', '甘孜', '四川省甘孜州泸定县磨西镇',
  '以现代冰川、高山温泉和原始森林为核心景观，贡嘎山麓冰川可步行近距离接触，是四川高原特色景区。',
  '/images/seed/mountain.svg',
  82.00, 41.00, 1200, 820, '08:00-18:00（以景区公告为准）',
  29.5744, 102.0626, NULL, '海螺沟景区管委会公开信息', '2026-05-19'),

(47, '黄龙风景名胜区', '阿坝', '四川省阿坝州松潘县',
  '世界自然遗产，以彩池、雪山、峡谷、森林构成的景观组合闻名，核心景区彩池群层叠绵延，与九寨沟形成黄九环线。',
  '/images/seed/lake.svg',
  150.00, 75.00, 1500, 960, '08:00-17:00（以景区公告为准，海拔高）',
  32.7502, 103.8165, NULL, '阿坝州文化和旅游局公开信息', '2026-05-19'),

(48, '武陵源·黄龙洞', '张家界', '湖南省张家界市武陵源区',
  '武陵源世界自然遗产核心区内的溶洞景区，以大规模石钟乳、石笋和地下河著称，是张家界地下景观的代表。',
  '/images/seed/zhangjiajie.svg',
  130.00, 65.00, 1000, 680, '08:00-17:30（以景区公告为准）',
  29.368, 110.5526, NULL, '武陵源景区管委会公开信息', '2026-05-19')

ON DUPLICATE KEY UPDATE
  `name`              = VALUES(`name`),
  `city`              = VALUES(`city`),
  `address`           = VALUES(`address`),
  `description`       = VALUES(`description`),
  `cover_img`         = VALUES(`cover_img`),
  `adult_price`       = VALUES(`adult_price`),
  `child_price`       = VALUES(`child_price`),
  `open_time`         = VALUES(`open_time`),
  `lat`               = VALUES(`lat`),
  `lng`               = VALUES(`lng`),
  `source_name`       = VALUES(`source_name`),
  `data_checked_date` = VALUES(`data_checked_date`);

-- ============================================================
-- 补充社区游记（帖子 11–15）
-- 来源：data_supplement.sql，2026-05-19
-- ============================================================
INSERT INTO `tm_post` (`id`, `user_id`, `title`, `content`, `images`, `destination`, `tags`, `like_count`, `comment_count`, `collect_count`, `view_count`, `status`, `source_name`, `data_checked_date`) VALUES
(11, 4, '山东泰山+曲阜孔庙4天｜登顶看日出攻略',
  'Day1济南：趵突泉+大明湖+宽厚里；Day2泰山：推荐凌晨12点从红门出发徒步，爬5小时到南天门在天街等日出，日出后吃豆腐脑早餐再下山（用缆车）；Day3曲阜：三孔（孔庙孔林孔府），半天到一天即可；Day4返程。泰山登顶一定要带厚衣物，山顶夏天也只有十几度，日出前后风大刺骨。',
  '/images/seed/mountain.svg,/images/seed/mountain.svg,/images/seed/nanjing.svg',
  '泰安', '山东,泰山,泰山日出,曲阜,孔庙,济南', 11200, 247, 10800, 43000, 1, '演示游记', '2026-05-19'),

(12, 2, '武汉2天｜黄鹤楼+汉口租界+东湖绿道',
  'Day1：黄鹤楼（早9点前进景区人少）—蛇山城墙—武汉大学（需提前预约）—户部巷宵夜；Day2：汉口老租界漫步（大和街、中山大道）—黎黄陂路小吃—东湖绿道骑行（推荐磨山段）—光谷步行街。武汉地铁很方便，主要景点都能覆盖，两天完全够用，热干面和豆皮必尝。',
  '/images/seed/attraction.svg,/images/seed/attraction.svg,/images/seed/lake.svg',
  '武汉', '武汉,黄鹤楼,汉口租界,东湖,两天一夜', 6700, 158, 6400, 26000, 1, '演示游记', '2026-05-19'),

(13, 3, '苏州+周庄2天｜古典园林完整打卡',
  'Day1苏州：拙政园（7:30刚开门进最安静）—狮子林—平江路茶馆—苏博（免费需预约）—观前街；Day2周庄：早8点前进古镇人最少，走富安桥—双桥—沈厅—张厅，中午吃万三蹄，下午2点前出镇避开人流。苏州园林建议只选1-2个深度游，不要贪多导致走马观花。周庄早进早出是关键。',
  '/images/seed/garden.svg,/images/seed/garden.svg,/images/seed/attraction.svg',
  '苏州', '苏州,拙政园,周庄,园林,江南水乡', 7400, 169, 7100, 28000, 1, '演示游记', '2026-05-19'),

(14, 4, '青岛+崂山3天｜海滨城市完整路线',
  'Day1青岛市区：栈桥—小鱼山—八大关（欧式建筑群）—太平山索道—台东步行街晚餐；Day2崂山：北线巨峰，上缆车下步行，下午顺路看太清宫；Day3劈柴院早餐+中山路+博物馆（免费）+啤酒街晚餐扫货。青岛比想象中大，建议地铁为主，五四广场+海边走走必排。海鲜大排档一定要货比三家再进。',
  '/images/seed/qingdao.svg,/images/seed/qingdao.svg,/images/seed/qingdao.svg',
  '青岛', '青岛,崂山,栈桥,八大关,海鲜,啤酒', 5900, 143, 5700, 23000, 1, '演示游记', '2026-05-19'),

(15, 2, '南京2天｜中山陵+夫子庙+先锋书店',
  'Day1：中山陵（徒步博爱坊到祭堂329级台阶，背景开阔壮观）—明孝陵（石象路神兽路很出片）—紫金山步道散步；Day2：玄武湖公园（免费，环湖一圈约1小时）—台城（樱花季必来，非花季也很美）—夫子庙秦淮河—先锋书店（地下空间全国最美书店之一）。南京旅游性价比极高，博物馆基本免费。',
  '/images/seed/nanjing.svg,/images/seed/nanjing.svg,/images/seed/nanjing.svg',
  '南京', '南京,中山陵,夫子庙,玄武湖,台城,周末游', 8100, 187, 7800, 32000, 1, '演示游记', '2026-05-19')

ON DUPLICATE KEY UPDATE
  `title`             = VALUES(`title`),
  `content`           = VALUES(`content`),
  `images`            = VALUES(`images`),
  `destination`       = VALUES(`destination`),
  `tags`              = VALUES(`tags`),
  `like_count`        = VALUES(`like_count`),
  `comment_count`     = VALUES(`comment_count`),
  `collect_count`     = VALUES(`collect_count`),
  `view_count`        = VALUES(`view_count`),
  `status`            = VALUES(`status`),
  `data_checked_date` = VALUES(`data_checked_date`);

-- 补充帖子 11–15 评论
INSERT IGNORE INTO `tm_comment` (`post_id`, `user_id`, `content`, `like_count`) VALUES
(11, 4, '凌晨12点出发，爬到南天门正好看日出，体力消耗超大但完全值', 38),
(11, 3, '泰山那个豆腐脑早餐是真的香，吃完再下山精力满满', 24),
(12, 3, '武汉大学需要提前在公众号预约，不然会被拦在门外', 22),
(13, 2, '拙政园7:30刚开门进去，那半小时几乎没什么人，太安静了', 47),
(14, 4, '崂山北线缆车上去步行下来是最合理方案，反过来会很累', 31),
(15, 3, '先锋书店真的很美，在地下室，建议预留1小时慢慢逛', 29);

-- 补充帖子 11–15 点赞
INSERT IGNORE INTO `tm_like` (`user_id`, `target_id`, `target_type`) VALUES
(3, 11, 0), (4, 11, 0),
(2, 12, 0), (3, 12, 0),
(2, 13, 0), (4, 13, 0),
(3, 14, 0), (4, 14, 0),
(3, 15, 0), (4, 15, 0);

-- ============================================================
-- 本地演示补充：丰富交通与酒店列表（2026-05-25）
-- ============================================================

INSERT INTO `tm_flight` (`id`, `flight_no`, `airline`, `departure_city`, `arrival_city`, `departure_time`, `arrival_time`, `economy_price`, `business_price`, `total_seats`, `available_seats`, `status`) VALUES
(201, 'CA1501', '中国国际航空', '北京', '上海', '2026-05-26 07:25:00', '2026-05-26 09:40:00', 620.00, 1980.00, 210, 96, 1),
(202, 'MU5106', '中国东方航空', '北京', '上海', '2026-05-26 11:10:00', '2026-05-26 13:25:00', 580.00, 1880.00, 190, 83, 1),
(203, 'HO1252', '吉祥航空', '北京', '上海', '2026-05-26 19:30:00', '2026-05-26 21:45:00', 540.00, 1680.00, 180, 71, 1),
(204, 'CZ3101', '中国南方航空', '广州', '北京', '2026-05-27 08:35:00', '2026-05-27 11:45:00', 760.00, 2380.00, 220, 108, 1),
(205, 'HU7802', '海南航空', '广州', '北京', '2026-05-27 15:20:00', '2026-05-27 18:30:00', 720.00, 2280.00, 200, 92, 1),
(206, '3U8733', '四川航空', '成都', '上海', '2026-05-27 09:20:00', '2026-05-27 12:05:00', 690.00, 2180.00, 190, 76, 1),
(207, 'MU5418', '中国东方航空', '成都', '上海', '2026-05-27 17:45:00', '2026-05-27 20:30:00', 650.00, 2080.00, 180, 69, 1),
(208, 'CA4111', '中国国际航空', '成都', '北京', '2026-05-28 08:50:00', '2026-05-28 11:25:00', 720.00, 2280.00, 200, 88, 1),
(209, 'MU2378', '中国东方航空', '西安', '上海', '2026-05-28 10:30:00', '2026-05-28 12:45:00', 560.00, 1780.00, 170, 74, 1),
(210, 'MF8048', '厦门航空', '厦门', '上海', '2026-05-28 13:15:00', '2026-05-28 15:00:00', 430.00, 1380.00, 175, 91, 1),
(211, 'SC4657', '山东航空', '青岛', '北京', '2026-05-29 07:55:00', '2026-05-29 09:20:00', 390.00, 1280.00, 165, 79, 1),
(212, 'CZ6409', '中国南方航空', '重庆', '广州', '2026-05-29 12:20:00', '2026-05-29 14:20:00', 520.00, 1680.00, 190, 99, 1),
(213, 'HU7345', '海南航空', '上海', '三亚', '2026-05-30 08:05:00', '2026-05-30 11:35:00', 880.00, 2780.00, 200, 58, 1),
(214, 'CA8219', '中国国际航空', '杭州', '西安', '2026-05-30 14:25:00', '2026-05-30 16:45:00', 590.00, 1880.00, 180, 82, 1),
(215, 'MU2895', '中国东方航空', '南京', '广州', '2026-05-31 09:45:00', '2026-05-31 12:05:00', 610.00, 1980.00, 180, 86, 1),
(216, 'ZH9876', '深圳航空', '广州', '成都', '2026-05-31 16:15:00', '2026-05-31 18:35:00', 560.00, 1780.00, 170, 73, 1),
(217, 'MF8127', '厦门航空', '北京', '厦门', '2026-06-01 09:25:00', '2026-06-01 12:20:00', 820.00, 2580.00, 190, 68, 1),
(218, '3U8926', '四川航空', '重庆', '杭州', '2026-06-01 18:10:00', '2026-06-01 20:25:00', 570.00, 1820.00, 180, 81, 1)
ON DUPLICATE KEY UPDATE
  `flight_no` = VALUES(`flight_no`),
  `airline` = VALUES(`airline`),
  `departure_city` = VALUES(`departure_city`),
  `arrival_city` = VALUES(`arrival_city`),
  `departure_time` = VALUES(`departure_time`),
  `arrival_time` = VALUES(`arrival_time`),
  `economy_price` = VALUES(`economy_price`),
  `business_price` = VALUES(`business_price`),
  `total_seats` = VALUES(`total_seats`),
  `available_seats` = VALUES(`available_seats`),
  `status` = VALUES(`status`);

INSERT INTO `tm_train` (`id`, `train_no`, `train_type`, `departure_station`, `arrival_station`, `departure_time`, `arrival_time`, `duration_minutes`, `first_class_price`, `second_class_price`, `first_class_seats`, `second_class_seats`, `status`) VALUES
(201, 'G101', '高铁', '北京南', '上海虹桥', '2026-05-26 07:00:00', '2026-05-26 12:36:00', 336, 933.00, 553.00, 44, 320, 1),
(202, 'G113', '高铁', '北京南', '上海虹桥', '2026-05-26 09:05:00', '2026-05-26 14:29:00', 324, 933.00, 553.00, 39, 286, 1),
(203, 'G157', '高铁', '北京南', '上海虹桥', '2026-05-26 17:18:00', '2026-05-26 22:43:00', 325, 933.00, 553.00, 42, 300, 1),
(204, 'G79', '高铁', '北京西', '广州南', '2026-05-27 10:00:00', '2026-05-27 18:02:00', 482, 1380.00, 862.00, 28, 210, 1),
(205, 'G337', '高铁', '北京西', '成都东', '2026-05-27 09:24:00', '2026-05-27 19:02:00', 578, 1240.00, 778.00, 31, 230, 1),
(206, 'G1975', '高铁', '重庆西', '成都东', '2026-05-27 08:18:00', '2026-05-27 09:34:00', 76, 154.00, 96.00, 62, 420, 1),
(207, 'G2191', '高铁', '成都东', '重庆西', '2026-05-27 18:02:00', '2026-05-27 19:20:00', 78, 154.00, 96.00, 58, 390, 1),
(208, 'G7375', '高铁', '上海虹桥', '杭州东', '2026-05-28 08:32:00', '2026-05-28 09:19:00', 47, 117.00, 73.00, 50, 360, 1),
(209, 'G7319', '高铁', '杭州东', '上海虹桥', '2026-05-28 19:10:00', '2026-05-28 20:02:00', 52, 117.00, 73.00, 48, 340, 1),
(210, 'G1936', '高铁', '西安北', '上海虹桥', '2026-05-28 08:03:00', '2026-05-28 14:36:00', 393, 1060.00, 669.00, 34, 240, 1),
(211, 'D2287', '动车', '上海虹桥', '厦门北', '2026-05-29 07:05:00', '2026-05-29 14:08:00', 423, 638.00, 399.00, 26, 210, 1),
(212, 'G1554', '高铁', '广州南', '长沙南', '2026-05-29 09:30:00', '2026-05-29 12:13:00', 163, 503.00, 314.00, 46, 300, 1),
(213, 'G6108', '高铁', '长沙南', '广州南', '2026-05-29 18:24:00', '2026-05-29 21:07:00', 163, 503.00, 314.00, 45, 310, 1),
(214, 'G205', '高铁', '南京南', '青岛北', '2026-05-30 08:16:00', '2026-05-30 12:27:00', 251, 536.00, 336.00, 36, 260, 1),
(215, 'G7587', '高铁', '南京南', '杭州东', '2026-05-30 13:00:00', '2026-05-30 14:38:00', 98, 187.00, 117.00, 42, 280, 1),
(216, 'G1378', '高铁', '昆明南', '长沙南', '2026-05-31 08:10:00', '2026-05-31 14:26:00', 376, 853.00, 535.00, 30, 220, 1),
(217, 'G2365', '高铁', '上海虹桥', '重庆西', '2026-06-01 07:09:00', '2026-06-01 19:21:00', 732, 1470.00, 923.00, 24, 180, 1),
(218, 'D1810', '动车', '广州南', '成都东', '2026-06-01 09:18:00', '2026-06-01 18:53:00', 575, 965.00, 603.00, 27, 210, 1)
ON DUPLICATE KEY UPDATE
  `train_no` = VALUES(`train_no`),
  `train_type` = VALUES(`train_type`),
  `departure_station` = VALUES(`departure_station`),
  `arrival_station` = VALUES(`arrival_station`),
  `departure_time` = VALUES(`departure_time`),
  `arrival_time` = VALUES(`arrival_time`),
  `duration_minutes` = VALUES(`duration_minutes`),
  `first_class_price` = VALUES(`first_class_price`),
  `second_class_price` = VALUES(`second_class_price`),
  `first_class_seats` = VALUES(`first_class_seats`),
  `second_class_seats` = VALUES(`second_class_seats`),
  `status` = VALUES(`status`);

INSERT INTO `tm_hotel` (`id`, `name`, `city`, `address`, `star_rating`, `description`, `cover_img`, `lat`, `lng`, `avg_price`, `score`, `source_name`, `source_url`, `data_checked_date`) VALUES
(41, '北京前门漫心府', '北京', '东城区前门东路与鲜鱼口街区附近', 4, '靠近前门、天安门和地铁站，适合第一次来北京的中轴线行程。', '/images/seed/beijing.svg', 39.899200, 116.397800, 620.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-25'),
(42, '北京望京凯悦精选酒店', '北京', '朝阳区望京商圈', 4, '临近望京商业区和地铁换乘站，适合商务与亲子短住。', '/images/seed/beijing.svg', 39.990600, 116.480700, 720.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-25'),
(43, '上海外滩南京东路亚朵S酒店', '上海', '黄浦区南京东路步行街附近', 4, '步行可达外滩和人民广场，适合城市漫步和夜景行程。', '/images/seed/shanghai.svg', 31.239000, 121.484500, 780.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-25'),
(44, '上海徐家汇衡山路花园酒店', '上海', '徐汇区衡山路街区', 4, '靠近衡山路、徐家汇和武康路，适合法租界 citywalk。', '/images/seed/shanghai.svg', 31.201600, 121.443800, 690.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-25'),
(45, '成都宽窄巷子桔子水晶酒店', '成都', '青羊区宽窄巷子商圈', 4, '适合宽窄巷子、人民公园和市区美食路线，地铁出行方便。', '/images/seed/chengdu.svg', 30.669400, 104.056900, 520.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-25'),
(46, '成都东站希尔顿欢朋酒店', '成都', '成华区成都东站附近', 4, '靠近成都东站，适合高铁中转和川西出发前后住宿。', '/images/seed/chengdu.svg', 30.629300, 104.142600, 460.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-25'),
(47, '西安钟楼回民街亚朵酒店', '西安', '莲湖区钟楼回民街片区', 4, '临近钟楼、鼓楼和回民街，夜游与美食路线集中。', '/images/seed/xian.svg', 34.264600, 108.943700, 480.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-25'),
(48, '广州珠江新城希尔顿欢朋酒店', '广州', '天河区珠江新城核心区', 4, '靠近花城广场、广东省博物馆和广州塔夜景动线。', '/images/seed/attraction.svg', 23.123400, 113.324800, 620.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-25'),
(49, '厦门中山路海景美居酒店', '厦门', '思明区中山路与鹭江道附近', 4, '靠近八市、中山路和鼓浪屿码头，适合厦门三日慢游。', '/images/seed/coast.svg', 24.455600, 118.080500, 580.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-25'),
(50, '青岛五四广场海景亚朵酒店', '青岛', '市南区五四广场商圈', 4, '靠近奥帆中心、五四广场和海边步道，适合看海和城市夜景。', '/images/seed/qingdao.svg', 36.064900, 120.384800, 650.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-25'),
(51, '重庆解放碑洪崖洞漫心酒店', '重庆', '渝中区解放碑步行街附近', 4, '步行可达解放碑、八一好吃街和洪崖洞夜景。', '/images/seed/chongqing.svg', 29.560500, 106.581900, 560.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-25'),
(52, '杭州西湖湖滨君亭酒店', '杭州', '上城区湖滨银泰商圈', 4, '临近西湖湖滨、龙翔桥地铁站和银泰商圈，适合周末短途。', '/images/seed/hangzhou.svg', 30.257900, 120.165900, 620.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-25')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `city` = VALUES(`city`),
  `address` = VALUES(`address`),
  `star_rating` = VALUES(`star_rating`),
  `description` = VALUES(`description`),
  `cover_img` = VALUES(`cover_img`),
  `lat` = VALUES(`lat`),
  `lng` = VALUES(`lng`),
  `avg_price` = VALUES(`avg_price`),
  `score` = VALUES(`score`),
  `source_name` = VALUES(`source_name`),
  `source_url` = VALUES(`source_url`),
  `data_checked_date` = VALUES(`data_checked_date`);

INSERT IGNORE INTO `tm_hotel_room` (`hotel_id`, `room_type`, `bed_type`, `area`, `price`, `total_rooms`, `available_rooms`, `facilities`) VALUES
(41, '前门雅致大床房', '1.8m大床', 30, 598.00, 22, 14, 'WiFi,空调,近地铁,行李寄存'),
(41, '老城家庭房', '1.8m+1.2m床', 38, 798.00, 10, 6, 'WiFi,亲子用品,早餐,近前门'),
(42, '望京商务大床房', '1.8m大床', 32, 688.00, 24, 16, 'WiFi,办公桌,健身房,洗衣房'),
(42, '精选双床房', '2×1.2m双床', 35, 728.00, 18, 10, 'WiFi,早餐,近商圈,停车'),
(43, '南京路景观大床房', '1.8m大床', 31, 758.00, 26, 18, 'WiFi,城市景观,近外滩,洗衣房'),
(43, '外滩亲子双床房', '1.5m+1.2m床', 40, 958.00, 12, 7, 'WiFi,亲子用品,早餐,近地铁'),
(44, '衡山路花园大床房', '1.8m大床', 30, 668.00, 24, 15, 'WiFi,咖啡角,近武康路,安静街区'),
(44, '徐家汇双床房', '2×1.2m双床', 34, 728.00, 16, 9, 'WiFi,办公桌,近地铁,早餐'),
(45, '宽窄巷子大床房', '1.8m大床', 30, 498.00, 28, 17, 'WiFi,空调,近人民公园,洗衣房'),
(45, '成都美食双床房', '2×1.2m双床', 34, 558.00, 18, 12, 'WiFi,早餐,近地铁,行李寄存'),
(46, '东站中转大床房', '1.8m大床', 28, 428.00, 30, 20, 'WiFi,近高铁站,早餐,接驳咨询'),
(46, '东站商务双床房', '2×1.2m双床', 32, 488.00, 18, 11, 'WiFi,办公桌,洗衣房,近地铁'),
(47, '钟楼夜景大床房', '1.8m大床', 30, 458.00, 26, 16, 'WiFi,城市景观,近回民街,早餐'),
(47, '古城家庭房', '1.8m+1.2m床', 40, 638.00, 12, 8, 'WiFi,亲子用品,近地铁,行李寄存'),
(48, '珠江新城商务房', '1.8m大床', 32, 598.00, 28, 18, 'WiFi,办公桌,健身房,近花城广场'),
(48, '广州塔景观双床房', '2×1.2m双床', 38, 768.00, 14, 7, 'WiFi,塔景,早餐,近地铁'),
(49, '中山路舒适大床房', '1.8m大床', 29, 528.00, 24, 15, 'WiFi,近八市,行李寄存,洗衣房'),
(49, '鹭江海景双床房', '2×1.2m双床', 36, 698.00, 12, 6, 'WiFi,海景,近码头,早餐'),
(50, '五四广场海景房', '1.8m大床', 34, 628.00, 24, 13, 'WiFi,海景,近奥帆中心,早餐'),
(50, '市南家庭双床房', '2×1.35m双床', 42, 788.00, 10, 5, 'WiFi,亲子用品,海边步道,行李寄存'),
(51, '解放碑城景大床房', '1.8m大床', 30, 528.00, 26, 17, 'WiFi,近洪崖洞,洗衣房,早餐'),
(51, '山城夜景双床房', '2×1.2m双床', 36, 638.00, 16, 9, 'WiFi,夜景,近地铁,行李寄存'),
(52, '湖滨雅致大床房', '1.8m大床', 30, 588.00, 24, 15, 'WiFi,近西湖,早餐,洗衣房'),
(52, '西湖亲子双床房', '2×1.2m双床', 39, 758.00, 12, 7, 'WiFi,亲子用品,近湖滨,行李寄存');

-- 演示图片修复：将旧占位 SVG 替换为可直接加载的真实图片 URL。
UPDATE `tm_media_asset`
SET `url` = REPLACE(
  `source_url`,
  'https://commons.wikimedia.org/wiki/File:',
  'https://commons.wikimedia.org/wiki/Special:Redirect/file/'
)
WHERE `source_url` LIKE 'https://commons.wikimedia.org/wiki/File:%'
  AND `url` LIKE '/images/seed/%';

UPDATE `tm_attraction` a
JOIN `tm_media_asset` m
  ON m.`target_type` = 'attraction'
 AND m.`target_id` = a.`id`
SET a.`cover_img` = m.`url`
WHERE a.`cover_img` LIKE '/images/seed/%'
  AND (m.`url` LIKE 'https://commons.wikimedia.org/%' OR m.`url` LIKE 'https://upload.wikimedia.org/%');

UPDATE `tm_post`
SET `images` = CASE `id`
  WHEN 1 THEN 'https://commons.wikimedia.org/wiki/Special:Redirect/file/The_Forbidden_City_-_View_from_Coal_Hill.jpg,https://commons.wikimedia.org/wiki/Special:Redirect/file/Badaling_China_Great-Wall-of-China-01.jpg,https://commons.wikimedia.org/wiki/Special:Redirect/file/20090530_Beijing_Summer_Palace_8467.jpg'
  WHEN 2 THEN 'https://commons.wikimedia.org/wiki/Special:Redirect/file/Shanghai_Bund-20150516-RM-173803.jpg,https://commons.wikimedia.org/wiki/Special:Redirect/file/Blue_hour_view_of_the_Bund_from_the_Shanghai_World_Financial_Center_dllu.jpg'
  WHEN 3 THEN 'https://upload.wikimedia.org/wikipedia/commons/1/19/Wuzhizhou_Island_-_01.jpg,https://upload.wikimedia.org/wikipedia/commons/4/44/Yalong_Bay_01.jpg,https://upload.wikimedia.org/wikipedia/commons/6/6e/Beach_of_Tianya-Haijiao_near_Tianya_Rock_%2820230325134441%29.jpg'
  WHEN 4 THEN 'https://upload.wikimedia.org/wikipedia/commons/a/a7/Dujiang_Weir.jpg,https://commons.wikimedia.org/wiki/Special:Redirect/file/Chengdu_Hotpot.jpg,https://commons.wikimedia.org/wiki/Special:Redirect/file/Chengdu_Kuanzhai_Alley_Touristic_Spot_Relics_%E6%88%90%E9%83%BD%E5%AE%BD%E7%AA%84%E5%B7%B7%E5%AD%90%E6%96%87%E7%89%A9%E5%8F%91%E6%8E%98%E9%81%97%E4%BA%A7.jpg'
  WHEN 5 THEN 'https://upload.wikimedia.org/wikipedia/commons/7/74/1_lijiang_old_town_night.jpg,https://commons.wikimedia.org/wiki/Special:Redirect/file/Lijiang_Yunnan_Doors-_in-old-town-01.jpg'
  WHEN 6 THEN 'https://upload.wikimedia.org/wikipedia/commons/f/f1/Hongyadong_night_lights_Chongqing.jpg'
  WHEN 7 THEN 'https://upload.wikimedia.org/wikipedia/commons/f/f1/Hongyadong_night_lights_Chongqing.jpg'
  WHEN 8 THEN 'https://upload.wikimedia.org/wikipedia/commons/b/b6/Gulangyu_Island_from_Zhongshan_Road%2C_Xiamen.jpg'
  WHEN 9 THEN 'https://commons.wikimedia.org/wiki/Special:Redirect/file/West_Lake,_Hangzhou_(Nine-turn_bridge).jpg,https://upload.wikimedia.org/wikipedia/commons/f/f5/Humble_Administrator%27s_Garden_Suzhou_November_2017_005.jpg,https://commons.wikimedia.org/wiki/Special:Redirect/file/Shanghai_Bund-20150516-RM-173803.jpg'
  WHEN 10 THEN 'https://commons.wikimedia.org/wiki/Special:Redirect/file/The_Forbidden_City_-_View_from_Coal_Hill.jpg,https://commons.wikimedia.org/wiki/Special:Redirect/file/Qingdao_Zhanqiao.jpg,https://upload.wikimedia.org/wikipedia/commons/f/f1/Hongyadong_night_lights_Chongqing.jpg'
  WHEN 11 THEN 'https://commons.wikimedia.org/wiki/Special:Redirect/file/Mount_Tai.jpg'
  WHEN 12 THEN 'https://commons.wikimedia.org/wiki/Special:Redirect/file/Wuhan_Yangtze_River_Bridge.jpg'
  WHEN 13 THEN 'https://upload.wikimedia.org/wikipedia/commons/f/f5/Humble_Administrator%27s_Garden_Suzhou_November_2017_005.jpg'
  WHEN 14 THEN 'https://commons.wikimedia.org/wiki/Special:Redirect/file/Qingdao_Zhanqiao.jpg'
  WHEN 15 THEN 'https://upload.wikimedia.org/wikipedia/commons/0/06/China-Nanjing_%282024%29_Mausoleum_of_Sun_Yat_Sen_%E4%B8%AD%E5%B1%B1%E9%99%B5_-_img_08.jpg'
  ELSE `images`
END
WHERE `id` BETWEEN 1 AND 15
  AND `images` LIKE '%/images/seed/%';

