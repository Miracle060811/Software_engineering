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
  `cover_img` VARCHAR(255) COMMENT '封面图',
  `lat` DECIMAL(10,6) COMMENT '纬度',
  `lng` DECIMAL(10,6) COMMENT '经度',
  `avg_price` DECIMAL(10,2) DEFAULT '0' COMMENT '平均价格',
  `score` DECIMAL(3,1) DEFAULT '4.5' COMMENT '评分',
  `status` TINYINT(1) DEFAULT '1' COMMENT '1-营业 0-停业',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店基础信息表';

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
  `cover_img` VARCHAR(255) COMMENT '封面图',
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
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_destination` (`destination`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区游记表';

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
INSERT IGNORE INTO `tm_hotel` (`id`, `name`, `city`, `address`, `star_rating`, `description`, `cover_img`, `lat`, `lng`, `avg_price`, `score`) VALUES
(1, '北京国贸大酒店', '北京', '朝阳区建国门外大街1号', 5, '位于北京CBD核心地带，毗邻国贸商圈，提供顶级商务服务与绝佳城市景观。', 'https://picsum.photos/400/300?random=1', 39.9087, 116.4575, 1580.00, 4.8),
(2, '上海外滩华尔道夫酒店', '上海', '黄浦区中山东一路2号', 5, '俯瞰黄浦江与外滩的绝美景观，百年历史建筑与现代奢华的完美融合。', 'https://picsum.photos/400/300?random=2', 31.2400, 121.4900, 2380.00, 4.9),
(3, '广州白天鹅宾馆', '广州', '荔湾区沙面岛南街1号', 5, '坐落于珠江畔的经典豪华酒店，独特的岭南园林特色，尽显广州风情。', 'https://picsum.photos/400/300?random=3', 23.1198, 113.2432, 1280.00, 4.7),
(4, '成都锦江宾馆', '成都', '锦江区人民南路二段80号', 4, '成都历史最悠久的豪华宾馆，毗邻天府广场，展现巴蜀文化底蕴。', 'https://picsum.photos/400/300?random=4', 30.6500, 104.0633, 880.00, 4.6),
(5, '西安大唐芙蓉园精品酒店', '西安', '雁塔区芙蓉南路100号', 4, '紧邻大唐芙蓉园景区，盛唐风格设计，感受千年古都的文化魅力。', 'https://picsum.photos/400/300?random=5', 34.2076, 109.0082, 680.00, 4.5),
(6, '三亚亚龙湾万豪度假酒店', '三亚', '天涯区亚龙湾国家旅游度假区', 5, '直面南海碧波，拥有私人沙滩，豪华海景套房配备顶级度假设施。', 'https://picsum.photos/400/300?random=6', 18.2208, 109.6600, 2880.00, 4.9),
(7, '丽江古城铂尔曼大酒店', '丽江', '古城区象山路99号', 5, '融合纳西族传统建筑美学，可俯瞰玉龙雪山与古城全景。', 'https://picsum.photos/400/300?random=7', 26.8720, 100.2275, 1680.00, 4.8),
(8, '杭州西湖喜来登大酒店', '杭州', '西湖区北山路9号', 5, '坐拥西湖一线湖景，经典欧式建筑，步行可达断桥残雪等知名景点。', 'https://picsum.photos/400/300?random=8', 30.2563, 120.1495, 1980.00, 4.8),
(9, '重庆解放碑威斯汀酒店', '重庆', '渝中区中山三路36号', 5, '位于解放碑商业核心，俯瞰山城夜景，提供顶级商务设施。', 'https://picsum.photos/400/300?random=9', 29.5600, 106.5714, 1380.00, 4.7),
(10, '北京王府井万豪酒店', '北京', '东城区王府井大街57号', 5, '紧邻王府井商业街，文化底蕴深厚，距故宫、天安门仅步行10分钟。', 'https://picsum.photos/400/300?random=10', 39.9200, 116.4114, 1880.00, 4.8),
(11, '上海静安香格里拉大酒店', '上海', '静安区静安寺路1218号', 5, '毗邻南京西路时尚商圈，俯瞰静安寺夜景，融汇都市活力。', 'https://picsum.photos/400/300?random=11', 31.2244, 121.4471, 1680.00, 4.7),
(12, '厦门悦华酒店', '厦门', '湖里区乌石浦路2号', 4, '临近厦门海湾，风景如画，是商务与休闲出行的理想选择。', 'https://picsum.photos/400/300?random=12', 24.5020, 118.0936, 780.00, 4.5),
(13, '桂林香格里拉大酒店', '桂林', '象山区解放东路111号', 5, '坐落于漓江之畔，将现代豪华与桂林山水融为一体。', 'https://picsum.photos/400/300?random=13', 25.2877, 110.2978, 1180.00, 4.8),
(14, '青岛海景花园大酒店', '青岛', '市南区香港中路76号', 5, '俯瞰青岛湾，依山傍海，是观赏岛城日落的绝佳之所。', 'https://picsum.photos/400/300?random=14', 36.0584, 120.3817, 1280.00, 4.7);

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
(1, '故宫博物院', '北京', '东城区景山前街4号', '明清两代皇家宫殿，世界文化遗产，馆藏体系覆盖古代宫廷建筑、书画、器物等。', NULL, 60.00, 0.00, 1000, 456, '08:30-17:00（旺季，周一闭馆，法定节假日除外）', 39.916345, 116.397155, 'https://www.dpm.org.cn/Visit.html', '故宫博物院官网', '2026-05-19'),
(2, '颐和园', '北京', '海淀区新建宫门路19号', '以昆明湖、万寿山为主体的大型皇家园林，1998年列入世界遗产名录。', NULL, 30.00, 0.00, 2000, 1234, '06:00-20:00（旺季，停止入园19:00）', 39.999946, 116.275481, 'https://www.summerpalace.net.cn/visit.html', '颐和园官网', '2026-05-19'),
(3, '外滩', '上海', '黄浦区中山东一路', '上海近代城市景观代表区域，沿黄浦江分布多座历史建筑，是城市公共开放空间。', NULL, 0.00, 0.00, 9999, 9999, '全天开放', 31.239706, 121.490317, 'https://www.shhuangpu.gov.cn/', '上海市黄浦区人民政府', '2026-05-19'),
(4, '西湖', '杭州', '西湖区龙井路1号', '杭州西湖文化景观为世界文化遗产，核心湖区与沿湖开放空间面向公众开放。', NULL, 0.00, 0.00, 9999, 9999, '全天开放（部分收费景点另行开放）', 30.242703, 120.150269, 'https://westlake.hangzhou.gov.cn/', '杭州西湖风景名胜区管委会', '2026-05-19'),
(5, '张家界国家森林公园', '张家界', '武陵源区国家森林公园内', '中国第一个国家森林公园，武陵源世界自然遗产核心景区之一，以石英砂岩峰林地貌著称。', NULL, 227.00, 113.00, 500, 234, '07:30-18:00（以景区当日公告为准）', 29.327001, 110.475704, 'https://wly.hunan.gov.cn/', '湖南省文化和旅游厅/武陵源景区公开信息', '2026-05-19'),
(6, '秦始皇帝陵博物院', '西安', '临潼区秦陵北路', '以秦始皇兵马俑坑和秦始皇陵相关遗址为核心的遗址类博物馆。', NULL, 120.00, 0.00, 800, 356, '08:30-18:30（旺季，停止检票17:00）', 34.384018, 109.278491, 'https://www.bmy.com.cn/', '秦始皇帝陵博物院官网', '2026-05-19'),
(7, '九寨沟风景名胜区', '阿坝', '阿坝藏族羌族自治州九寨沟县漳扎镇', '以高山湖泊、瀑布群、彩林和雪峰景观闻名的世界自然遗产。', NULL, 190.00, 95.00, 1000, 445, '07:30-17:00（旺季，具体以景区公告为准）', 33.260772, 103.918599, 'https://www.jiuzhai.com/intelligent-service/tickets', '九寨沟风景名胜区官网', '2026-05-19'),
(8, '黄山风景区', '黄山', '黄山市黄山区汤口镇', '世界文化与自然双重遗产，以奇松、怪石、云海、温泉等景观著称。', NULL, 190.00, 95.00, 1500, 678, '06:00-17:30（旺季，具体以景区公告为准）', 30.130130, 118.168498, 'https://hsgwh.huangshan.gov.cn/', '黄山风景区管委会', '2026-05-19'),
(9, '漓江风景名胜区', '桂林', '桂林市灵川县至阳朔县漓江沿线', '桂林山水代表性景区，游船线路以漓江喀斯特峰林、江湾和田园景观为核心。', NULL, 210.00, 105.00, 600, 289, '08:00-12:00（游船班次以当日公告为准）', 25.166667, 110.416667, 'https://wglj.guilin.gov.cn/', '桂林市文化广电和旅游局', '2026-05-19')
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

-- 社区游记数据
INSERT IGNORE INTO `tm_post` (`user_id`, `title`, `content`, `images`, `destination`, `tags`, `like_count`, `comment_count`, `view_count`, `status`) VALUES
(3, '北京三日游｜故宫+长城+颐和园完美攻略', '第一天打卡故宫，推荐早上8:30就去排队，人不多风景绝了！午饭在附近吃了正宗的北京烤鸭，下午去了景山公园俯瞰故宫，超美！\n\n第二天爬八达岭长城，建议穿舒适的运动鞋，台阶多但风景值！\n\n第三天颐和园，湖边散步超惬意，推荐坐船游昆明湖。', 'https://picsum.photos/400/600?random=30,https://picsum.photos/400/600?random=31,https://picsum.photos/400/600?random=32', '北京', '北京,故宫,长城,颐和园,旅游攻略', 256, 38, 1892, 1),
(4, '上海外滩夜景｜这才是魔都的正确打开方式', '外滩的夜景真的绝！站在浦西看陆家嘴的高楼大厦，配上黄浦江的波光粼粼，整个人都被震撼到了。推荐傍晚6点左右到，可以拍到日落+夜景双版本！\n\n记得去和平饭店顶楼喝杯咖啡，体验老上海的摩登风情。', 'https://picsum.photos/400/600?random=33,https://picsum.photos/400/600?random=34', '上海', '上海,外滩,夜景,魔都,打卡', 412, 67, 3241, 1),
(3, '三亚亚龙湾｜椰林沙滩就是人间天堂', '亚龙湾的海真的是蓝得不真实！沙细水清，躺在沙滩椅上听浪声，人生赢家就是这种感觉。酒店就在沙滩边，推荐住海景房，早起看日出美炸！\n\n浮潜也超推荐，能看到很多热带鱼，跟鱼共舞的感觉太棒了。', 'https://picsum.photos/400/600?random=35,https://picsum.photos/400/600?random=36,https://picsum.photos/400/600?random=37', '三亚', '三亚,亚龙湾,海岛,沙滩,浮潜', 523, 89, 4567, 1),
(4, '成都美食之旅｜找到了最正宗的火锅！', '成都真的是美食爱好者的天堂！这次来专门吃火锅，去了网红店排队2小时，但完全值得！红汤锅底配上毛肚和鸭肠，麻辣鲜香，又爱了。\n\n宽窄巷子的各种小吃也不要错过，钟水饺、龙抄手、担担面，一条街吃到底！', 'https://picsum.photos/400/600?random=38,https://picsum.photos/400/600?random=39', '成都', '成都,火锅,美食,宽窄巷子,川菜', 334, 52, 2789, 1),
(2, '丽江古城｜时光仿佛停在了这里', '丽江古城的夜晚太迷人了，小桥流水，灯火阑珊，在这里随便一个角落都是绝美的拍照场景。\n\n玉龙雪山是必去的，推荐买大索道，到达4506米的海拔，看着皑皑白雪，震撼感无法言喻。不过高原反应要注意，备好红景天！', 'https://picsum.photos/400/600?random=40,https://picsum.photos/400/600?random=41', '丽江', '丽江,古城,玉龙雪山,云南,慢生活', 278, 43, 2134, 1);

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
  `cover_img` VARCHAR(255) DEFAULT NULL COMMENT '封面图',
  `status` TINYINT(1) DEFAULT '1' COMMENT '0=下线, 1=上线',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一日游/周边游产品表';

INSERT IGNORE INTO `tm_tour_product` (`name`, `description`, `tour_type`, `departure_city`, `destination`, `duration`, `price`) VALUES
('北京故宫一日游', '含故宫门票+专业讲解，领略皇家风范', 0, '北京', '故宫博物院', '1天', 268.00),
('杭州西湖精品一日游', '西湖十景+龙井茶文化体验', 0, '杭州', '西湖景区', '1天', 198.00),
('成都大熊猫基地一日游', '近距离接触国宝大熊猫+锦里古街', 0, '成都', '大熊猫繁育基地', '1天', 228.00),
('上海周边乌镇两日游', '江南水乡乌镇+西塘古镇，含住宿一晚', 1, '上海', '乌镇', '2天1晚', 598.00),
('北京周边承德避暑山庄两日游', '皇家避暑山庄+外八庙，感受帝王避暑胜境', 1, '北京', '承德', '2天1晚', 688.00),
('广州周边清远漂流两日游', '清远漂流+温泉度假，刺激与放松之旅', 1, '广州', '清远', '2天1晚', 528.00);

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
