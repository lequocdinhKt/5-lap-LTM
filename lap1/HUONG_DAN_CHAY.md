# Hướng dẫn chạy Lab 1 trên Windows

Lab 1 dùng Maven Wrapper nên JavaFX và MySQL Connector/J được tải tự động. Không cần
cài JavaFX riêng hoặc chép JAR giữa hai máy.

## Chuẩn bị

1. Cài Temurin JDK 21 x64.
2. Từ thư mục root `5lap`, chạy `setup-windows.ps1` để tạo MySQL và `db.properties`.
3. Trên máy server, chạy `configure-firewall.ps1` bằng quyền Administrator.

## Build và chạy

```powershell
.\build.ps1
.\run-server.ps1          # Server có giao diện JavaFX
.\run-server.ps1 -Console # Hoặc server console
.\run-client.ps1          # Client JavaFX
```

Trên máy client, nhập IPv4 của máy server vào ô Server; không dùng `localhost` khi
hai chương trình chạy trên hai máy khác nhau.

Lab 1 sử dụng TCP `5000`, UDP file `5001` và UDP multicast/broadcast text `5002`.
Cả hai máy phải ở cùng mạng LAN và Windows Network Profile nên đặt là `Private`.

## Kiểm thử tích hợp

MySQL phải đang chạy và `db.properties` phải đúng:

```powershell
.\mvnw.cmd -q test-compile exec:java "-Dexec.mainClass=lab1.test.ChatIntegrationTest" "-Dexec.classpathScope=test"
```

Test mở sáu client, kiểm tra đăng nhập, chat riêng, multicast/broadcast và truyền file UDP.

## Bot demo

```powershell
.\mvnw.cmd -q test-compile exec:java "-Dexec.mainClass=lab1.test.LiveDemoBot" "-Dexec.classpathScope=test" `
  "-Dexec.args=192.168.1.10 5000 An"
```

Thay `192.168.1.10` bằng IPv4 của máy server.

## Lỗi thường gặp

- `Access denied`: chạy lại `setup-windows.ps1` để đồng bộ mật khẩu local.
- `Address already in use`: đóng tiến trình server cũ hoặc kiểm tra cổng 5000-5002.
- Client máy khác không kết nối: kiểm tra IPv4, profile mạng Private và firewall.
- Không nhận multicast/broadcast: bảo đảm hai máy cùng subnet và router không chặn multicast.
