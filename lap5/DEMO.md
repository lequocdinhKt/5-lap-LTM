# Demo Lab 5

Theo đề: Server A/MariaDB A trên máy thật A; Server B/MariaDB B trên máy thật B; hai client trên hai VM. Bản README cũng hướng dẫn phương án mỗi VM chạy cả server/database/client; cần chọn đúng topology giảng viên yêu cầu khi nghiệm thu.

1. Tạo schema và seed riêng trên A/B theo README; sửa IP LAN và cấu hình coordinator A.
2. Build cùng JAR, chạy Server A trước rồi Server B. Mở client A/B bằng script trong `scripts`.
3. Tạo sản phẩm trên A, kiểm tra B; cập nhật trên B, kiểm tra A.
4. Nhập/xuất và xem tồn kho, lịch sử trên cả hai client; thử xuất vượt tồn.
5. Hai client cùng sửa sản phẩm: chỉ client giữ lease được ghi.
6. Tắt B: ghi/đăng ký ở A phải thất bại và rollback. Bật lại B, thử thao tác mới.
7. Không trình bày rằng có hàng đợi tự hội tụ: mất phản hồi sau commit peer vẫn cần đối soát thủ công.

Demo LAN và ảnh/video chưa thực hiện. Xem [hướng dẫn chạy](README.md), [kịch bản chi tiết](docs/demo-scenarios.md) và [bằng chứng test](TESTING.md).
