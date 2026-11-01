# Feature Map Lab 1

| Chức năng | Điểm vào UI/API | Xử lý chính | Server/protocol |
|---|---|---|---|
| Khởi động client | `ChatClient.main()` | `ChatFxApp` | `ClientConnection.connect()` |
| Đăng nhập/đăng ký | màn hình xác thực | `ClientConnection.connect()` | `ClientHandler.login()`, `MySqlDB` |
| Danh sách online | listener của UI | `ClientConnection.readLoop()` | `ChatServer.broadcastUserList()` |
| Chat riêng TCP | action gửi | `ClientConnection.sendText()` | `ClientHandler.readDirectText()` → `ChatServer.sendDirectText()` |
| Chat nhóm UDP multicast | action gửi nhóm | `ClientConnection.sendGroupText()` | `ChatServer.sendGroupText()` → `UdpTextBroadcaster.multicast()` |
| Chat tất cả UDP broadcast | action gửi tất cả | `ClientConnection.sendGroupText()` | `UdpTextBroadcaster.broadcast()` |
| Tạo nhóm | dialog nhóm | `ClientConnection.createGroup()` | `ChatServer.createGroup()` → `GroupRegistry` |
| Sửa/rời/xóa nhóm | menu nhóm | `updateGroupMembers()`, `leaveGroup()`, `deleteGroup()` | method tương ứng trong `ChatServer`/`GroupRegistry` |
| Gửi file/audio/video | nút đính kèm | `sendFile()` hoặc `sendGroupFile()` | `offerDirectFile()`/`offerGroupFile()` → `UdpFileRelay` |
| ACK và retry file | tự động | `ClientUdpFiles` | `UdpFileRelay.deliverWithRetry()` |
| Theo dõi server | `ServerFxApp.startServer()` | `ChatServer.startAsync()` | `ServerListener` |

Khi đổi tên hoặc di chuyển method, cập nhật bảng này trong cùng commit.
