# Quy tắc Lab 5 — RMI

- Lab 5 là Maven project độc lập, chạy trên Windows bằng JDK 21 và Maven Wrapper; mức bytecode là Java 17.
- Giữ package gốc `lab5.*`, JavaFX 21, MySQL Server 8.x local và đồng bộ trực tiếp hai chiều qua Java RMI.
- UI chỉ gọi lớp trong `lab5.service`; SQL chỉ nằm trong `lab5.db`; RMI adapter không chứa JavaFX type.
- Server A và Server B dùng hai MySQL local với data directory độc lập, không dùng storage chung để giả lập đồng bộ.
- Giữ lock lease 30 giây, heartbeat 10 giây, fencing token, idempotency và rollback nguồn khi peer lỗi.
- Cấu hình ưu tiên: tham số dòng lệnh, biến môi trường `LAB5_*`, properties cục bộ, rồi default local.
- Không commit `server.properties`, mật khẩu, IP cá nhân hoặc dữ liệu database thật.
- Mỗi thay đổi chức năng phải cập nhật Feature Map, protocol, kiểm thử hoặc demo liên quan.
