# Kiến thức Lập trình Mạng - TCP, UDP, Multicast, Broadcast

Tài liệu này giải thích các khái niệm mạng cốt lõi mà Lab 1 sử dụng, kèm **vị trí chính xác trong mã nguồn** để bạn đọc code song song.

## 1. Mô hình TCP (Transmission Control Protocol)

### 1.1. Lý thuyết

TCP là giao thức **hướng kết nối** (connection-oriented), hoạt động trên tầng giao vận (transport layer). Đặc điểm:

- **Ba bước bắt tay** (three-way handshake): SYN → SYN-ACK → ACK trước khi truyền dữ liệu.
- **Đảm bảo thứ tự**: byte đến đúng thứ tự đã gửi.
- **Đảm bảo không mất**: tự động truyền lại gói bị mất.
- **Kiểm soát luồng** (flow control) và **kiểm soát tắc nghẽn** (congestion control).
- **Hai chiều** (full-duplex): gửi và nhận đồng thời.
- Phù hợp: đăng nhập, chat riêng, điều khiển phiên, dữ liệu phải đúng 100%.

### 1.2. TCP trong Lab 1

| Khái niệm | File | Dòng / Ghi chú |
|-----------|------|----------------|
| Mở cổng TCP server | [`ChatServer.java`](src/main/java/lab1/server/ChatServer.java:73) | `new ServerSocket(tcpPort)` |
| Chấp nhận kết nối | [`ChatServer.java`](src/main/java/lab1/server/ChatServer.java:87) | `serverSocket.accept()` trong vòng lặp |
| Tắt Nagle (giảm trễ) | [`ChatServer.java`](src/main/java/lab1/server/ChatServer.java:88) | `socket.setTcpNoDelay(true)` |
| Thread pool cho client | [`ChatServer.java`](src/main/java/lab1/server/ChatServer.java:38) | `Executors.newFixedThreadPool(Protocol.MAX_CLIENTS)` |
| Xử lý từng client | [`ClientHandler.java`](src/main/java/lab1/server/ClientHandler.java) | Đọc/ghi `DataInputStream`/`DataOutputStream` |
| Kết nối client | [`ClientConnection.java`](src/main/java/lab1/client/ClientConnection.java) | `Socket` + luồng đọc riêng |
| Khung lệnh dạng chuỗi | [`Protocol.java`](src/main/java/lab1/common/Protocol.java:26) | `LOGIN`, `REGISTER`, `TEXT`, `CREATE_GROUP`... |
| Ghi/đọc danh sách chuỗi | [`Protocol.java`](src/main/java/lab1/common/Protocol.java:63) | `writeStringList` / `readStringList` |

**Luồng TCP điển hình:**

```text
Client                          Server
  │  LOGIN|username|password  →  │
  │  ←  LOGIN_OK               │
  │  ←  USER_LIST|...          │
  │  ←  GROUP_CATALOG|...      │
  │  TEXT|DIRECT|target|nội dung → │
  │  ←  TEXT|... (tới người nhận) │
```

Mỗi dòng là một khung `DataOutputStream.writeUTF(...)` phân tách bằng ký tự `|`, đọc bằng `readUTF()`.

### 1.3. Vì sao chat riêng dùng TCP?

- Tin nhắn riêng **không được phép mất** hoặc sai thứ tự.
- Kết nối TCP đã thiết lập sẵn cho mỗi client, gửi rất nhanh.
- Server tra cứu người nhận trong [`ClientRegistry`](src/main/java/lab1/server/ClientRegistry.java) rồi ghi trực tiếp xuống socket của họ.

Xem hàm [`ChatServer.sendDirectText()`](src/main/java/lab1/server/ChatServer.java:151).

---

## 2. Mô hình UDP (User Datagram Protocol)

### 2.1. Lý thuyết

UDP là giao thức **không kết nối** (connectionless), cũng ở tầng giao vận. Đặc điểm:

