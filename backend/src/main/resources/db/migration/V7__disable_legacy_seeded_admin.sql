-- 仅禁用仍保留公开默认密码哈希的历史种子管理员。
-- V3 会把最早的 admin123 默认哈希更新为 123456 的默认哈希，
-- 因此同时覆盖这两个已知种子值。
-- 已修改密码的真实管理员不会匹配此条件，也不会受到影响。
UPDATE tm_user
SET status = 0,
    token_version = COALESCE(token_version, 0) + 1
WHERE id = 1
  AND username = 'admin'
  AND role = 1
  AND status = 1
  AND deleted = 0
  AND password IN (
      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTpSn4DZe6m',
      '$2a$10$EqXcym8OtggJIwHYz1TkMOzw0RoZYxzv6m9Ge7tGk64gdbghNlKhG'
  );
