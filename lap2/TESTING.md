# Kiểm thử Lab 2

## Build

```powershell
.\mvnw.cmd -q clean verify
```

## Integration test hiện có

Chạy server trước, sau đó:

```powershell
.\mvnw.cmd -q compile exec:java "-Dexec.mainClass=lab2.test.FtpIntegrationTest"
```

Test đăng nhập, upload, LIST, download, so sánh byte và xóa file.

## Ma trận test bổ sung

- Sai user/password; lệnh cần auth trước khi login.
- Upload/download file rỗng, Unicode, PDF/MP3/MP4 và file lớn.
- Hai client upload đồng thời; hai user không nhìn thấy home của nhau.
- PORT/PASV sai định dạng; data port bị chiếm; client ngắt giữa transfer.
- `CWD ..`, tên chứa `../` và symbolic link không thoát root.
- FileZilla thật ở passive mode.

## Sau khi đổi JavaFX

Giữ integration test độc lập UI. Bổ sung test service/progress và kiểm tra UI không treo khi transfer; build phải chạy được khi không có màn hình đồ họa.
