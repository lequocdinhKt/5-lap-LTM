# Contract RMI Lab 5

- Registry TCP `1099`: tên `warehouse`, `lock`, `sync` (xem `RmiNames`).
- Warehouse service TCP `1100`; Sync service TCP `1101`; Lock TCP `1102` trên cả A/B. Port object được tính từ `rmi.service.port`.
- Server A là coordinator khóa duy nhất. Server B dùng `ReconnectingLockService` chuyển tiếp tới A.
- `java.rmi.server.hostname` lấy từ IP LAN đã chọn. Hai server dùng database độc lập.
- `WarehouseService`: login/register, list/create/update/delete product, changeInventory, transactionHistory, isPeerAvailable.
- `LockService`: acquire, heartbeat, release, validate; lease mặc định 30 giây, client heartbeat mỗi 10 giây.
- `SyncService`: ping, syncUser, syncCreateProduct, syncUpdateProduct, syncDeleteProduct, syncInventory. DTO truyền qua RMI triển khai Serializable.
- Ghi cục bộ trong transaction, gọi peer, chỉ commit nguồn sau khi peer xác nhận; lỗi peer thì rollback nguồn. Không có outbox/retry ghi tự động.
- CREATE trùng mã khác UUID bị từ chối; CREATE lại cùng UUID/mã được xác nhận mà không ghi đè.
- Inventory khóa dòng trước khi kiểm tra UUID giao dịch; giao dịch đã nhận không đổi lại tồn kho. Giao dịch cũ/sai số lượng trước bị từ chối.
- `applyEvent` cũ trả false, không xác nhận giả rằng đã áp dụng event.

Giới hạn: đây không phải two-phase commit. Nếu peer commit nhưng nguồn lỗi commit hoặc mất phản hồi, dữ liệu có thể lệch; cần đối soát thủ công trước khi tiếp tục. Cùng một JAR mới phải được dùng trên cả hai server/client.
