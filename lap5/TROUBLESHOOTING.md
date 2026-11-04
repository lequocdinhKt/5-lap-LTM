# Xử lý lỗi Lab 5

- Maven/JDK: chạy `java -version` và `.\mvnw.cmd -version`; nghiệm thu bằng JDK 21, project compile release 17.
- Thiếu cấu hình server: chạy `.\configure-server.ps1 -Role A|B -LocalHost <IP> -PeerHost <IP>`. `server.properties` bị ignore.
- MySQL chưa sẵn sàng: kiểm tra Windows service MySQL, port local và tài khoản trong `.env`, rồi cấu hình lại. Không seed lại database có dữ liệu.
- Registry gọi được nhưng remote object lỗi: mở đủ TCP `1099–1102`, kiểm tra `server.host` là IP LAN của đúng card mạng và dùng cùng build trên mọi node.
- Peer/lock đỏ: chạy Server A trước; từ B kiểm tra `Test-NetConnection <IP-A> -Port 1099`.
- Ghi thất bại khi peer tắt: đây là hành vi chủ động để nguồn rollback, không phải lỗi UI.
- JavaFX không phản hồi: thu log console; mọi RMI/SQL đã chạy qua service nền, không gọi trực tiếp từ FX thread.
- Dữ liệu lệch sau mất phản hồi: dừng thao tác ghi, đối soát hai MySQL; hệ thống không có 2PC hoặc tự sửa lệch.
- Integration script lỗi ngay: kiểm tra `mysql.exe`, Windows service MySQL và `.env` root do `setup-windows.ps1` tạo.
