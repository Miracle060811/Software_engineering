-- 恢复默认超级管理员账号 admin/123456。
-- 该迁移覆盖 V7 对历史默认种子管理员的禁用，确保初始环境可用。
-- 仅当账号仍是已知历史默认密码（admin123 或 123456）时才会重置，
-- 已修改密码的管理员不受影响。
UPDATE `tm_user`
SET `password` = '$2a$10$EqXcym8OtggJIwHYz1TkMOzw0RoZYxzv6m9Ge7tGk64gdbghNlKhG',
    `status` = 1,
    `token_version` = COALESCE(`token_version`, 0) + 1
WHERE `username` = 'admin'
  AND `role` = 1
  AND `deleted` = 0
  AND `password` IN (
      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTpSn4DZe6m',
      '$2a$10$EqXcym8OtggJIwHYz1TkMOzw0RoZYxzv6m9Ge7tGk64gdbghNlKhG'
  );
