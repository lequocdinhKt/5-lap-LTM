# Xử lý lỗi Lab 4

## Không truy cập từ máy khác

- Dùng `http://<IPv4-máy-server>:8080`, không dùng `localhost` trên Máy B.
- Chạy `Test-NetConnection <IP> -Port 8080` và mở firewall profile Private.
- Kiểm tra server bind được port và không bị process cũ chiếm.

## 404 dù file tồn tại

- File phải nằm dưới web root `www`; URL phân biệt đường dẫn theo mapping server.
- Kiểm tra tên/extension và log request target.
- Không dùng đường dẫn filesystem tuyệt đối trong URL.

## PDF/MP3/MP4 không hiển thị trong browser

Mini Browser không có engine media đầy đủ. Kiểm tra status, `Content-Type`, `Content-Length`, download và hash; đây vẫn là bằng chứng HTTP đúng.

## POST trả 400

Endpoint phải là `/submit`, `Content-Type` phải bắt đầu bằng `application/x-www-form-urlencoded` và Content-Length hợp lệ dưới 1 MB.

## Browser/UI treo

`MiniBrowserApp` dùng `Task`/executor. Không thực hiện socket read/download trên JavaFX Application Thread.

## Server dùng nhiều RAM với media

Hiện `FileService.Resource` giữ `byte[]`. Với file lớn, cần refactor streaming sau khi có integration test; không sửa protocol và UI cùng lúc.
