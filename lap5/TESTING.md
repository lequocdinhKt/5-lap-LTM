# Kiểm thử Lab 5

## Bằng chứng ngày 03/11/2026

- Biên dịch trực tiếp bằng JDK 17 và chạy JUnit Platform: 49/49 test thành công.
- MariaDB 10.4.32: 43 kiểm tra database/RMI thành công với hai database tạm trên cùng máy. Đã thử schema mới và bản sao cấu trúc database đang dùng.
- Bao gồm đăng nhập, đăng ký đồng bộ UUID, CRUD, lock cạnh tranh, nhập/xuất hai chiều, giao dịch trùng, rollback khi peer offline và từ chối xuất quá tồn kho.
- `scripts/schema.sql` đã tạo bảng và chạy seed thành công; các database thử đã được xóa, dữ liệu lab gốc giữ nguyên.
- Test JDBC/RMI bổ sung dùng harness tạm trong môi trường kiểm tra; harness này không đóng gói trong repo. Unit test nằm trong `src/test/java`.

- Bản nhập trong repo đích đã chạy Maven `clean verify` bằng JDK 21 thành công: 49 test, đóng gói đủ JAR thường và JAR `-all`.

## Chạy lại

Trong `lap5`, chạy `.\mvnw.cmd clean verify` với JDK 21 hoặc `mvn clean verify` với JDK 17 trở lên. Lần đầu cần Internet để tải Maven/dependency.

## Còn chờ nghiệm thu

- Hai máy thật và hai VM: IP quảng bá trong stub, firewall 1099–1102, độ trễ và mất kết nối LAN.
- Ảnh/video UI, demo giảng viên và đối chiếu toàn bộ REQUIREMENTS/DEFINITION_OF_DONE.
- Chưa khẳng định đạt JavaFX/Docker theo chuẩn chung; bản hiện tại dùng Swing/MariaDB.
- Không có bảo đảm commit nguyên tử trên hai database; xem PROTOCOL.md.
