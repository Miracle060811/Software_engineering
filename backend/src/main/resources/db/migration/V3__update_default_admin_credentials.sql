-- 将仍使用旧默认密码 admin123 的默认管理员更新为 123456。
-- 如果管理员已自行修改密码，则不覆盖其现有密码。
UPDATE `tm_user`
SET `password` = '$2a$10$EqXcym8OtggJIwHYz1TkMOzw0RoZYxzv6m9Ge7tGk64gdbghNlKhG'
WHERE `username` = 'admin'
  AND `role` = 1
  AND `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTpSn4DZe6m';
