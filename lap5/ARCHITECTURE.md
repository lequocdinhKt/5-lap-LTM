# Kiến trúc Lab 5 — dự kiến

```mermaid
flowchart LR
    C1[VM Client 1] --> RA[RMI Registry A]
    C2[VM Client 2] --> RB[RMI Registry B]
    RA --> SA[Server A / Remote Service]
    RB --> SB[Server B / Remote Service]
    SA <--> SYNC[Replication Service]
    SYNC <--> SB
    SA --> DA[(Storage A)]
    SB --> DB[(Storage B)]
```

## Ranh giới dự kiến

- `remote`: remote interface, DTO serializable và exception contract.
- `server`: implementation, registry bootstrap và lifecycle.
- `service`: nghiệp vụ, version/xung đột và replication.
- `storage`: dữ liệu độc lập của từng server.
- `client`/`ui`: lookup stub, gọi remote và trạng thái kết nối.

Tên package/class cuối cùng phải theo code thật. Không xây abstraction trước khi review code được push.

## Luồng cần giải thích

Client lookup registry → nhận stub → gọi remote method → server validate/lưu local → replicate sang peer → trả kết quả/version → client cập nhật UI.
