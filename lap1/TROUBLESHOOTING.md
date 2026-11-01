# Xử lý lỗi Lab 1

## Client không kết nối

- Dùng `ipconfig` lấy IPv4 máy server, không dùng IP Wi-Fi cũ.
- Kiểm tra server đang nghe: `Test-NetConnection <IP> -Port 5000`.
- Đặt network profile là Private và chạy `configure-firewall.ps1` bằng Administrator.

## Đăng nhập/database lỗi

- Chạy `docker compose ps` ở root và chờ MySQL healthy.
- Kiểm tra `db.properties` cục bộ hoặc các biến `LTM_DB_*`; không commit mật khẩu.
- Chạy lại `setup-windows.ps1` nếu file cấu hình chưa được tạo.

## TCP được nhưng UDP không nhận

- Kiểm tra UDP `5001-5002` trong firewall và hai máy cùng subnet.
- Tắt VPN/adapter ảo tạm thời nếu chương trình chọn sai network interface.
- Multicast có thể bị access point chặn; thử loopback trên cùng máy để phân biệt lỗi code và mạng.

## Truyền file thất bại

- Kiểm tra file dưới 20 MB, tên file hợp lệ và receiver còn online.
- Xem log `UdpFileRelay` để xác định thiếu ACK ở sender→server hay server→receiver.
- Không chạy hai server cùng bộ cổng trên một máy.

## UI treo

Network I/O không được chạy trên JavaFX Application Thread. Kiểm tra task/executor và mọi UI update từ listener phải qua `Platform.runLater`.
