# Yêu cầu Lab 5 — RMI

## Phạm vi đã triển khai

- JavaFX client: kết nối, đăng nhập/đăng ký, tìm kiếm, bảng/chi tiết sản phẩm, CRUD, nhập/xuất và lịch sử.
- JavaFX server: trạng thái MySQL, RMI local, peer, lock coordinator và timeline đồng bộ.
- Hai server đồng bộ trực tiếp hai chiều qua Java RMI; mỗi server có MySQL Server 8.x local riêng.
- Lock lease/fencing, heartbeat, idempotency giao dịch và rollback nguồn khi peer offline.
- Cấu hình không chứa secret trong mã nguồn; hỗ trợ tham số, `LAB5_*`, properties cục bộ và default.
- Chạy Windows bằng Maven Wrapper; Maven compile release 17, môi trường nghiệm thu JDK 21.

## Topology nghiệm thu

- Máy thật A: Server A, MySQL local A và lock coordinator.
- Máy thật B: Server B, MySQL local B.
- VM A: Client A kết nối Server A.
- VM B: Client B kết nối Server B.
- Mở TCP `1099–1102` trên hai máy server; không dùng `localhost` giữa các máy.

## Tiêu chí hoàn thành

- Build/test Maven thành công trên JDK 21.
- Integration test hai schema/port local thành công.
- Demo A→B và B→A cho đăng ký, CRUD, tồn kho và lịch sử.
- Chứng minh tranh lock, hết lease, fencing cũ, transaction trùng và rollback khi peer offline.
- Có ảnh hai dashboard, hai client và video lỗi có chủ đích.
- Nêu đúng giới hạn đồng bộ trực tiếp, không quảng bá là 2PC hoặc tự hội tụ.
