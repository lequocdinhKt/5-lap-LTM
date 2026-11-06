# Contract RMI Lab 5

- Registry TCP `1099`, binding `warehouse`, `sync`, `lock`.
- Warehouse object TCP `1100`, Sync object TCP `1101`, Lock object TCP `1102`.
- `WarehouseService`: login/register, list/create/update/delete product, changeInventory, transactionHistory, isPeerAvailable.
- `LockService`: acquire, heartbeat, release, validate; lease mặc định 30 giây, heartbeat client 10 giây.
- UI chỉ mở dialog Sửa/Nhập kho/Xuất kho sau khi `LockService.acquire` trả về lease hợp lệ; nếu sản phẩm đang bị client khác giữ lease, UI hiển thị lỗi và không mở dialog.
- `SyncService`: ping, syncUser, syncCreateProduct, syncUpdateProduct, syncDeleteProduct, syncInventory.
- DTO tuần tự hóa: `Product`, `InventoryTransaction`, `ProductInput`, `LockLease`, `LoginResult`, `RegistrationResult`.
- Hai server và client phải dùng cùng một build `lab5.*`; không tương thích nhị phân với JAR `com.warehouse.*` cũ.

Quy tắc ghi:

1. Nguồn mở transaction và ghi dữ liệu cục bộ.
2. Nguồn gọi peer trực tiếp và chờ `true`.
3. Peer ghi MySQL cục bộ rồi phản hồi.
4. Nguồn chỉ commit sau phản hồi thành công; lỗi peer làm nguồn rollback.

CREATE lại cùng UUID/mã được xác nhận mà không ghi đè; mã trùng với UUID khác bị từ chối. Inventory khóa dòng, kiểm tra UUID giao dịch trước khi so tồn kho; transaction trùng không làm tồn kho thay đổi lần hai. Fencing token cũ và thứ tự tồn kho sai bị từ chối.

Đã loại bỏ `applyEvent`, `SyncEvent`, JSON codec và outbox cũ. Không có retry/outbox tự hội tụ. Đây không phải 2PC; mất phản hồi sau khi peer commit cần đối soát thủ công.
