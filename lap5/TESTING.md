# Kiểm thử Lab 5 — kế hoạch

## Unit test

- Validation và thao tác dữ liệu.
- So sánh version/chính sách conflict.
- Queue/retry replication nếu có.

## Integration test

- Khởi động hai registry/server trên port test tách biệt.
- Ghi A → đọc B; ghi B → đọc A nếu hai chiều.
- Dữ liệu Unicode và object serialization.
- Một peer offline, restart và hội tụ.
- Concurrent update và duplicate retry/idempotency.

## Test bốn máy

- Stub quảng bá đúng IPv4, không phải `127.0.0.1`.
- Windows Firewall mở cả registry port và remote object port.
- VM network mode cho phép tới hai máy thật.
- Server/storage độc lập và clock/timezone nhất quán.

Lệnh build/test sẽ được điền khi có `pom.xml` và code thật.
