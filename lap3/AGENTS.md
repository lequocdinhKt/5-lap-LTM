# Quy tắc Lab 3 — Mail

- Giữ hai chế độ: Localhost qua SMTP `3025`/POP3 `3110` và Gmail qua SMTP/POP3S.
- Không commit Gmail App Password, mật khẩu database hoặc email thật.
- UI chỉ thu thập dữ liệu; `AppService` điều phối; `MailProtocol` chịu trách nhiệm SMTP/POP3.
- Gọi mạng và database ngoài JavaFX Application Thread; trạng thái loading/error phải phản hồi trên UI.
- Thay đổi cấu hình mail phải hỗ trợ chạy hai máy bằng host truyền qua biến môi trường hoặc script.
- Mọi thay đổi gửi/đọc mail phải cập nhật cả `FEATURE_MAP.md`, `PROTOCOL.md` và test localhost.
