# Kiến trúc Lab 1

```mermaid
flowchart LR
    UI[ChatFxApp] --> C[ClientConnection]
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
- `ClientConnection`: API client TCP, signaling file và listener nhận sự kiện.
- `ClientUdpText`: nhận multicast/broadcast text.
- `ClientUdpFiles`: chia/ghép gói file và ACK.
- `ChatServer` + `ClientHandler`: nhận kết nối, xác thực và điều phối.
- `ClientRegistry`/`GroupRegistry`: trạng thái online và nhóm trong RAM.
- `UdpTextBroadcaster`/`UdpFileRelay`: phát text và relay file.
- `MySqlDB`: tài khoản người dùng.

## Luồng chính

1. Client mở TCP và đăng nhập; server đăng ký phiên.
2. Server phát danh sách online và catalog nhóm.
3. Chat riêng đi qua TCP; chat nhóm/tất cả được server phát qua UDP.
4. File được báo hiệu bằng TCP, truyền chunk qua UDP và xác nhận ACK/retry.

## Cấu trúc Maven

Source chính nằm tại `src/main/java/lab1`; integration test và bot demo nằm tại `src/test/java/lab1/test`. Các package `client`, `server`, `common`, `db` được giữ vì phản ánh trực tiếp kiến trúc Chat Client–Server.
