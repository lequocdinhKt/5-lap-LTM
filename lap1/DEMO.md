# Demo Lab 1

## Chuẩn bị

- Máy A chạy MySQL, server và 2 client; Máy B chạy 3 client.
- Chạy firewall ở root; dùng cùng Wi-Fi/LAN.
- Chuẩn bị ảnh, audio/video ngắn dưới 20 MB và 5 tài khoản demo.

## Kịch bản 7 phút

1. Mở server, chỉ TCP/UDP ports và danh sách client trống.
2. Kết nối 5 client; chỉ danh sách online cập nhật thời gian thực.
3. Client A gửi riêng cho B, giải thích TCP bảo đảm thứ tự.
4. Tạo nhóm ba người và gửi multicast; client ngoài nhóm không nhận.
5. Gửi “Tất cả” bằng broadcast.
6. Gửi emoji, ảnh và video; chỉ tiến trình ACK/retry.
7. Tắt một client, quan sát cập nhật online và các client còn lại tiếp tục.

## Demo dự phòng

Nếu LAN chặn multicast/broadcast, demo toàn bộ trên một máy qua loopback, sau đó dùng log/packet flow để giải thích LAN. Không đổi protocol ngay trước buổi bảo vệ.

## Câu hỏi giảng viên có thể hỏi

- Vì sao chat riêng dùng TCP còn nhóm dùng UDP?
- Broadcast và multicast khác nhau ở người nhận thế nào?
- UDP mất gói thì file có hỏng không? ACK/retry nằm ở đâu?
- Một client được server quản lý bằng thread/class nào?
- Nhóm được lưu ở đâu và mất khi nào?
- Vì sao file giới hạn 20 MB và chunk 4 KB?
- Chức năng gửi riêng bắt đầu từ method nào?

## Bằng chứng cần lưu

Lưu ảnh server + 5 client, ảnh multicast/broadcast, ảnh truyền media và video demo vào `evidence/` khi giai đoạn UI hoàn tất; không commit dữ liệu cá nhân.
