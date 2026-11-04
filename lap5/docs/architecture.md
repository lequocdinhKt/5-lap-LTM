# Kiến trúc chi tiết

Server A là lock coordinator duy nhất. Mỗi server có MySQL Server 8.x local riêng, nhận yêu cầu từ client qua `WarehouseService` và gọi `SyncService` peer trực tiếp trong thao tác ghi. Server nguồn chỉ commit sau khi peer xác nhận.

`WarehouseClientService` tạo executor cho RMI và scheduler heartbeat. `ServerRuntimeService` sở hữu connection pool, registry, remote object và status snapshot. JavaFX view không lookup registry, gọi JDBC hay giữ scheduler.

`WarehouseServiceImpl` điều phối transaction nguồn qua `UserRepository`, `ProductRepository` và `InventoryRepository`. `SyncServiceImpl` là RMI adapter, ủy quyền SQL nhận peer cho `PeerSyncRepository`. `LockManager` ủy quyền lưu lease/fencing cho `LockRepository`.

Đồng bộ trực tiếp bảo vệ rollback nguồn khi peer offline nhưng không tạo commit nguyên tử xuyên hai database. Trạng thái peer đã commit rồi mất phản hồi phải được đối soát thủ công.
