# Kiểm thử Lab 2

## Bằng chứng refactor service ngày 06/11/2026

- `FtpClientApp` chỉ gọi `FtpService`; kiểm tra tĩnh không còn tham chiếu `FtpClientSession` trong UI.
- `FtpIntegrationTest` chạy qua `FtpService` và đạt luồng đăng nhập, upload, LIST, download đúng byte và xóa file trên MySQL local.
- `run-client.ps1` khởi động được màn hình JavaFX sau refactor; tiến trình được dừng sau smoke test.

## Build

```powershell
.\mvnw.cmd -q clean verify
```

## Integration test hiện có

Chạy server trước, sau đó:

```powershell
.\mvnw.cmd -q test-compile exec:java "-Dexec.mainClass=lab2.test.FtpIntegrationTest" "-Dexec.classpathScope=test"
```

Test đăng nhập, upload, LIST, download, so sánh byte và xóa file.

Kết quả ngày 04/11/2026 trên dịch vụ MySQL80 local: server khởi động tại `2121`; integration test đăng nhập `alice`, STOR, LIST, RETR, so sánh byte và DELE thành công. Server được dừng sau test.

## Ma trận test bổ sung

- Sai user/password; lệnh cần auth trước khi login.
- Upload/download file rỗng, Unicode, PDF/MP3/MP4 và file lớn.
- Hai client upload đồng thời; hai user không nhìn thấy home của nhau.
- PORT/PASV sai định dạng; data port bị chiếm; client ngắt giữa transfer.
- `CWD ..`, tên chứa `../` và symbolic link không thoát root.
- FileZilla thật ở passive mode.

## JavaFX

Integration test vẫn độc lập UI. UI dùng `Task`/executor; kiểm tra thủ công progress upload/download, resize SplitPane và đóng cửa sổ trong lúc đã kết nối.
