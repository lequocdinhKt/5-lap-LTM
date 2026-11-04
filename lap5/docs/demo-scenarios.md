# Kịch bản demo chi tiết

1. Đăng ký tài khoản tại A, đăng nhập tài khoản đó qua B.
2. Thêm một sản phẩm tại A; tìm kiếm và xem chi tiết tại B.
3. Sửa sản phẩm tại B; kiểm tra A nhận đúng version và fencing token.
4. Nhập rồi xuất hàng ở hai chiều; lịch sử và tồn kho hai database phải giống nhau.
5. Hai client tranh sửa cùng sản phẩm; chỉ lease hợp lệ được ghi.
6. Dừng heartbeat, chờ 30 giây rồi xin lease mới; thử fencing token cũ và xác minh bị từ chối.
7. Gửi lại cùng UUID transaction; tồn kho không thay đổi lần hai.
8. Tắt Server B; thao tác ghi tại A phải lỗi và nguồn rollback. Bật lại B và thực hiện một thao tác mới.
9. Thử mã sản phẩm trùng, dữ liệu không hợp lệ, xuất vượt tồn, port bận và database tắt.
10. Lưu ảnh hai server dashboard, hai client và video lỗi peer có chủ đích.
