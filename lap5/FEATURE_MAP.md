# Feature Map Lab 5

| Chức năng | Class / method chính |
|---|---|
| Khởi động/dừng server, bind RMI | `ServerRuntimeService.start/stop` |
| Dashboard và trạng thái server | `ServerDashboardView`, `ServerRuntimeService.status` |
| Kết nối client, chạy tác vụ nền | `WarehouseClientService.connect`, `WarehouseClient` |
| Đăng nhập / đăng ký | `WarehouseClientService.login/register`, `WarehouseServiceImpl.login/register` |
| Tìm kiếm và CRUD sản phẩm | `WarehouseClientService.listProducts/createProduct/updateProduct/deleteProduct` |
| Nhập, xuất và lịch sử | `WarehouseClientService.changeInventory/transactionHistory` |
| Nghiệp vụ giao dịch nguồn | `WarehouseServiceImpl` |
| SQL user/product/inventory/lock | `UserRepository`, `ProductRepository`, `InventoryRepository`, `LockRepository` |
| Ghi dữ liệu nhận từ peer | `PeerSyncRepository` |
| RMI nhận đồng bộ hai chiều | `SyncServiceImpl.syncUser/syncCreateProduct/syncUpdateProduct/syncDeleteProduct/syncInventory` |
| Lock, lease, fencing | `LockManager.acquire/heartbeat/release/validate` |
| Kết nối lại coordinator A | `ReconnectingLockService` |
| Heartbeat client mỗi 10 giây | `WarehouseClientService.startHeartbeat` |
| Timeline đồng bộ nội bộ | `SyncActivityLog`, `ServerDashboardView` |
| Giao diện JavaFX client/server | `ClientView`, `ServerDashboardView` |
| Thứ tự ưu tiên cấu hình | `ConfigValues`, `ClientConfig.load`, `ServerConfig.load` |

Mã nguồn nằm dưới `src/main/java/lab5`. Bằng chứng hiện có và phần demo còn thiếu được ghi trong [TESTING.md](TESTING.md).
