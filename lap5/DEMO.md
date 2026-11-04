# Demo Lab 5

Topology bắt buộc: Server A/MySQL A trên máy thật A; Server B/MySQL B trên máy thật B; Client A và B trên hai VM. Hai database cùng tên schema nhưng thuộc hai MySQL local độc lập.

## Kịch bản ngắn

Khởi động A/B, thêm sản phẩm ở A và xem ở B, nhập kho ở B và xem lịch sử ở A, sau đó tắt B để chứng minh thao tác ghi tại A thất bại và rollback.

## Kịch bản đầy đủ

1. Trên mỗi máy server, chạy `configure-server.ps1` với Role, LocalHost, PeerHost; chỉ dùng `-SeedDemo` cho database demo mới.
2. Mở firewall TCP `1099–1102`; build cùng commit/build trên cả bốn node.
3. Chạy Server A trước, Server B sau; xác nhận database, local RMI, peer và lock đều xanh.
4. Chạy hai client bằng `run-client.ps1`, đăng nhập/đăng ký và kiểm tra thông báo tiếng Việt.
5. Tạo sản phẩm ở A và đọc ở B; cập nhật ở B và đọc ở A.
6. Nhập/xuất, so tồn kho và lịch sử trên hai client; thử xuất vượt tồn.
7. Hai client cùng sửa một sản phẩm; chỉ client giữ lease được ghi. Tắt client giữ lock, chờ quá 30 giây rồi xin lại.
8. Gửi lại cùng transaction và thử fencing token cũ; dữ liệu không được đổi lần hai.
9. Tắt B, ghi ở A phải thất bại và rollback nguồn; bật B rồi thử thao tác mới.
10. Giải thích rằng không có outbox/2PC: nếu peer commit nhưng mất phản hồi, phải dừng ghi và đối soát thủ công.

## Phương án dự phòng

- Nếu một VM lỗi, chạy hai client trên một máy nhưng vẫn giữ hai server/MySQL trên hai máy thật; nói rõ đây chỉ là phương án dự phòng client.
- Nếu LAN chặn RMI, dùng `Test-NetConnection` cho `1099–1102`, quay lại IP LAN đúng rồi khởi động lại server để stub quảng bá đúng host.
- Nếu MySQL local lỗi, trình bày log integration đã lưu trong `TESTING.md`; không đổi sang storage chung hoặc bỏ qua rollback.
- Không seed lại database đang có dữ liệu. Dùng database demo mới và cùng build trên bốn node.

Demo bốn máy, ảnh và video vẫn chưa có bằng chứng trong repo.
