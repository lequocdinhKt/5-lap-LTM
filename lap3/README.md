# Lab 3 - Mail Client

Ứng dụng JavaFX gửi và đọc email bằng SMTP/POP3 trên hai môi trường:

- Gmail: SMTP `smtp.gmail.com:587`, POP3S `pop.gmail.com:995`.
- Localhost: GreenMail chạy cùng ứng dụng, SMTP `3025`, POP3 `3110`.
- MySQL lưu tài khoản ứng dụng và lịch sử thư đã gửi.

## Chuẩn bị MySQL

1. Cài MySQL Community Server và MySQL Workbench.
2. Giữ cổng mặc định `3306` và nhớ mật khẩu của tài khoản `root`.
3. Mở `db.properties`, thay `YOUR_MYSQL_PASSWORD` bằng mật khẩu MySQL.

Ứng dụng tự tạo database `lab3_mail` cùng hai bảng `users` và `mails` khi thao tác lần đầu.

## Chạy chương trình

Yêu cầu Temurin JDK 21 x64. Maven Wrapper tự tải JavaFX và các dependency.
Chạy một máy bằng `run.bat` hoặc lệnh:

```powershell
.\mvnw.cmd javafx:run
```

Chạy giữa hai máy:

```powershell
# Máy A: mail server local lắng nghe trên LAN
.\run-local-server.ps1

# Máy B: thay IP bên dưới bằng IPv4 của máy A
.\run-client.ps1 -ServerHost 192.168.1.10
```

Để chạy client và mail server nhúng trên cùng một máy:

```powershell
.\run-client.ps1 -EmbeddedMailServer
```

Máy A cần mở TCP `3025` và `3110`; script `configure-firewall.ps1` ở root thực hiện việc này.

## Kiểm tra localhost

1. Đăng ký và đăng nhập một tài khoản ứng dụng.
2. Ở tab Gửi mail, chọn `Localhost`.
3. Gửi từ `user1@localhost` đến `user2@localhost`, mật khẩu `123456`.
4. Ở tab Đọc mail, chọn `Localhost`, nhập `user2@localhost` và mật khẩu `123456`.

## Kiểm tra Gmail

Chọn `Gmail`, nhập địa chỉ Gmail và App Password của tài khoản. Không nhập mật khẩu Gmail thông thường.

## Tách lớp

- `ui`: hiển thị và nhận dữ liệu từ người dùng.
- `service`: kiểm tra và xử lý nghiệp vụ.
- `network`: SMTP, POP3 và mail server localhost.
- `db`: kết nối và thao tác MySQL.
