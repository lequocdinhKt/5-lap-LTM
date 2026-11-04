# Dữ liệu demo

Mỗi MySQL mới tạo schema `lab5_warehouse` bằng `scripts/schema.sql`. Chỉ chạy `scripts/demo-seed.sql` hoặc `configure-server.ps1 -SeedDemo` cho database demo trống.

Seed chứa tài khoản công khai dành cho bài lab và danh mục sản phẩm mẫu; không chứa secret hoặc dữ liệu thật. A và B phải khởi tạo cùng seed nhưng nằm trên hai MySQL local độc lập.

Không migration database MariaDB cũ. Nếu đã có dữ liệu thử, xóa/tạo lại database demo hoặc bỏ `-SeedDemo`; không chạy seed chồng lên dữ liệu cần giữ.
