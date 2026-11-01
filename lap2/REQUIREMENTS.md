# Yêu cầu Lab 2 — FTP

## Bắt buộc theo đề

- Xây FTP Server kèm client hoặc FTP Client có giao thức FTP.
- Server có database.
- Cách dùng tương tự FileZilla và giải thích được control/data connection.

## Phạm vi đã chọn

- Tự cài FTP Server và client bằng TCP socket, không dùng thư viện FTP cấp cao.
- MySQL lưu user và transfer log; mỗi user có home riêng.
- Hỗ trợ USER, PASS, PWD, CWD, LIST, RETR, STOR, PASV, PORT và các lệnh quản lý file/thư mục.
- Client đi kèm mặc định dùng PASV; server có thể kiểm tra bằng FileZilla thật.

## Tiêu chí nghiệm thu

- Ít nhất hai client đăng nhập đồng thời bằng hai user.
- Upload rồi download giữ nguyên byte; thao tác không thoát khỏi home.
- PASV dùng đúng dải `30000-30020`; transfer được ghi database.
- UI mục tiêu là JavaFX hai cột và có tiến trình upload/download.

## Ngoài phạm vi

Không hỗ trợ FTPS/TLS, resume, IPv6, symbolic link hoặc quyền file hệ điều hành.
