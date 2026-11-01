# Quy tắc Lab 2 — FTP

- Tự cài đặt FTP trên TCP socket; không dùng thư viện FTP server/client cấp cao để che giao thức.
- Control channel mặc định `2121`; PASV chỉ dùng `30000-30020` để firewall hai máy nhất quán.
- Một data connection chỉ phục vụ một lệnh `LIST`, `RETR` hoặc `STOR`, sau đó phải đóng.
- Mọi lệnh mới phải có reply code hợp lệ, cập nhật `PROTOCOL.md` và test tích hợp.
- Mọi đường dẫn phải được chuẩn hóa và không được thoát khỏi home của người dùng.
- UI hiện dùng JavaFX; mọi cải tiến phải giữ `FtpClientSession` độc lập UI và không đổi hành vi FTP ngoài chủ đích.
