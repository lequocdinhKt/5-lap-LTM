# Feature Map Lab 5

| Chức năng | Class / method |
|---|---|
| Khởi động registry và bind | `ServerLauncher.startServer` |
| Kết nối client | `WarehouseClient.connect` |
| Đăng nhập / đăng ký và đồng bộ user | `WarehouseServiceImpl.login/register`, `SyncServiceImpl.syncUser` |
| Tạo / sửa / xóa sản phẩm | `WarehouseServiceImpl.createProduct/updateProduct/deleteProduct` |
| Nhập / xuất / lịch sử | `WarehouseServiceImpl.changeInventory/transactionHistory` |
| Đồng bộ hai chiều | `SyncServiceImpl.syncCreateProduct/syncUpdateProduct/syncDeleteProduct/syncInventory` |
| Lock, lease, fencing | `LockManager.acquire/heartbeat/release/validate` |
| Kết nối lại coordinator | `ReconnectingLockService` |
| Swing UI | `ServerMonitorFrame`, `ConnectionFrame`, `ClientAccessFrame`, `DashboardFrame` |

Mã nguồn nằm dưới `src/main/java/com/warehouse`. Xem [kiểm thử](TESTING.md) để phân biệt bằng chứng local và demo LAN còn chờ.
