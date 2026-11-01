# Xử lý lỗi Lab 3

## Không kết nối localhost

- Máy A phải chạy `run-local-server.ps1`, không chỉ chạy client nhúng.
- Máy B đặt `LAB3_LOCAL_MAIL_HOST` qua script bằng IPv4 Máy A.
- Kiểm tra `Test-NetConnection <IP> -Port 3025` và `-Port 3110`.
- Mở firewall profile Private; tránh dùng AP có client isolation.

## Gmail authentication failed

- Dùng App Password, không dùng mật khẩu Gmail thông thường.
- Kiểm tra địa chỉ sender trùng tài khoản tạo App Password.
- Kiểm tra POP có được phép cho tài khoản và mạng không chặn 587/995.
- Không in App Password vào log.

## Local server báo port bận

Đảm bảo chỉ một GreenMail instance. Khi chạy server riêng, client không được bật embedded server (`LAB3_EMBEDDED_MAIL_SERVER=false`).

## Database lỗi

Chạy Docker MySQL và kiểm tra `db.properties`/`LTM_DB_*`. Database ứng dụng không thay thế mailbox SMTP/POP3; hai nơi lưu dữ liệu có mục đích khác nhau.

## UI treo hoặc không cập nhật

SMTP/POP3/DB phải đi qua `MailAppController.runBackground()`. Chỉ cập nhật control trong handler thành công/thất bại trên JavaFX Application Thread.
