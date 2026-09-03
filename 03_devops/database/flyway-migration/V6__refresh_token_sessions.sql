CREATE TABLE IF NOT EXISTS `auth_refresh_session` (
  `id` CHAR(36) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `token_hash` CHAR(64) NOT NULL,
  `token_version` INT NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `revoked_at` DATETIME NULL,
  `source_ip` VARCHAR(64) NULL,
  `user_agent` VARCHAR(255) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_used_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
  KEY `idx_refresh_session_user` (`user_id`, `revoked_at`, `expires_at`),
  CONSTRAINT `fk_refresh_session_user`
    FOREIGN KEY (`user_id`) REFERENCES `tm_user` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
