# Xử lý lỗi Lab 2

## Login thất bại

- Kiểm tra MySQL healthy và `db.properties`/`LTM_DB_*` đúng.
- Dùng tài khoản seed `alice` hoặc `bob`; xem server log có lỗi SQL không.

## Kết nối control được nhưng LIST/transfer treo

- Đây thường là data channel/firewall. Mở `30000-30020` TCP trên máy server.
- FileZilla phải dùng Passive, plain FTP và port control `2121`.
- Kiểm tra reply `227` trả đúng IPv4 máy server; VPN có thể làm chọn sai adapter.

## `425` hoặc `550`

- `425`: client chưa gửi PASV/PORT hoặc data socket chưa kết nối.
- `550`: path/file không tồn tại, sai loại file/thư mục hoặc bị chặn khỏi home.
- Đọc chuỗi command/reply trong log trước khi sửa code.

## Port bận

```powershell
Get-NetTCPConnection -LocalPort 2121 -ErrorAction SilentlyContinue
```

Dừng server cũ; không mở hai FTP Server cùng port. Với PASV, kiểm tra process chiếm dải `30000-30020`.

## UI treo

Transfer phải chạy background; Swing hiện tại dùng `SwingWorker`, JavaFX mục tiêu phải dùng `Task`/executor và chỉ cập nhật UI trên JavaFX thread.
