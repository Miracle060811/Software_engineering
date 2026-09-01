ALTER TABLE `tm_user`
  ADD COLUMN `token_version` INT NOT NULL DEFAULT 0 COMMENT '登录令牌版本，账号安全状态变化时递增'
  AFTER `role`;
