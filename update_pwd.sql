USE smart_community;
UPDATE user SET password='$2a$10$XEs59PS2AwwVQVxWBlb7Cek4u4kB7u8dlhG4Lb4m/kVYkbrllekha' WHERE username='admin';
SELECT username, real_name FROM user WHERE username='admin';
