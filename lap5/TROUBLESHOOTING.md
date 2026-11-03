# Xử lý lỗi Lab 5

- Thiếu JAR: build trong `lap5` bằng `.\mvnw.cmd clean verify`. Script cần `target/Lap8_LTM-1.0.0-SNAPSHOT-all.jar`.
- Thiếu properties: copy mẫu từ `src/main/resources` ra thư mục `lap5`, bỏ đuôi `.example`, sửa IP và thông tin database.
- Database chưa có bảng: trên database mới chạy `scripts/schema.sql` rồi `scripts/demo-seed.sql`. Không seed lại database đã có dữ liệu.
- Registry kết nối được nhưng gọi service/lock lỗi: mở cả 1099, 1100, 1101, 1102; kiểm tra IP trong stub và coordinator A.
- Không ghi/đăng ký được khi peer tắt: hành vi chủ động của bản này; chạy cả hai server và dùng cùng JAR.
- Dữ liệu lệch sau mất kết nối: dừng demo ghi, đối soát dữ liệu hai database; chưa có two-phase commit hoặc cơ chế tự sửa lệch.

Xem phần xử lý lỗi trong [README](README.md). Docker/setup/firewall chung của Lab 1–4 chưa cấu hình Lab 5.
