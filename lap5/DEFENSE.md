# Câu hỏi bảo vệ Lab 5

## Vì sao dùng ba remote service?

`WarehouseService` phục vụ nghiệp vụ client, `SyncService` chỉ nhận dữ liệu từ peer, còn `LockService` tách lease/fencing để hai client không ghi cạnh tranh vào cùng sản phẩm.

## Vì sao Server A là lock coordinator?

Một coordinator tạo thứ tự fencing token thống nhất. Server B dùng `ReconnectingLockService` chuyển tiếp yêu cầu lock tới A.

## Lease và fencing token khác nhau thế nào?

Lease tự hết hạn nếu client mất heartbeat. Fencing token tăng dần và giúp database từ chối một client cũ dù client đó vẫn giữ token đã hết giá trị.

## Vì sao transaction trùng không làm tăng tồn kho lần hai?

Mỗi `InventoryTransaction` có UUID. Peer khóa dòng sản phẩm rồi kiểm tra UUID đã tồn tại trước khi áp dụng số lượng.

## Vì sao peer offline làm nguồn rollback?

Nguồn chỉ commit sau khi `SyncService` peer xác nhận. Exception hoặc phản hồi thất bại đi qua `TransactionManager` và rollback connection nguồn.

## Đây có phải two-phase commit không?

Không. Peer commit trước khi phản hồi, nên nếu phản hồi bị mất thì nguồn có thể rollback trong khi peer đã commit. Khi đó phải dừng ghi và đối soát thủ công.

## Vì sao UI không gọi RMI trực tiếp?

JavaFX view chỉ gọi `WarehouseClientService`/`ServerRuntimeService`. Service chạy RMI, SQL và heartbeat trên executor riêng để FX thread chỉ cập nhật giao diện.

## Hai database có dùng chung không?

Không. Hai máy thật cài hai MySQL local độc lập, chỉ giống schema `lab5_warehouse`; dữ liệu đi qua RMI.

## Cấu hình được ưu tiên thế nào?

Tham số dòng lệnh cao nhất, sau đó biến môi trường `LAB5_*`, properties cục bộ bị ignore và cuối cùng là default local.
