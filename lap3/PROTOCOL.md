# SMTP/POP3 Lab 3

## Máy chủ

| Chế độ | Gửi | Đọc | Bảo mật |
|---|---|---|---|
| Localhost | SMTP `<LAB3_LOCAL_MAIL_HOST>:3025` | POP3 `<host>:3110` | plain, chỉ demo LAN |
| Gmail | SMTP `smtp.gmail.com:587` | POP3S `pop.gmail.com:995` | STARTTLS / TLS |

`LAB3_LOCAL_MAIL_HOST` mặc định `127.0.0.1`. Server riêng bind `0.0.0.0`; server nhúng mặc định bind `127.0.0.1`. `LAB3_EMBEDDED_MAIL_SERVER` điều khiển chế độ nhúng.

## SMTP rút gọn

```text
Client -> kết nối/authenticate
Client -> MAIL FROM
Client -> RCPT TO
Client -> DATA + MIME message
Server -> chấp nhận và đưa vào mailbox
```

Implementation dùng Jakarta Mail nhưng lựa chọn protocol/host/port nằm tại `MailProtocol.sendMail()`.

## POP3 rút gọn

```text
Client -> USER/PASS hoặc TLS authentication
Client -> mở INBOX, lấy message list
Client -> đọc From/Subject/body ở chế độ READ_ONLY
Client -> QUIT/close
```

`MailProtocol.readMails()` dùng `pop3` cho local và `pop3s` cho Gmail.

## Tài khoản demo

- `user1@localhost / 123456`
- `user2@localhost / 123456`

Chỉ dùng cho local lab. Gmail dùng App Password từ người dùng và không được lưu/commit.

## Lỗi cần xử lý

Sai credentials, Gmail chưa bật/cho phép POP, DNS/Internet lỗi, local server chưa chạy, port bị firewall chặn và MIME body không hỗ trợ.
