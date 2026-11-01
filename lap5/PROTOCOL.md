# Contract RMI Lab 5 — chờ chốt

## Thành phần cần ghi rõ

- Registry name và registry port của Server A/B.
- Hostname mà RMI stub quảng bá; trên nhiều máy không được để loopback.
- Remote interface và từng remote method: input, output, exception và tính idempotent.
- DTO triển khai `Serializable` và `serialVersionUID`.
- Remote object port cố định nếu cần cấu hình Windows Firewall.
- Peer-to-peer replication call hoặc cơ chế pull/push.

## Contract version dữ liệu đề xuất

Mỗi record nên có `id`, `version`, `updatedAt`, `updatedBy` và trạng thái xóa nếu cần. Đây chỉ là đề xuất; phải đối chiếu code thật trước khi áp dụng.

## Lỗi cần phân biệt

- `RemoteException`: lỗi transport/server.
- Lỗi validation nghiệp vụ.
- Conflict do version cũ.
- Peer server tạm offline.
- Serialization/class version không tương thích.

Port và method cụ thể để trống có chủ đích, tránh tài liệu sai với phần code sẽ được push.
