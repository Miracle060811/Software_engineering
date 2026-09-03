CREATE TABLE IF NOT EXISTS `tm_tour_schedule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL COMMENT '旅游产品ID',
  `travel_date` DATE NOT NULL COMMENT '出行日期',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '班期单人价格',
  `total_stock` INT NOT NULL COMMENT '班期总库存',
  `available_stock` INT NOT NULL COMMENT '班期可售库存',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '0=停售, 1=可售',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tour_schedule_product_date` (`product_id`, `travel_date`),
  KEY `idx_tour_schedule_sale` (`product_id`, `status`, `travel_date`),
  CONSTRAINT `fk_tour_schedule_product` FOREIGN KEY (`product_id`) REFERENCES `tm_tour_product` (`id`),
  CONSTRAINT `chk_tour_schedule_stock` CHECK (`total_stock` >= 0 AND `available_stock` >= 0 AND `available_stock` <= `total_stock`),
  CONSTRAINT `chk_tour_schedule_price` CHECK (`unit_price` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一日游/周边游可售班期';

CREATE TABLE IF NOT EXISTS `tm_tour_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '下单用户ID',
  `product_id` BIGINT NOT NULL COMMENT '旅游产品ID',
  `schedule_id` BIGINT NOT NULL COMMENT '班期ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '产品名称快照',
  `tour_type` TINYINT(1) NOT NULL COMMENT '0=一日游, 1=周边游',
  `travel_date` DATE NOT NULL COMMENT '出行日期快照',
  `participant_count` INT NOT NULL COMMENT '出行人数',
  `contact_name` VARCHAR(50) NOT NULL COMMENT '联系人姓名',
  `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系人手机号',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '单人价格快照',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `idempotency_key` VARCHAR(64) NOT NULL COMMENT '客户端幂等键',
  `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '1=已预订/待出行, 2=已完成, 4=已取消',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tour_order_no` (`order_no`),
  UNIQUE KEY `uk_tour_order_user_idempotency` (`user_id`, `idempotency_key`),
  KEY `idx_tour_order_user_create` (`user_id`, `create_time`),
  KEY `idx_tour_order_schedule` (`schedule_id`),
  CONSTRAINT `fk_tour_order_product` FOREIGN KEY (`product_id`) REFERENCES `tm_tour_product` (`id`),
  CONSTRAINT `fk_tour_order_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `tm_tour_schedule` (`id`),
  CONSTRAINT `chk_tour_order_count` CHECK (`participant_count` > 0),
  CONSTRAINT `chk_tour_order_amount` CHECK (`unit_price` > 0 AND `amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一日游/周边游订单';

-- Dynamic demo schedules are added only when the product has no future saleable schedule.
-- Re-running the SQL therefore preserves historical schedules referenced by existing orders.
INSERT INTO `tm_tour_schedule`
  (`product_id`, `travel_date`, `unit_price`, `total_stock`, `available_stock`, `status`)
SELECT
  seed.`product_id`, seed.`travel_date`, seed.`unit_price`, seed.`total_stock`, seed.`available_stock`, seed.`status`
FROM (
  SELECT 1 AS `product_id`, DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY) AS `travel_date`, 128.00 AS `unit_price`, 30 AS `total_stock`, 30 AS `available_stock`, 1 AS `status`
  UNION ALL SELECT 1, DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 138.00, 30, 30, 1
  UNION ALL SELECT 3, DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY), 118.00, 25, 25, 1
  UNION ALL SELECT 3, DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY), 128.00, 25, 25, 1
  UNION ALL SELECT 4, DATE_ADD(CURRENT_DATE, INTERVAL 9 DAY), 398.00, 20, 20, 1
  UNION ALL SELECT 4, DATE_ADD(CURRENT_DATE, INTERVAL 16 DAY), 428.00, 20, 20, 1
  UNION ALL SELECT 7, DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 468.00, 20, 20, 1
  UNION ALL SELECT 8, DATE_ADD(CURRENT_DATE, INTERVAL 11 DAY), 498.00, 20, 20, 1
  UNION ALL SELECT 9, DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 158.00, 30, 30, 1
  UNION ALL SELECT 10, DATE_ADD(CURRENT_DATE, INTERVAL 13 DAY), 168.00, 30, 30, 1
) AS seed
WHERE NOT EXISTS (
  SELECT 1
  FROM `tm_tour_schedule` AS existing
  WHERE existing.`product_id` = seed.`product_id`
    AND existing.`travel_date` >= CURRENT_DATE
    AND existing.`status` = 1
);