- **Không bắt tay**: gửi thẳng datagram, không cần thiết lập kênh.
- **Không đảm bảo**: có thể mất, trùng, sai thứ tự.
- **Không kiểm soát tắc nghẽn**: gửi theo khả năng ứng dụng.
- **Nhẹ, trễ thấp**: header 8 byte so với 20-60 byte của TCP.
- Phù hợp: streaming, game, multicast/broadcast, dữ liệu chấp nhận mất.

### 2.2. UDP trong Lab 1

Lab 1 dùng UDP cho **ba mục đích khác nhau**, mỗi mục đích một cổng:

| Cổng mặc định | Mục đích | File chính |
|---------------|----------|------------|
| `5002/udp` | Tin nhắn text nhóm (multicast) và "Tất cả" (broadcast) | [`UdpTextBroadcaster.java`](src/main/java/lab1/server/UdpTextBroadcaster.java), [`ClientUdpText.java`](src/main/java/lab1/client/ClientUdpText.java) |
| `5001/udp` | Truyền file/audio/video (unicast có ACK/retry) | [`UdpFileRelay.java`](src/main/java/lab1/server/UdpFileRelay.java), [`ClientUdpFiles.java`](src/main/java/lab1/client/ClientUdpFiles.java) |

Hằng số giao thức tại [`Protocol.java`](src/main/java/lab1/common/Protocol.java:14):

```java
DEFAULT_TCP_PORT      = 5000
DEFAULT_UDP_FILE_PORT = 5001
DEFAULT_UDP_TEXT_PORT = 5002
UDP_CHUNK_SIZE        = 4 * 1024        // 4 KB mỗi chunk
UDP_ACK_TIMEOUT_MS    = 500             // chờ ACK 500 ms
UDP_MAX_RETRIES       = 8               // gửi lại tối đa 8 lần
MAX_FILE_SIZE         = 20 MB
MAX_UDP_TEXT_PACKET   = 8 KB
```

### 2.3. Vì sao chat nhóm dùng UDP?

- Gửi một lần tới nhiều người (multicast) tiết kiệm băng thông hơn gửi N lần TCP.
- Tin nhắn chat chấp nhận mất gói (best effort) — đúng bản chất UDP.
- Không cần thiết lập kết nối cho từng người nhận.

Xem hàm [`ChatServer.sendGroupText()`](src/main/java/lab1/server/ChatServer.java:158).

---

## 3. UDP Multicast

### 3.1. Lý thuyết

Multicast là gửi **một bản duy nhất** tới một **nhóm máy** đã đăng ký (join) một địa chỉ multicast lớp D (`224.0.0.0` – `239.255.255.255`).

- Chỉ máy đã `joinGroup` mới nhận.
- Một gói đi trên mạng, router/switch nhân bản tới các nhánh cần thiết.
- Không cần biết địa chỉ từng thành viên.
- Phù hợp: chat nhóm, streaming tới nhóm người xem.

### 3.2. Multicast trong Lab 1

| Khái niệm | File | Dòng / Ghi chú |
|-----------|------|----------------|
| Cấp địa chỉ multicast cho nhóm | [`GroupRegistry.java`](src/main/java/lab1/server/GroupRegistry.java) | Mỗi nhóm một `239.10.x.x` |
| Gửi multicast | [`UdpTextBroadcaster.multicast()`](src/main/java/lab1/server/UdpTextBroadcaster.java) | `MulticastSocket.send()` |
| Client join nhóm | [`ClientUdpText.java`](src/main/java/lab1/client/ClientUdpText.java) | `MulticastSocket.joinGroup()` |
| Mã gói tin multicast | [`Protocol.java`](src/main/java/lab1/common/Protocol.java:56) | `UDP_TEXT_MULTICAST = 1` |
| Mã magic gói text | [`Protocol.java`](src/main/java/lab1/common/Protocol.java:54) | `UDP_TEXT_MAGIC = 0x4C315458` ("L1TX") |

