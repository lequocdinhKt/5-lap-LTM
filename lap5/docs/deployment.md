# Triển khai

Build bằng JDK 21 và Maven Wrapper. Dùng cùng commit/build cho hai server và hai client; contract package `lab5.*` không tương thích JAR cũ.

Mỗi máy thật cài MySQL Server 8.x riêng. Chạy `setup-windows.ps1`, rồi `configure-server.ps1` để tạo `server.properties` cục bộ và schema; mở firewall `1099–1102`, sau đó chạy Server A trước Server B. Hai VM chỉ cần JDK 21 và source/build để chạy JavaFX client.

Không chép `.env` hoặc `server.properties` giữa máy. Mỗi máy tự cấu hình IP/password cục bộ. Timeline server nằm trong RAM và mất khi server dừng.
