# Demo Lab 2

## Chuẩn bị

- Máy A: MySQL + FTP Server; Máy A/B mỗi máy mở 1–2 client.
- Tài khoản mẫu `alice/123456`, `bob/123456` và file nhị phân nhỏ.
- Mở firewall TCP `2121`, `30000-30020`.

## Kịch bản 7 phút

1. Đăng nhập hai user, chỉ hai home độc lập.
2. Upload file bằng client đi kèm; quan sát `PASV`, `STOR`, `150`, `226` trong log.
3. Download sang thư mục khác và so sánh hash.
4. Tạo/đổi/xóa thư mục và thử `..` để chứng minh không thoát home.
5. Mở bảng `transfer_logs` trong MySQL.
6. Kết nối FileZilla thật để chứng minh server nói FTP tương thích.

## Câu hỏi giảng viên có thể hỏi

- Vì sao FTP cần hai kết nối TCP?
- PASV và PORT khác nhau về bên mở data connection thế nào?
- Vì sao data connection đóng sau mỗi LIST/RETR/STOR?
- Reply `150` và `226` biểu thị gì?
- Code nào ngăn `../` thoát khỏi user home?
- Upload bắt đầu ở method nào phía client và server?
- Nhiều client được xử lý đồng thời ra sao?

## Dự phòng

Nếu FileZilla bị cấu hình TLS, chọn plain FTP. Nếu firewall chặn PASV, demo localhost trước và dùng `Test-NetConnection` kiểm tra từng port.
