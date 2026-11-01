# Demo Lab 4

## Chuẩn bị

- Máy A chạy server tại `http://<IP>:8080`; Máy B chạy Mini Browser và curl.
- Đặt HTML, PDF, MP3, MP4 mẫu trong `www`; lưu hash file media trước demo.
- Mở firewall TCP 8080.

## Kịch bản 7 phút

1. GET trang chủ và chỉ request/status/header/body.
2. HEAD PDF, chứng minh có Content-Length nhưng không có body.
3. POST form `/submit`, chỉ server parse và phản hồi dữ liệu đã escape.
4. Mở PDF/MP3/MP4, tải file và so sánh SHA-256.
5. Mở history của browser và quay lại URL trước.
6. Gọi URL không tồn tại, PUT và path traversal để chỉ 404/405/400.

## Câu hỏi giảng viên có thể hỏi

- HTTP chạy trên giao thức transport nào?
- GET và HEAD khác nhau ở response body ra sao?
- `Content-Length` dùng để làm gì?
- POST form được parse ở method nào?
- MIME type được xác định ở đâu?
- Server xử lý nhiều client đồng thời thế nào?
- Code nào chặn `../` và giới hạn request?
- Vì sao project dùng `Connection: close`?

## Dự phòng

Nếu Mini Browser lỗi hiển thị media, dùng nó để gửi/inspect HTTP và dùng file download/hash làm bằng chứng. `curl.exe` là công cụ đối chiếu protocol, không thay client chính.
