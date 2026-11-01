# Lab 2 - FTP Server kèm FTP Client bằng Java

Ứng dụng tự cài đặt giao thức FTP trên TCP Socket, gồm FTP Server có MySQL và FTP Client Swing hai cột tương tự FileZilla. Không dùng Apache FtpServer hoặc thư viện FTP cấp cao.

## Chức năng

- Đăng nhập bằng `USER` / `PASS`, tài khoản lưu trong MySQL.
- Danh sách file, đổi thư mục, tạo/xóa thư mục và xóa file.
- Upload (`STOR`) và download (`RETR`) mọi loại file ở chế độ binary.
- Hỗ trợ data connection **PASV** và **PORT**; client đi kèm dùng PASV.
- Ghi nhật ký upload/download vào bảng `transfer_logs`.
- Mỗi user có home riêng và không thể dùng `..` để thoát ra ngoài.
- Nhiều client đồng thời qua thread pool.

## Kiến trúc FTP

FTP dùng hai kết nối TCP độc lập:

```text
FTP Client ── TCP control :2121 ──> FTP Server
           USER, PASS, PWD, LIST, RETR, STOR...

FTP Client ── TCP data tạm thời ──> FTP Server (PASV)
           danh sách thư mục hoặc byte của file
```

- **Control connection** tồn tại suốt phiên và trao đổi lệnh/response mã 3 chữ số.
- **Data connection** được mở riêng cho từng `LIST`, `RETR` hoặc `STOR`, rồi đóng.
- **PASV:** server mở một cổng tạm, client chủ động kết nối. Dễ chạy qua NAT/firewall hơn.
- **PORT:** client mở cổng và yêu cầu server chủ động kết nối ngược lại.
- TCP phù hợp chuyển file vì đảm bảo đủ byte, đúng thứ tự và tự truyền lại khi mất gói.

## Yêu cầu

- Temurin JDK 21 x64; Maven biên dịch mã ở mức Java 17.
- MySQL từ môi trường Docker chung ở root hoặc MySQL 8.4 cài trực tiếp.
- PowerShell và kết nối Internet trong lần build đầu để Maven tải dependency.

## Chuẩn bị MySQL

Sửa `db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306
db.name=lab2_ftp
db.user=root
db.password=MAT_KHAU_MYSQL
```

Server tự tạo database `lab2_ftp`, hai bảng `users`, `transfer_logs`, cùng hai tài khoản:

- `alice` / `123456`
- `bob` / `123456`

Mật khẩu để dạng rõ chỉ nhằm phục vụ bài thực hành và demo giao thức, không dùng cho hệ thống thật.

## Chạy

Terminal thứ nhất, tại thư mục `lap2`:

```powershell
.\run-server.ps1
```

Terminal thứ hai (có thể mở nhiều client):

```powershell
.\run-client.ps1
```

Nhập `localhost`, cổng `2121`, tài khoản `alice` hoặc `bob`. Cột trái là máy local, cột phải là server. Nhấp đúp thư mục để đi vào.

Nếu PowerShell chặn script:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-server.ps1
```

## Kiểm thử tích hợp

Khởi động server trước, sau đó:

```powershell
.\compile.ps1
.\mvnw.cmd -q compile exec:java "-Dexec.mainClass=lab2.test.FtpIntegrationTest"
```

Test đăng nhập, upload, `LIST`, download, so sánh byte và xóa file. Kết quả cuối:

```text
TẤT CẢ KIỂM THỬ FTP ĐÃ THÀNH CÔNG.
```

## Kết nối bằng FileZilla thật

Tạo site với:

- Protocol: FTP
- Host: `127.0.0.1`
- Port: `2121`
- Encryption: Only use plain FTP
- Logon Type: Normal
- User/password: `alice` / `123456`
- Transfer mode: Passive

Server hỗ trợ: `USER`, `PASS`, `QUIT`, `SYST`, `FEAT`, `OPTS`, `PWD`, `CWD`, `CDUP`, `TYPE`, `PASV`, `PORT`, `LIST`, `NLST`, `RETR`, `STOR`, `DELE`, `MKD`, `RMD`, `SIZE`, `NOOP`.
PASV dùng dải cố định `30000-30020` để cấu hình Windows Firewall giống nhau trên hai máy.

## Kịch bản trình bày

1. Chạy server và hai client đăng nhập bằng `alice`, `bob`.
2. Upload một file từ cột local; mở client khác để chứng minh home tách riêng.
3. Download file và so sánh nội dung.
4. Tạo/xóa thư mục, quan sát lệnh và response FTP ở khung log.
5. Mở MySQL:

```sql
USE lab2_ftp;
SELECT username, command, filename, bytes, created_at
FROM transfer_logs ORDER BY id DESC;
```

6. Kết nối FileZilla thật để chứng minh server sử dụng FTP chuẩn.
7. Giải thích control channel, data channel và khác nhau giữa PASV/PORT.

## Giới hạn có chủ đích

Không hỗ trợ FTPS/TLS, resume (`REST`), IPv6, symbolic link hoặc phân quyền hệ điều hành. Cổng `2121` được dùng thay cổng chuẩn `21` để không cần quyền Administrator.
