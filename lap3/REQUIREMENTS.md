# Yêu cầu Lab 3 — Mail

## Bắt buộc theo đề

- Mail Server kèm client hoặc Mail Client.
- Có SMTP và POP3.
- Gửi/đọc được trên Gmail và localhost; dữ liệu lưu database phía ứng dụng/server.

## Phạm vi đã chọn

- JavaFX Mail Client với đăng ký/đăng nhập tài khoản ứng dụng bằng MySQL.
- Gmail: SMTP STARTTLS `587`, POP3S `995`, xác thực bằng App Password.
- Localhost: GreenMail SMTP `3025`, POP3 `3110`, chạy nhúng hoặc tiến trình riêng trên máy A.
- MySQL lưu user ứng dụng và lịch sử thư gửi.

## Tiêu chí nghiệm thu

- Gửi và đọc thư giữa `user1@localhost`/`user2@localhost` trên hai máy.
- Gửi thư tới địa chỉ Gmail và đọc mailbox Gmail khi tài khoản cho phép POP.
- UI không treo trong lúc kết nối; lỗi xác thực/mạng được hiển thị rõ.
- Test localhost SMTP/POP3 chạy tự động và không cần tài khoản Gmail thật.

## Ngoài phạm vi

Không xây mail transfer agent công cộng, IMAP, spam filter hoặc lưu attachment ở giai đoạn hiện tại.
