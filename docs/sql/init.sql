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
  PRIMARY KEY (`id`)
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='火车票信息表';

-- 5. 交通票务订单表 (Traffic Order) 成员A负责
CREATE TABLE IF NOT EXISTS `tm_traffic_order` (
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
  `is_read` TINYINT(1) DEFAULT '0' COMMENT '0-未读, 1-已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

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
-- 酒店封面采用所在城市/周边景观的开放版权图片，仅用于演示展示，不代表酒店官方宣传图；图片授权见 tm_media_asset。
INSERT IGNORE INTO `tm_hotel` (`id`, `name`, `city`, `address`, `star_rating`, `description`, `cover_img`, `lat`, `lng`, `avg_price`, `score`, `source_name`, `source_url`, `data_checked_date`) VALUES
(1, '北京国贸大酒店', '北京', '朝阳区建国门外大街1号', 5, '位于北京CBD核心地带，毗邻国贸商圈，提供顶级商务服务与城市景观。', 'https://upload.wikimedia.org/wikipedia/commons/e/ef/The_Forbidden_City_-_View_from_Coal_Hill.jpg', 39.9087, 116.4575, 1580.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(2, '上海外滩华尔道夫酒店', '上海', '黄浦区中山东一路2号', 5, '位于外滩历史建筑群区域，临近黄浦江与外滩公共开放空间。', 'https://upload.wikimedia.org/wikipedia/commons/2/2b/Shanghai_Bund-20150516-RM-173803.jpg', 31.2400, 121.4900, 2380.00, 4.9, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(3, '广州白天鹅宾馆', '广州', '荔湾区沙面岛南街1号', 5, '坐落于沙面岛珠江畔，是广州具有代表性的老牌高星酒店。', 'https://upload.wikimedia.org/wikipedia/commons/5/50/Guangzhou_skyline_%283to4%29.jpg', 23.1198, 113.2432, 1280.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(4, '成都锦江宾馆', '成都', '锦江区人民南路二段80号', 4, '位于成都市中心区域，临近天府广场及核心商圈。', 'https://upload.wikimedia.org/wikipedia/commons/2/20/Chengdu_skyline_June_2017.jpg', 30.6500, 104.0633, 880.00, 4.6, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(5, '西安大唐芙蓉园精品酒店', '西安', '雁塔区芙蓉南路100号', 4, '临近大唐芙蓉园景区，适合古都文化主题出行。', 'https://upload.wikimedia.org/wikipedia/commons/8/8e/Xi-an_city_wall_side.jpg', 34.2076, 109.0082, 680.00, 4.5, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(6, '三亚亚龙湾万豪度假酒店', '三亚', '亚龙湾国家旅游度假区', 5, '位于亚龙湾度假区，面向滨海休闲度假场景。', 'https://upload.wikimedia.org/wikipedia/commons/4/44/Yalong_Bay_01.jpg', 18.2208, 109.6600, 2880.00, 4.9, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(7, '丽江古城铂尔曼大酒店', '丽江', '古城区象山路99号', 5, '临近丽江古城，面向古城休闲与雪山观光客群。', 'https://upload.wikimedia.org/wikipedia/commons/7/74/1_lijiang_old_town_night.jpg', 26.8720, 100.2275, 1680.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(8, '杭州西湖喜来登大酒店', '杭州', '西湖区北山路9号', 5, '位于西湖周边，适合湖滨休闲及城市观光。', 'https://upload.wikimedia.org/wikipedia/commons/f/fd/Hangzhou_Skyline_against_the_West_Lake.png', 30.2563, 120.1495, 1980.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(9, '重庆解放碑威斯汀酒店', '重庆', '渝中区中山三路36号', 5, '位于重庆中心城区，适合解放碑及两江夜景行程。', 'https://upload.wikimedia.org/wikipedia/commons/f/f5/Chongqing_Skyline_At_Night.png', 29.5600, 106.5714, 1380.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(10, '北京王府井万豪酒店', '北京', '东城区王府井大街57号', 5, '位于王府井商圈，临近故宫、天安门等核心景点。', 'https://upload.wikimedia.org/wikipedia/commons/2/28/Beijing_Wangfujing_Yintai_in88_%2820220908154425%29.jpg', 39.9200, 116.4114, 1880.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(11, '上海静安香格里拉大酒店', '上海', '静安区静安寺路1218号', 5, '位于南京西路商圈，临近静安寺及商业办公区。', 'https://upload.wikimedia.org/wikipedia/commons/4/48/2024-Apr_Jing-an_Temple_%E9%9D%99%E5%AE%89%E5%AF%BA_Shanghai_02.jpg', 31.2244, 121.4471, 1680.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(12, '厦门悦华酒店', '厦门', '湖里区乌石浦路2号', 4, '位于厦门岛内，适合商务与休闲出行。', 'https://upload.wikimedia.org/wikipedia/commons/b/b6/Gulangyu_Island_from_Zhongshan_Road%2C_Xiamen.jpg', 24.5020, 118.0936, 780.00, 4.5, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(13, '桂林香格里拉大酒店', '桂林', '象山区解放东路111号', 5, '临近漓江城市段，适合桂林山水观光行程。', 'https://upload.wikimedia.org/wikipedia/commons/9/92/1_li_jiang_guilin_yangshuo_2011.jpg', 25.2877, 110.2978, 1180.00, 4.8, '公开酒店信息/演示价格', NULL, '2026-05-19'),
(14, '青岛海景花园大酒店', '青岛', '市南区香港中路76号', 5, '位于青岛滨海城区，适合海滨度假及城市观光。', 'https://upload.wikimedia.org/wikipedia/commons/8/89/Zhanqiao_pier_with_Little_Qingdao_Isle.jpg', 36.0584, 120.3817, 1280.00, 4.7, '公开酒店信息/演示价格', NULL, '2026-05-19');

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

-- 景点数据
-- 票价与开放时间来自官方/政府页面核验；余票为演示库存，不代表实时可售库存。
INSERT INTO `tm_attraction` (`id`, `name`, `city`, `address`, `description`, `cover_img`, `adult_price`, `child_price`, `total_tickets`, `available_tickets`, `open_time`, `lat`, `lng`, `official_url`, `source_name`, `data_checked_date`) VALUES
(1, '故宫博物院', '北京', '东城区景山前街4号', '明清两代皇家宫殿，世界文化遗产，馆藏体系覆盖古代宫廷建筑、书画、器物等。', 'https://upload.wikimedia.org/wikipedia/commons/e/ef/The_Forbidden_City_-_View_from_Coal_Hill.jpg', 60.00, 0.00, 1000, 456, '08:30-17:00（旺季，周一闭馆，法定节假日除外）', 39.916345, 116.397155, 'https://www.dpm.org.cn/Visit.html', '故宫博物院官网', '2026-05-19'),
(2, '颐和园', '北京', '海淀区新建宫门路19号', '以昆明湖、万寿山为主体的大型皇家园林，1998年列入世界遗产名录。', 'https://upload.wikimedia.org/wikipedia/commons/f/fb/20090530_Beijing_Summer_Palace_8467.jpg', 30.00, 0.00, 2000, 1234, '06:00-20:00（旺季，停止入园19:00）', 39.999946, 116.275481, 'https://www.summerpalace.net.cn/visit.html', '颐和园官网', '2026-05-19'),
(3, '外滩', '上海', '黄浦区中山东一路', '上海近代城市景观代表区域，沿黄浦江分布多座历史建筑，是城市公共开放空间。', 'https://upload.wikimedia.org/wikipedia/commons/2/2b/Shanghai_Bund-20150516-RM-173803.jpg', 0.00, 0.00, 9999, 9999, '全天开放', 31.239706, 121.490317, 'https://www.shhuangpu.gov.cn/', '上海市黄浦区人民政府', '2026-05-19'),
(4, '西湖', '杭州', '西湖区龙井路1号', '杭州西湖文化景观为世界文化遗产，核心湖区与沿湖开放空间面向公众开放。', 'https://upload.wikimedia.org/wikipedia/commons/d/d8/West_Lake%2C_Hangzhou_%28Nine-turn_bridge%29.jpg', 0.00, 0.00, 9999, 9999, '全天开放（部分收费景点另行开放）', 30.242703, 120.150269, 'https://westlake.hangzhou.gov.cn/', '杭州西湖风景名胜区管委会', '2026-05-19'),
(5, '张家界国家森林公园', '张家界', '武陵源区国家森林公园内', '中国第一个国家森林公园，武陵源世界自然遗产核心景区之一，以石英砂岩峰林地貌著称。', 'https://upload.wikimedia.org/wikipedia/commons/4/47/Zhangjiajie_National_Forest_Park.jpg', 227.00, 113.00, 500, 234, '07:30-18:00（以景区当日公告为准）', 29.327001, 110.475704, 'https://wly.hunan.gov.cn/', '湖南省文化和旅游厅/武陵源景区公开信息', '2026-05-19'),
(6, '秦始皇帝陵博物院', '西安', '临潼区秦陵北路', '以秦始皇兵马俑坑和秦始皇陵相关遗址为核心的遗址类博物馆。', 'https://upload.wikimedia.org/wikipedia/commons/5/52/Terracotta_Army_%2854082561381%29.jpg', 120.00, 0.00, 800, 356, '08:30-18:30（旺季，停止检票17:00）', 34.384018, 109.278491, 'https://www.bmy.com.cn/', '秦始皇帝陵博物院官网', '2026-05-19'),
(7, '九寨沟风景名胜区', '阿坝', '阿坝藏族羌族自治州九寨沟县漳扎镇', '以高山湖泊、瀑布群、彩林和雪峰景观闻名的世界自然遗产。', 'https://upload.wikimedia.org/wikipedia/commons/2/28/1_jiuzhaigou_valley_wu_hua_hai_2011b.jpg', 190.00, 95.00, 1000, 445, '07:30-17:00（旺季，具体以景区公告为准）', 33.260772, 103.918599, 'https://www.jiuzhai.com/intelligent-service/tickets', '九寨沟风景名胜区官网', '2026-05-19'),
(8, '黄山风景区', '黄山', '黄山市黄山区汤口镇', '世界文化与自然双重遗产，以奇松、怪石、云海、温泉等景观著称。', 'https://upload.wikimedia.org/wikipedia/commons/2/28/Anhui_Huangshan.jpg', 190.00, 95.00, 1500, 678, '06:00-17:30（旺季，具体以景区公告为准）', 30.130130, 118.168498, 'https://hsgwh.huangshan.gov.cn/', '黄山风景区管委会', '2026-05-19'),
(9, '漓江风景名胜区', '桂林', '桂林市灵川县至阳朔县漓江沿线', '桂林山水代表性景区，游船线路以漓江喀斯特峰林、江湾和田园景观为核心。', 'https://upload.wikimedia.org/wikipedia/commons/9/92/1_li_jiang_guilin_yangshuo_2011.jpg', 210.00, 105.00, 600, 289, '08:00-12:00（游船班次以当日公告为准）', 25.166667, 110.416667, 'https://wglj.guilin.gov.cn/', '桂林市文化广电和旅游局', '2026-05-19'),
(10, '天坛公园', '北京', '东城区天坛东里甲1号', '明清两代皇帝祭天祈谷场所，是北京中轴线南段的重要世界文化遗产。', 'https://upload.wikimedia.org/wikipedia/commons/8/86/20200110_Temple_of_Heaven-1.jpg', 34.00, 17.00, 2000, 1280, '06:00-22:00（景点院落开放时间另行公告）', 39.882200, 116.406600, 'http://www.tiantanpark.com/', '天坛公园官网', '2026-05-19'),
(11, '八达岭长城', '北京', '延庆区G6京藏高速58号出口', '明长城重要关隘，保存状况较好，是北京长城游览代表景区。', 'https://upload.wikimedia.org/wikipedia/commons/5/50/Badaling_China_Great-Wall-of-China-01.jpg', 40.00, 20.00, 2500, 1600, '06:30-16:30（以景区公告为准）', 40.358100, 116.020300, 'https://www.badaling.cn/', '八达岭长城景区官网', '2026-05-19'),
(12, '东方明珠广播电视塔', '上海', '浦东新区世纪大道1号', '上海陆家嘴地标建筑，集城市观光、展览和餐饮功能于一体。', 'https://upload.wikimedia.org/wikipedia/commons/1/1f/Oriental_Pearl_Tower_in_Shanghai.jpg', 199.00, 99.00, 1800, 920, '09:00-21:00（以景区公告为准）', 31.239700, 121.499800, 'https://www.orientalpearltower.com/', '东方明珠官网', '2026-05-19'),
(13, '豫园', '上海', '黄浦区福佑路168号', '江南古典园林代表之一，周边连接豫园商城和城隍庙历史文化街区。', 'https://upload.wikimedia.org/wikipedia/commons/6/6e/Yuyuan_Garden_3.jpg', 40.00, 20.00, 1200, 760, '09:00-16:30（以景区公告为准）', 31.227200, 121.492100, 'https://www.yuyuantm.com.cn/', '豫园商城公开信息', '2026-05-19'),
(14, '上海迪士尼乐园', '上海', '浦东新区川沙新镇黄赵路310号', '大型主题乐园，包含主题园区、演艺、巡游和烟花等游乐体验。', 'https://upload.wikimedia.org/wikipedia/commons/5/58/Firework_in_Shanghai_Disneyland_Park.jpg', 475.00, 356.00, 3000, 1780, '08:30-21:30（以乐园日历为准）', 31.143400, 121.657900, 'https://www.shanghaidisneyresort.com/', '上海迪士尼度假区官网', '2026-05-19'),
(15, '成都大熊猫繁育研究基地', '成都', '成华区熊猫大道1375号', '以大熊猫保护、繁育、科研和公众教育为核心的生态型景区。', 'https://upload.wikimedia.org/wikipedia/commons/5/54/Chengdu-pandas-d10.jpg', 55.00, 27.00, 2000, 1188, '07:30-18:00（以景区公告为准）', 30.735500, 104.145600, 'https://www.panda.org.cn/', '成都大熊猫繁育研究基地官网', '2026-05-19'),
(16, '都江堰景区', '成都', '都江堰市公园路', '世界文化遗产，古代水利工程代表，与青城山共同构成重要旅游目的地。', 'https://upload.wikimedia.org/wikipedia/commons/a/a7/Dujiang_Weir.jpg', 80.00, 40.00, 1800, 960, '08:00-18:00（以景区公告为准）', 31.001700, 103.605500, 'https://www.djy517.com/', '都江堰青城山景区官网', '2026-05-19'),
(17, '蜈支洲岛', '三亚', '海棠区林旺镇后海村', '三亚近海海岛景区，以海水能见度、潜水和滨海休闲项目闻名。', 'https://upload.wikimedia.org/wikipedia/commons/1/19/Wuzhizhou_Island_-_01.jpg', 136.00, 68.00, 1500, 820, '08:00-18:30（航班以天气和公告为准）', 18.312600, 109.757400, 'https://www.wuzhizhou.com/', '蜈支洲岛旅游区官网', '2026-05-19'),
(18, '天涯海角游览区', '三亚', '天涯区天涯镇马岭山麓', '海南代表性滨海文化景区，以海滨礁石景观和“天涯”“海角”题刻闻名。', 'https://upload.wikimedia.org/wikipedia/commons/6/6e/Beach_of_Tianya-Haijiao_near_Tianya_Rock_%2820230325134441%29.jpg', 68.00, 34.00, 1600, 900, '08:00-18:00（以景区公告为准）', 18.296000, 109.344000, 'https://www.aitianya.cn/', '天涯海角游览区官网', '2026-05-19'),
(19, '丽江古城', '丽江', '古城区大研古城', '世界文化遗产，以纳西族传统街巷、水系和民居建筑景观著称。', 'https://upload.wikimedia.org/wikipedia/commons/7/74/1_lijiang_old_town_night.jpg', 0.00, 0.00, 9999, 9999, '全天开放（部分院落另行开放）', 26.872200, 100.233000, 'https://www.ljgucheng.com/', '丽江古城保护管理局公开信息', '2026-05-19'),
(20, '亚龙湾国家旅游度假区', '三亚', '吉阳区亚龙湾国家旅游度假区', '三亚重要滨海度假区，以海湾、沙滩和度假酒店群为主要吸引物。', 'https://upload.wikimedia.org/wikipedia/commons/4/44/Yalong_Bay_01.jpg', 0.00, 0.00, 9999, 9999, '全天开放（海滩及项目以现场公告为准）', 18.229500, 109.637000, 'https://lwj.sanya.gov.cn/', '三亚市旅游和文化广电体育局', '2026-05-19'),
(21, '鼓浪屿', '厦门', '思明区鼓浪屿', '世界文化遗产，融合海岛街巷、近代建筑和音乐文化景观。', 'https://upload.wikimedia.org/wikipedia/commons/b/b6/Gulangyu_Island_from_Zhongshan_Road%2C_Xiamen.jpg', 0.00, 0.00, 9999, 9999, '全天开放（上岛船班及收费景点另行公告）', 24.447700, 118.061900, 'https://gly.xm.gov.cn/', '鼓浪屿管委会公开信息', '2026-05-19'),
(22, '中山陵', '南京', '玄武区石象路7号', '孙中山先生陵寝所在地，位于钟山风景名胜区内，是南京重要历史文化地标。', 'https://upload.wikimedia.org/wikipedia/commons/0/06/China-Nanjing_%282024%29_Mausoleum_of_Sun_Yat_Sen_%E4%B8%AD%E5%B1%B1%E9%99%B5_-_img_08.jpg', 0.00, 0.00, 3000, 2100, '08:30-17:00（周一部分区域闭馆，节假日除外）', 32.064300, 118.848600, 'https://zschina.nanjing.gov.cn/', '南京钟山风景区官网', '2026-05-19'),
(23, '拙政园', '苏州', '姑苏区东北街178号', '苏州古典园林代表，世界文化遗产，以水景、亭台和园林空间布局著称。', 'https://upload.wikimedia.org/wikipedia/commons/f/f5/Humble_Administrator%27s_Garden_Suzhou_November_2017_005.jpg', 80.00, 40.00, 1400, 780, '07:30-17:30（以景区公告为准）', 31.324300, 120.629700, 'https://www.szzzy.cn/', '拙政园官网', '2026-05-19'),
(24, '洪崖洞民俗风貌区', '重庆', '渝中区嘉陵江滨江路88号', '重庆山城夜景代表地，以吊脚楼风貌、滨江夜景和城市商业空间闻名。', 'https://upload.wikimedia.org/wikipedia/commons/f/f1/Hongyadong_night_lights_Chongqing.jpg', 0.00, 0.00, 9999, 9999, '全天开放（商户营业时间不一）', 29.562800, 106.584000, 'https://whlyw.cq.gov.cn/', '重庆市文化和旅游发展委员会', '2026-05-19'),
(25, '龙脊梯田', '桂林', '龙胜各族自治县和平乡', '桂林北部山地梯田景观，包含平安寨、金坑大寨等观景区域。', 'https://upload.wikimedia.org/wikipedia/commons/5/5e/Longji_Rice_Terraces_004.jpg', 80.00, 40.00, 1000, 520, '08:00-17:30（以景区公告为准）', 25.764000, 110.123000, 'https://wglj.guilin.gov.cn/', '桂林市文化广电和旅游局', '2026-05-19'),
(26, '象鼻山景区', '桂林', '象山区滨江路1号', '桂林城市山水代表景观，山体形似象鼻伸入漓江，是桂林标志性景点。', 'https://upload.wikimedia.org/wikipedia/commons/9/98/Elephant_Trunk_Hill%2C_Guilin.jpg', 0.00, 0.00, 1800, 1260, '07:00-18:00（以景区公告为准）', 25.273900, 110.296700, 'https://wglj.guilin.gov.cn/', '桂林市文化广电和旅游局', '2026-05-19'),
(27, '天门山国家森林公园', '张家界', '永定区大庸路', '张家界城市近郊山岳型景区，以天门洞、玻璃栈道和索道体验闻名。', 'https://upload.wikimedia.org/wikipedia/commons/b/b5/Zhangjiajie_from_Tianmen_Mountain_01.jpg', 278.00, 139.00, 1200, 660, '08:00-18:00（索道及线路以景区公告为准）', 29.044600, 110.482400, 'https://wly.hunan.gov.cn/', '湖南省文化和旅游厅/天门山公开信息', '2026-05-19'),
(28, '宏村', '黄山', '黟县宏村镇', '皖南古村落代表，世界文化遗产，以月沼、水圳和徽派民居格局著称。', 'https://upload.wikimedia.org/wikipedia/commons/5/51/Yuezhao_Lake%2C_Hongcun%2C_Anhui%2C_China.jpg', 104.00, 52.00, 1200, 720, '07:30-17:30（以景区公告为准）', 30.004900, 117.987500, 'https://ct.ah.gov.cn/', '安徽省文化和旅游厅公开信息', '2026-05-19')
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
(1, 'attraction', 1, 'image', 'https://upload.wikimedia.org/wikipedia/commons/e/ef/The_Forbidden_City_-_View_from_Coal_Hill.jpg', '故宫俯瞰图', 'Pixelflake', 'CC BY-SA 3.0', 'https://creativecommons.org/licenses/by-sa/3.0/', 'https://commons.wikimedia.org/wiki/File:The_Forbidden_City_-_View_from_Coal_Hill.jpg', 'Wikimedia Commons', '2026-05-19'),
(2, 'attraction', 2, 'image', 'https://upload.wikimedia.org/wikipedia/commons/f/fb/20090530_Beijing_Summer_Palace_8467.jpg', '颐和园景观', 'Jakub Halun', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:20090530_Beijing_Summer_Palace_8467.jpg', 'Wikimedia Commons', '2026-05-19'),
(3, 'attraction', 3, 'image', 'https://upload.wikimedia.org/wikipedia/commons/2/2b/Shanghai_Bund-20150516-RM-173803.jpg', '上海外滩', 'Ermell', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:Shanghai_Bund-20150516-RM-173803.jpg', 'Wikimedia Commons', '2026-05-19'),
(4, 'attraction', 4, 'image', 'https://upload.wikimedia.org/wikipedia/commons/d/d8/West_Lake%2C_Hangzhou_%28Nine-turn_bridge%29.jpg', '西湖九曲桥', 'Sekino Tadashi', 'Public domain', 'https://creativecommons.org/publicdomain/mark/1.0/', 'https://commons.wikimedia.org/wiki/File:West_Lake,_Hangzhou_(Nine-turn_bridge).jpg', 'Wikimedia Commons', '2026-05-19'),
(5, 'attraction', 5, 'image', 'https://upload.wikimedia.org/wikipedia/commons/4/47/Zhangjiajie_National_Forest_Park.jpg', '张家界国家森林公园', 'Kuruman', 'CC BY 2.0', 'https://creativecommons.org/licenses/by/2.0/', 'https://commons.wikimedia.org/wiki/File:Zhangjiajie_National_Forest_Park.jpg', 'Wikimedia Commons', '2026-05-19'),
(6, 'attraction', 6, 'image', 'https://upload.wikimedia.org/wikipedia/commons/5/52/Terracotta_Army_%2854082561381%29.jpg', '秦兵马俑', 'xiquinhosilva', 'CC BY 2.0', 'https://creativecommons.org/licenses/by/2.0/', 'https://commons.wikimedia.org/wiki/File:Terracotta_Army_(54082561381).jpg', 'Wikimedia Commons', '2026-05-19'),
(7, 'attraction', 7, 'image', 'https://upload.wikimedia.org/wikipedia/commons/2/28/1_jiuzhaigou_valley_wu_hua_hai_2011b.jpg', '九寨沟五花海', 'Chensiyuan', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:1_jiuzhaigou_valley_wu_hua_hai_2011b.jpg', 'Wikimedia Commons', '2026-05-19'),
(8, 'attraction', 8, 'image', 'https://upload.wikimedia.org/wikipedia/commons/2/28/Anhui_Huangshan.jpg', '黄山景观', 'Miaulian', 'CC BY-SA 3.0', 'https://creativecommons.org/licenses/by-sa/3.0/', 'https://commons.wikimedia.org/wiki/File:Anhui_Huangshan.jpg', 'Wikimedia Commons', '2026-05-19'),
(9, 'attraction', 9, 'image', 'https://upload.wikimedia.org/wikipedia/commons/9/92/1_li_jiang_guilin_yangshuo_2011.jpg', '漓江山水', 'Chensiyuan', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:1_li_jiang_guilin_yangshuo_2011.jpg', 'Wikimedia Commons', '2026-05-19'),
(10, 'post', 3, 'image', 'https://upload.wikimedia.org/wikipedia/commons/4/44/Yalong_Bay_01.jpg', '三亚亚龙湾', 'Anna Frodesiak', 'Public domain', 'https://creativecommons.org/publicdomain/mark/1.0/', 'https://commons.wikimedia.org/wiki/File:Yalong_Bay_01.jpg', 'Wikimedia Commons', '2026-05-19'),
(11, 'post', 5, 'image', 'https://upload.wikimedia.org/wikipedia/commons/7/74/1_lijiang_old_town_night.jpg', '丽江古城夜景', 'Chensiyuan', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:1_lijiang_old_town_night.jpg', 'Wikimedia Commons', '2026-05-19'),
(12, 'hotel', 3, 'image', 'https://upload.wikimedia.org/wikipedia/commons/5/50/Guangzhou_skyline_%283to4%29.jpg', '广州城市景观', 'jo.sau', 'CC BY 2.0', 'https://creativecommons.org/licenses/by/2.0/', 'https://commons.wikimedia.org/wiki/File:Guangzhou_skyline_(3to4).jpg', 'Wikimedia Commons', '2026-05-19'),
(13, 'hotel', 4, 'image', 'https://upload.wikimedia.org/wikipedia/commons/2/20/Chengdu_skyline_June_2017.jpg', '成都城市景观', 'George N', 'CC BY 2.0', 'https://creativecommons.org/licenses/by/2.0/', 'https://commons.wikimedia.org/wiki/File:Chengdu_skyline_June_2017.jpg', 'Wikimedia Commons', '2026-05-19'),
(14, 'hotel', 9, 'image', 'https://upload.wikimedia.org/wikipedia/commons/f/f5/Chongqing_Skyline_At_Night.png', '重庆夜景', 'Maple Doctor', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:Chongqing_Skyline_At_Night.png', 'Wikimedia Commons', '2026-05-19'),
(15, 'hotel', 14, 'image', 'https://upload.wikimedia.org/wikipedia/commons/8/89/Zhanqiao_pier_with_Little_Qingdao_Isle.jpg', '青岛栈桥', 'K.Y.K.Z.K.', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:Zhanqiao_pier_with_Little_Qingdao_Isle.jpg', 'Wikimedia Commons', '2026-05-19')
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
(16, 'post', 1, 'image', 'https://upload.wikimedia.org/wikipedia/commons/5/50/Badaling_China_Great-Wall-of-China-01.jpg', '八达岭长城', 'CEphoto, Uwe Aranas', 'CC BY-SA 3.0', 'https://creativecommons.org/licenses/by-sa/3.0/', 'https://commons.wikimedia.org/wiki/File:Badaling_China_Great-Wall-of-China-01.jpg', 'Wikimedia Commons', '2026-05-19'),
(17, 'post', 2, 'image', 'https://upload.wikimedia.org/wikipedia/commons/8/86/Blue_hour_view_of_the_Bund_from_the_Shanghai_World_Financial_Center_dllu.jpg', '上海外滩蓝调时刻', 'Dllu', 'CC BY-SA 4.0', 'https://creativecommons.org/licenses/by-sa/4.0/', 'https://commons.wikimedia.org/wiki/File:Blue_hour_view_of_the_Bund_from_the_Shanghai_World_Financial_Center_dllu.jpg', 'Wikimedia Commons', '2026-05-19'),
(18, 'post', 3, 'image', 'https://upload.wikimedia.org/wikipedia/commons/0/04/Yalong_Bay_from_hotel.JPG', '亚龙湾海岸', 'Phillip Hong', 'CC BY-SA 1.0', 'https://creativecommons.org/licenses/by-sa/1.0/', 'https://commons.wikimedia.org/wiki/File:Yalong_Bay_from_hotel.JPG', 'Wikimedia Commons', '2026-05-19'),
(19, 'post', 4, 'image', 'https://upload.wikimedia.org/wikipedia/commons/1/13/Chengdu_Hotpot.jpg', '成都火锅', 'Prince Roy', 'CC BY 2.0', 'https://creativecommons.org/licenses/by/2.0/', 'https://commons.wikimedia.org/wiki/File:Chengdu_Hotpot.jpg', 'Wikimedia Commons', '2026-05-19'),
(20, 'post', 4, 'image', 'https://upload.wikimedia.org/wikipedia/commons/2/26/Chengdu_Kuanzhai_Alley_Touristic_Spot_Relics_%E6%88%90%E9%83%BD%E5%AE%BD%E7%AA%84%E5%B7%B7%E5%AD%90%E6%96%87%E7%89%A9%E5%8F%91%E6%8E%98%E9%81%97%E4%BA%A7.jpg', '成都宽窄巷子', 'Breaknet2025', 'CC BY 4.0', 'https://creativecommons.org/licenses/by/4.0/', 'https://commons.wikimedia.org/wiki/File:Chengdu_Kuanzhai_Alley_Touristic_Spot_Relics_%E6%88%90%E9%83%BD%E5%AE%BD%E7%AA%84%E5%B7%B7%E5%AD%90%E6%96%87%E7%89%A9%E5%8F%91%E6%8E%98%E9%81%97%E4%BA%A7.jpg', 'Wikimedia Commons', '2026-05-19'),
(21, 'post', 5, 'image', 'https://upload.wikimedia.org/wikipedia/commons/b/b9/Lijiang_Yunnan_Doors-_in-old-town-01.jpg', '丽江古城街巷', 'CEphoto, Uwe Aranas', 'CC BY-SA 3.0', 'https://creativecommons.org/licenses/by-sa/3.0/', 'https://commons.wikimedia.org/wiki/File:Lijiang_Yunnan_Doors-_in-old-town-01.jpg', 'Wikimedia Commons', '2026-05-19');

-- 社区游记数据
-- 文案按小红书高互动笔记的路线结构改写；互动数来自公开笔记详情，图片仍使用开放授权素材。
INSERT INTO `tm_post` (`id`, `user_id`, `title`, `content`, `images`, `destination`, `tags`, `like_count`, `comment_count`, `collect_count`, `view_count`, `status`, `source_name`, `source_url`, `data_checked_date`) VALUES
(1, 3, '北京4天｜故宫中轴线+长城高收藏路线', '参考高收藏北京攻略后整理：Day1走天安门、故宫、前门和大栅栏，适合把中轴线和老城逛吃放在一起；Day2安排升旗、国家博物馆和人民大会堂周边；Day3走雍和宫、五道营、鼓楼和什刹海；Day4留给八达岭长城。故宫和热门场馆要提前预约，冬季看升旗和长城要注意保暖。', 'https://upload.wikimedia.org/wikipedia/commons/e/ef/The_Forbidden_City_-_View_from_Coal_Hill.jpg,https://upload.wikimedia.org/wikipedia/commons/5/50/Badaling_China_Great-Wall-of-China-01.jpg,https://upload.wikimedia.org/wikipedia/commons/f/fb/20090530_Beijing_Summer_Palace_8467.jpg', '北京', '北京,故宫,长城,前门,小红书高收藏', 22000, 384, 21000, 68000, 1, '小红书高互动笔记：宝宝九九（改写）', 'https://www.xiaohongshu.com/search_result/694cff5f000000001d03d59b?xsec_token=ABelvmINpKxL7R_CYKkAE9RXb8ZxE85ZoG-NsJ1g1OO0E=&xsec_source=', '2026-05-19'),
(2, 4, '上海3天2晚｜外滩陆家嘴+武康路豫园', '参考高互动上海三日路线后整理：Day1走南京路、陆家嘴、外滩和码头夜景；Day2安排武康路、新天地、思南公馆、豫园和城隍庙；Day3走人民广场、多伦路、甜爱路、鲁迅公园和M50。交通上虹桥站衔接地铁更方便，外滩夜景建议预留傍晚到夜间。', 'https://upload.wikimedia.org/wikipedia/commons/2/2b/Shanghai_Bund-20150516-RM-173803.jpg,https://upload.wikimedia.org/wikipedia/commons/8/86/Blue_hour_view_of_the_Bund_from_the_Shanghai_World_Financial_Center_dllu.jpg', '上海', '上海,外滩,陆家嘴,武康路,小红书高收藏', 8317, 188, 7003, 29600, 1, '小红书高互动笔记：讨厌吃香菇（改写）', 'https://www.xiaohongshu.com/search_result/69532e88000000001e021fab?xsec_token=AB1tF-nvxStX6A5gtwSy0VPsGo0Ib6BwZ_JIejRXZw2OU=&xsec_source=', '2026-05-19'),
(3, 3, '三亚度假｜亚龙湾海棠湾选酒店路线', '参考高互动三亚度假笔记后整理：如果不追求密集打卡，亚龙湾和海棠湾更适合泡酒店、看海和休闲；蜈支洲岛适合晴天且愿意玩项目时安排；大东海适合傍晚餐吧和夜景；三亚湾、椰梦长廊更看天气和晚霞。三亚海色受天气影响很大，行程最好保留机动日。', 'https://upload.wikimedia.org/wikipedia/commons/4/44/Yalong_Bay_01.jpg,https://upload.wikimedia.org/wikipedia/commons/0/04/Yalong_Bay_from_hotel.JPG', '三亚', '三亚,亚龙湾,海棠湾,度假酒店,小红书高收藏', 5682, 127, 4045, 18800, 1, '小红书高互动笔记：重生之我是富家千金（改写）', 'https://www.xiaohongshu.com/search_result/68d3da6b000000001101c64f?xsec_token=ABL1I3s0l7Q-ClmH7gt7dlSMXrF-Epb6fMeUDb-Hv-caM=&xsec_source=', '2026-05-19'),
(4, 4, '成都+川西7天｜熊猫基地九寨沟都江堰', '参考高收藏成都川西路线后整理：Day1成都市区，Day2三星堆后前往九寨沟，Day3九寨沟一日，Day4前往四姑娘山并看猫鼻梁，Day5双桥沟，Day6都江堰，Day7熊猫基地。路线跨度较大，适合时间充足的人，预算和交通都要提前规划，并根据天气和身体状态调整。', 'https://upload.wikimedia.org/wikipedia/commons/1/13/Chengdu_Hotpot.jpg,https://upload.wikimedia.org/wikipedia/commons/2/26/Chengdu_Kuanzhai_Alley_Touristic_Spot_Relics_%E6%88%90%E9%83%BD%E5%AE%BD%E7%AA%84%E5%B7%B7%E5%AD%90%E6%96%87%E7%89%A9%E5%8F%91%E6%8E%98%E9%81%97%E4%BA%A7.jpg', '成都', '成都,川西,九寨沟,都江堰,小红书高收藏', 3487, 122, 3710, 15600, 1, '小红书高互动笔记：平行世界（改写）', 'https://www.xiaohongshu.com/search_result/695bab330000000022038a9d?xsec_token=ABzZxehAXwcVFKsH-66ZM73gO3UXs6rlftnrpJlPmtPLQ=&xsec_source=', '2026-05-19'),
(5, 2, '丽江一日游｜古城周边8点位速览', '参考丽江一日高互动笔记后整理：时间紧时可以把丽江古城作为核心，按不走回头路的思路串联古城街巷、美食点和周边观景点；如果想继续衔接玉龙雪山，需要单独预留一天，提前看索道票、天气和高海拔适应情况。', 'https://upload.wikimedia.org/wikipedia/commons/7/74/1_lijiang_old_town_night.jpg,https://upload.wikimedia.org/wikipedia/commons/b/b9/Lijiang_Yunnan_Doors-_in-old-town-01.jpg', '丽江', '丽江,丽江古城,一日游,小红书高收藏', 1171, 23, 1015, 6200, 1, '小红书高互动笔记：丽江一日游笔记（改写）', 'https://www.xiaohongshu.com/search_result/691ac47f000000001b0239d2?xsec_token=ABbtvPWmxV0rfwgnLPwHKTm9Veh9BCdTioJqPISej8WkA=&xsec_source=', '2026-05-19')
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
(1, '故宫博物院官方导览一日', '基于故宫博物院开放时间与官方导览信息整理，适合首次到访的中轴线游览。价格为演示服务费，不代表官方票价。', 0, '北京', '故宫博物院', '1天', 128.00, 'https://upload.wikimedia.org/wikipedia/commons/e/ef/The_Forbidden_City_-_View_from_Coal_Hill.jpg', '故宫博物院官网', 'https://www.dpm.org.cn/Visit.html', '2026-05-19'),
(2, '颐和园昆明湖半日游', '基于颐和园开放时间与园区导览整理，覆盖东宫门、仁寿殿、长廊、昆明湖等节点。价格为演示服务费。', 0, '北京', '颐和园', '0.5天', 98.00, 'https://upload.wikimedia.org/wikipedia/commons/f/fb/20090530_Beijing_Summer_Palace_8467.jpg', '颐和园官网', 'https://www.summerpalace.net.cn/visit.html', '2026-05-19'),
(3, '杭州西湖环湖一日游', '基于西湖景区公开信息整理，串联断桥、白堤、苏堤、花港观鱼和湖滨区域。价格为演示服务费。', 0, '杭州', '西湖', '1天', 118.00, 'https://upload.wikimedia.org/wikipedia/commons/f/fd/Hangzhou_Skyline_against_the_West_Lake.png', '杭州西湖风景名胜区管委会', 'https://westlake.hangzhou.gov.cn/', '2026-05-19'),
(4, '黄山风景区经典两日游', '基于黄山风景区公开信息整理，覆盖云谷寺、始信峰、北海、光明顶、玉屏楼等常规节点。价格为演示服务费。', 1, '黄山', '黄山风景区', '2天1晚', 398.00, 'https://upload.wikimedia.org/wikipedia/commons/2/28/Anhui_Huangshan.jpg', '黄山风景区管委会', 'https://hsgwh.huangshan.gov.cn/', '2026-05-19'),
(5, '九寨沟核心景点一日游', '基于九寨沟景区票务与服务信息整理，覆盖树正沟、日则沟、则查洼沟核心观景点。价格为演示服务费。', 0, '阿坝', '九寨沟风景名胜区', '1天', 268.00, 'https://upload.wikimedia.org/wikipedia/commons/2/28/1_jiuzhaigou_valley_wu_hua_hai_2011b.jpg', '九寨沟风景名胜区官网', 'https://www.jiuzhai.com/intelligent-service/tickets', '2026-05-19'),
(6, '桂林漓江游船一日游', '基于桂林文旅公开信息整理，围绕桂林至阳朔漓江游船线路设计。价格为演示服务费。', 0, '桂林', '漓江风景名胜区', '1天', 288.00, 'https://upload.wikimedia.org/wikipedia/commons/9/92/1_li_jiang_guilin_yangshuo_2011.jpg', '桂林市文化广电和旅游局', 'https://wglj.guilin.gov.cn/', '2026-05-19');

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
  `discount_type` TINYINT(1) DEFAULT '0' COMMENT '0=满减, 1=折扣',
  `discount_value` DECIMAL(10,2) DEFAULT '0.00' COMMENT '减免金额或折扣比例',
  `min_amount` DECIMAL(10,2) DEFAULT '0.00' COMMENT '最低消费金额',
  `expire_date` DATETIME DEFAULT NULL COMMENT '过期时间',
  `stock` INT DEFAULT '100' COMMENT '可领取数量',
  `status` TINYINT(1) DEFAULT '0' COMMENT '0=有效, 1=已过期',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

-- 示例优惠券数据
INSERT IGNORE INTO `tm_coupon` (`name`, `description`, `discount_type`, `discount_value`, `min_amount`, `expire_date`, `stock`, `status`) VALUES
('新用户专享', '新用户首单立减50元', 0, 50.00, 200.00, '2026-12-31 23:59:59', 200, 0),
('机票满减券', '机票订单满500减30', 0, 30.00, 500.00, '2026-12-31 23:59:59', 150, 0),
('酒店9折券', '酒店订单享9折优惠', 1, 0.90, 0.00, '2026-12-31 23:59:59', 100, 0),
('火车票85折', '火车票订单享85折', 1, 0.85, 100.00, '2026-12-31 23:59:59', 80, 0);

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
  INDEX `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券关联表';

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
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_review_id` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价举报表';
