# Kiến trúc Lab 1

```mermaid
flowchart LR
    UI[ChatFxApp] --> SVC[ChatService]
    SVC --> C[ClientConnection]
    C -- TCP 5000 --> H[ClientHandler]
    H --> S[ChatServer]
    S --> DB[(MySqlDB)]
    S --> CR[ClientRegistry]
    S --> GR[GroupRegistry]
    S -- UDP 5002 --> UT[ClientUdpText]
    C --> UF[ClientUdpFiles]
    UF -- UDP 5001 --> R[UdpFileRelay]
    R --> UF
```

## Thành phần

- `ChatFxApp`: giao diện client, lựa chọn người nhận/nhóm và hiển thị sự kiện.
- `ChatService`: cửa vào nghiệp vụ của UI; sở hữu kết nối, chuyển lệnh xuống network và chuyển sự kiện về UI.
- `ClientConnection`: API client TCP, signaling file và listener nhận sự kiện.
- `ClientUdpText`: nhận multicast/broadcast text.
- `ClientUdpFiles`: chia/ghép gói file và ACK.
- `ChatServer` + `ClientHandler`: nhận kết nối, xác thực và điều phối.
- `ClientRegistry`/`GroupRegistry`: trạng thái online và nhóm trong RAM.
- `UdpTextBroadcaster`/`UdpFileRelay`: phát text và relay file.
- `MySqlDB`: tài khoản người dùng.

## Luồng chính

1. UI gọi `ChatService.connect()`; service mở `ClientConnection`, đăng nhập và đăng ký listener.
2. Server phát danh sách online và catalog nhóm.
3. Chat riêng đi qua TCP; chat nhóm/tất cả được server phát qua UDP.
4. File được báo hiệu bằng TCP, truyền chunk qua UDP và xác nhận ACK/retry.

## Cấu trúc Maven

Source chính nằm tại `src/main/java/lab1`; integration test và bot demo nằm tại `src/test/java/lab1/test`. Package `service` là cửa vào của UI; các package `client`, `server`, `common`, `db` giữ trách nhiệm giao thức và dữ liệu hiện có.