**Luồng multicast:**

```text
An gửi tin nhóm "Nhóm Java"
        │
        ▼
Server nhận qua TCP (GROUP_TEXT)
        │
        ▼
UdpTextBroadcaster.multicast(239.10.1.5, ...)
        │
        ├──► Binh (đã join 239.10.1.5)  ✓ nhận
        └──► Chi  (không join)          ✗ không nhận
```

---

## 4. UDP Broadcast

### 4.1. Lý thuyết

Broadcast gửi tới **mọi máy trong cùng mạng LAN** (subnet), dùng địa chỉ broadcast của interface (ví dụ `192.168.1.255`).

- Không cần đăng ký; mọi máy trong LAN đều nhận ở tầng card mạng.
- Không bị router chuyển qua mạng khác (broadcast không định tuyến).
- Phù hợp: thông báo trong LAN, nhóm "Tất cả".

### 4.2. Broadcast trong Lab 1

| Khái niệm | File | Dòng / Ghi chú |
|-----------|------|----------------|
| Gửi broadcast | [`UdpTextBroadcaster.broadcast()`](src/main/java/lab1/server/UdpTextBroadcaster.java) | Gửi tới địa chỉ broadcast LAN |
| Thêm loopback cho máy đơn | [`UdpTextBroadcaster.java`](src/main/java/lab1/server/UdpTextBroadcaster.java) | Gửi thêm `127.0.0.1` để demo 1 máy |
| Mã gói tin broadcast | [`Protocol.java`](src/main/java/lab1/common/Protocol.java:57) | `UDP_TEXT_BROADCAST = 2` |
| Nhóm hệ thống "Tất cả" | [`GroupRegistry.java`](src/main/java/lab1/server/GroupRegistry.java) | `systemGroup = true` |

**Luồng broadcast:**

```text
An gửi tin vào nhóm "Tất cả"
        │
        ▼
Server nhận qua TCP (GROUP_TEXT với groupId = "all")
        │
        ▼
UdpTextBroadcaster.broadcast(...)
        │
        ├──► 192.168.1.255:5002  (mọi máy LAN)
        └──► 127.0.0.1:5002      (máy đơn)
```

---

## 5. UDP Unicast có ACK/Retry (truyền file)

### 5.1. Lý thuyết

UDP không đảm bảo, nên muốn truyền file đúng byte phải **tự xây dựng cơ chế tin cậy** ở tầng ứng dụng:

1. **Chia nhỏ** file thành các chunk cố định (4 KB).
2. Mỗi chunk có **số thứ tự** (sequence).
3. Người nhận gửi **ACK** xác nhận từng chunk.
4. Nếu không nhận ACK trong thời gian chờ, **gửi lại** (retry).
5. Sau số lần retry tối đa, đánh dấu thất bại.

Đây là mô hình **Selective Repeat** đơn giản.

### 5.2. Truyền file trong Lab 1

| Khái niệm | File | Dòng / Ghi chú |
|-----------|------|----------------|
| Chia chunk 4 KB | [`Protocol.java`](src/main/java/lab1/common/Protocol.java:22) | `UDP_CHUNK_SIZE = 4 * 1024` |
| Timeout 500 ms | [`Protocol.java`](src/main/java/lab1/common/Protocol.java:23) | `UDP_ACK_TIMEOUT_MS = 500` |
| Retry tối đa 8 lần | [`Protocol.java`](src/main/java/lab1/common/Protocol.java:24) | `UDP_MAX_RETRIES = 8` |
| Magic chunk | [`Protocol.java`](src/main/java/lab1/common/Protocol.java:52) | `UDP_MAGIC_CHUNK = 0x1A01` |
| Magic ACK | [`Protocol.java`](src/main/java/lab1/common/Protocol.java:53) | `UDP_MAGIC_ACK = 0x1A02` |
| Mã hóa chunk | [`UdpFileRelay.encodeChunk()`](src/main/java/lab1/server/UdpFileRelay.java:166) | `ByteBuffer` gói tin nhị phân |
| Mã hóa ACK | [`UdpFileRelay.encodeAck()`](src/main/java/lab1/server/UdpFileRelay.java:173) | 10 byte |
| Nhận chunk + gửi ACK | [`UdpFileRelay.handlePacket()`](src/main/java/lab1/server/UdpFileRelay.java:67) | `putIfAbsent` chống trùng |
| Gửi lại khi mất ACK | [`UdpFileRelay.deliverWithRetry()`](src/main/java/lab1/server/UdpFileRelay.java:114) | Vòng lặp retry |
| Client nhận file | [`ClientUdpFiles.java`](src/main/java/lab1/client/ClientUdpFiles.java) | Ghi chunk vào file tạm |
| Client gửi file | [`ClientUdpFiles.java`](src/main/java/lab1/client/ClientUdpFiles.java) | Đọc file, chia chunk, chờ ACK |

