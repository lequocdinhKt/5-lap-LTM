# Xử lý lỗi Lab 5 — khung

## Client lookup thất bại

Kiểm tra IP/registry port, service name, firewall và registry đã bind object. Không quảng bá `localhost` cho client ở máy khác.

## Kết nối registry được nhưng gọi method lỗi

RMI có thể dùng port remote object khác registry. Chốt port cố định hoặc mở đúng firewall; kiểm tra `java.rmi.server.hostname` của từng server.

## Serialization lỗi

Client/server phải dùng DTO tương thích, cùng `serialVersionUID`; không truyền JavaFX control, socket hoặc database connection qua RMI.

## Dữ liệu không đồng bộ

So sánh log/version ở A và B, kiểm tra peer address, retry queue và conflict policy. Không sửa trực tiếp storage để che lỗi replication.

## VM không thấy máy thật

Kiểm tra NAT/bridged/host-only theo topology được chọn, ping nếu được phép và `Test-NetConnection` tới từng port. Ghi cấu hình cuối cùng vào `DEMO_TOPOLOGY.md`.

Nội dung sẽ được cập nhật bằng lỗi thực tế sau khi code Lab 5 được tích hợp.
