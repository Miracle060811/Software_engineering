CREATE TABLE IF NOT EXISTS `admin_bootstrap_token` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `secret_fingerprint` CHAR(64) NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `used_at` DATETIME NULL,
  `attempt_count` INT NOT NULL DEFAULT 0,
  `last_attempt_ip` VARCHAR(64) NULL,
  `last_result` VARCHAR(64) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_bootstrap_fingerprint` (`secret_fingerprint`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `admin_bootstrap_audit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `token_id` BIGINT NULL,
  `username` VARCHAR(64) NULL,
  `source_ip` VARCHAR(64) NULL,
  `result` VARCHAR(64) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_bootstrap_audit_time` (`create_time`),
  CONSTRAINT `fk_admin_bootstrap_audit_token`
    FOREIGN KEY (`token_id`) REFERENCES `admin_bootstrap_token` (`id`)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