**Cấu trúc gói CHUNK (nhị phân):**

```text
┌──────────┬────────────┬──────────┬───────────┬──────────┬─────────────┐
│ magic(2) │ transferId │ sequence │ total     │ length   │ data        │
│ 0x1A01   │ (4 byte)   │ (4 byte) │ (4 byte)  │ (2 byte) │ (≤ 4096 B)  │
└──────────┴────────────┴──────────┴───────────┴──────────┴─────────────┘
```

**Cấu trúc gói ACK:**

```text
┌──────────┬────────────┬──────────┐
│ magic(2) │ transferId │ sequence │
│ 0x1A02   │ (4 byte)   │ (4 byte) │
└──────────┴────────────┴──────────┘
```

**Luồng truyền file:**

```text
An (sender)              Server (relay)              Binh (receiver)
     │  FILE_OFFER (TCP)  →  │                              │
     │  ←  FILE_READY     │                              │
     │  CHUNK #0 (UDP)    →  │  CHUNK #0 (UDP)           →  │
     │  ←  ACK #0         │  ←  ACK #0                  │
     │  CHUNK #1 (UDP)    →  │  CHUNK #1 (UDP)           →  │
     │  ←  ACK #1         │  ←  ACK #1                  │
     │  ...               │  ...                        │
     │  ←  FILE_DONE      │  ←  FILE_DONE               │
```

Server làm **relay** vì client sau NAT không mở được cổng vào. Mỗi chặng (sender→server, server→receiver) đều có ACK/retry riêng.

---

## 6. So sánh nhanh TCP vs UDP trong Lab 1

| Tiêu chí | TCP | UDP |
|----------|-----|-----|
| Kết nối | Có (handshake) | Không |
| Đảm bảo | Có | Không |
| Thứ tự | Có | Không |
| Tốc độ / trễ | Chậm hơn | Nhanh hơn |
| Dùng cho | Đăng nhập, chat riêng, điều khiển nhóm | Chat nhóm, broadcast, file |
| Cổng Lab 1 | `5000` | `5001` (file), `5002` (text) |
| File chính | [`ChatServer.java`](src/main/java/lab1/server/ChatServer.java), [`ClientHandler.java`](src/main/java/lab1/server/ClientHandler.java) | [`UdpFileRelay.java`](src/main/java/lab1/server/UdpFileRelay.java), [`UdpTextBroadcaster.java`](src/main/java/lab1/server/UdpTextBroadcaster.java) |

---

## 7. Bảng tra cứu: khái niệm → file

