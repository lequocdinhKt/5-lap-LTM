# Giao thức Lab 1

## Kênh và cổng

| Kênh | Cổng mặc định | Vai trò |
|---|---:|---|
| TCP | 5000 | phiên, xác thực, chat riêng, nhóm, signaling file |
| UDP unicast | 5001 | chunk file và ACK |
| UDP multicast/broadcast | 5002 | text nhóm và “Tất cả” |

## TCP framing

`DataInputStream`/`DataOutputStream` truyền một frame name từ `Protocol`, sau đó là các field theo đúng thứ tự của frame. Danh sách được ghi bằng số lượng trước rồi từng chuỗi UTF-8. Các frame thực tế phải xem tại `src/main/java/lab1/common/Protocol.java`.

Luồng phiên rút gọn:

```text
Client -> LOGIN|REGISTER, username, password
Server -> LOGIN_OK hoặc LOGIN_ERROR
Server -> USER_LIST, count, usernames...
Client -> DIRECT_TEXT, target, text
Server -> TEXT, sender, mode, targets, timestamp, text
```

## UDP text

Packet có magic/version, loại multicast hoặc broadcast, group id, sender, timestamp và text. `Protocol.encodeUdpText()`/`decodeUdpText()` là nguồn sự thật.

## UDP file

1. Client gửi offer qua TCP.
2. Server trả transfer id/UDP port và báo receiver chuẩn bị.
3. Sender gửi chunk 4 KB tới server; server relay tới receiver.
4. Mỗi chặng dùng ACK theo `transferId + sequence`; tối đa 8 lần thử mỗi chunk.
5. Server báo `done` hoặc `fail` qua TCP.

## Giới hạn/lỗi

- Text và số client bị giới hạn trong `Protocol` để tránh packet/frame bất thường.
- File tối đa 20 MB; tên file được làm sạch trước khi ghi.
- UDP multicast/broadcast best effort; file bổ sung ACK/retry ở tầng ứng dụng.
