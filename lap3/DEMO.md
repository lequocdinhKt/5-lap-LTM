# Demo Lab 3

## Chuẩn bị

- Máy A chạy local mail server; Máy B chạy client với IP Máy A.
- MySQL chạy trên máy có client ứng dụng; port `3025`, `3110` đã mở trên Máy A.
- Gmail demo dùng tài khoản riêng và App Password nhập trực tiếp, không lưu trong file/video.

## Kịch bản localhost

1. Máy A chạy `run-local-server.ps1`; chỉ log SMTP/POP3.
2. Máy B chạy `run-client.ps1 -ServerHost <IP_MAY_A>`.
3. Đăng ký/login tài khoản ứng dụng.
4. Gửi từ `user1@localhost` đến `user2@localhost`.
5. Đọc POP3 của `user2@localhost`, chỉ thư vừa gửi.
6. Mở MySQL để chỉ lịch sử gửi đã lưu.

## Kịch bản Gmail

1. Chọn Gmail, nhập sender và App Password.
2. Gửi tới địa chỉ kiểm thử; kiểm tra Gmail nhận thư.
3. Đọc POP3S nếu tài khoản đã cho phép POP.

## Câu hỏi giảng viên có thể hỏi

- SMTP và POP3 chịu trách nhiệm khác nhau thế nào?
- Vì sao Gmail dùng 587/995 còn localhost dùng 3025/3110?
- STARTTLS khác POP3S ở thời điểm thiết lập TLS thế nào?
- Chức năng gửi đi qua những method nào?
- Vì sao server local chạy riêng khi demo hai máy?
- Database lưu thư nào và mailbox thật nằm ở đâu?
- Vì sao không test tự động bằng Gmail?

## Dự phòng

Nếu Gmail bị chặn bởi tài khoản/mạng, dùng localhost làm bằng chứng protocol và chiếu ảnh/video Gmail đã quay trước; không đưa App Password vào repo.