| Khái niệm | File chứa |
|-----------|-----------|
| Hằng số cổng, magic, kích thước | [`Protocol.java`](src/main/java/lab1/common/Protocol.java) |
| Mã hóa/giải mã gói UDP text | [`Protocol.encodeUdpText()`](src/main/java/lab1/common/Protocol.java:82), [`Protocol.decodeUdpText()`](src/main/java/lab1/common/Protocol.java:101) |
| Server TCP + điều phối | [`ChatServer.java`](src/main/java/lab1/server/ChatServer.java) |
| Xử lý một client TCP | [`ClientHandler.java`](src/main/java/lab1/server/ClientHandler.java) |
| Danh sách client online | [`ClientRegistry.java`](src/main/java/lab1/server/ClientRegistry.java) |
| Quản lý nhóm + địa chỉ multicast | [`GroupRegistry.java`](src/main/java/lab1/server/GroupRegistry.java) |
| Gửi UDP multicast/broadcast | [`UdpTextBroadcaster.java`](src/main/java/lab1/server/UdpTextBroadcaster.java) |
| Relay file UDP + ACK/retry | [`UdpFileRelay.java`](src/main/java/lab1/server/UdpFileRelay.java) |
| Client API mạng | [`ClientConnection.java`](src/main/java/lab1/client/ClientConnection.java) |
| Client nhận UDP text | [`ClientUdpText.java`](src/main/java/lab1/client/ClientUdpText.java) |
| Client gửi/nhận file UDP | [`ClientUdpFiles.java`](src/main/java/lab1/client/ClientUdpFiles.java) |
| Giao diện server | [`ServerFxApp.java`](src/main/java/lab1/server/ServerFxApp.java) |
| Giao diện client | [`ChatFxApp.java`](src/main/java/lab1/client/ChatFxApp.java) |
| Lưu tài khoản MySQL | [`MySqlDB.java`](src/main/java/lab1/db/MySqlDB.java) |
| Kiểm thử tích hợp | [`ChatIntegrationTest.java`](src/test/java/lab1/test/ChatIntegrationTest.java) |
| Bot demo tự động | [`LiveDemoBot.java`](src/test/java/lab1/test/LiveDemoBot.java) |

---

## 8. Câu hỏi lý thuyết thường gặp

**Hỏi: Vì sao TCP đảm bảo mà UDP không?**
Đáp: TCP có handshake, số thứ tự byte, ACK và tự retry ở tầng giao vận. UDP chỉ gửi datagram, không có cơ chế xác nhận.

**Hỏi: Multicast khác broadcast thế nào?**
Đáp: Multicast chỉ tới máy đã join nhóm; broadcast tới mọi máy trong LAN. Multicast tiết kiệm băng thông hơn khi nhóm nhỏ.

**Hỏi: Vì sao file dùng UDP mà vẫn đúng byte?**
Đáp: Ứng dụng tự chia chunk 4 KB, mỗi chunk có ACK và retry tối đa 8 lần. Đây là tầng tin cậy tự xây trên UDP.

**Hỏi: Vì sao server làm relay file?**
Đáp: Client sau NAT không mở được cổng vào. Server trung gian nhận từ sender rồi chuyển tiếp tới receiver.

**Hỏi: Vì sao có cả broadcast LAN và loopback?**
Đáp: Broadcast LAN để nhiều máy cùng nhận; loopback để máy đơn (chỉ chạy 1 máy) vẫn demo được.

**Hỏi: Vì sao chat nhóm dùng UDP thay vì TCP?**
Đáp: Gửi một lần tới nhiều người tiết kiệm băng thông; chat chấp nhận mất gói. TCP sẽ phải gửi N bản cho N người.

**Hỏi: Vì sao nhóm lưu RAM còn tài khoản lưu MySQL?**
Đáp: Tài khoản cần bền vững qua khởi động lại; nhóm là trạng thái phiên, mất khi server dừng là chấp nhận được (và giúp bài học về state).

**Hỏi: `setTcpNoDelay(true)` để làm gì?**
Đáp: Tắt thuật toán Nagle, giảm trễ cho tin nhắn chat nhỏ thay vì gộp gói.

**Hỏi: Vì sao dùng `DataInputStream`/`DataOutputStream`?**
Đáp: Đảm bảo định dạng nhị phân thống nhất (big-endian) giữa client và server, tránh lỗi endianness.
