# Feature Map Lab 1

| Chức năng | Điểm vào UI/API | Xử lý chính | Server/protocol |
|---|---|---|---|
| Khởi động client | `ChatClient.main()` | `ChatFxApp` → `ChatService` | `ClientConnection.connect()` |
| Đăng nhập/đăng ký | `ChatFxApp.connect()` | `ChatService.connect()` | `ClientHandler.login()`, `MySqlDB` |
| Danh sách online | `ChatFxApp.onUserList()` | `ChatService.onUserList()` | `ClientConnection.readLoop()` ← `ChatServer.broadcastUserList()` |
| Chat riêng TCP | `ChatFxApp.sendCurrentText()` | `ChatService.sendText()` | `ClientConnection.sendText()` → `ClientHandler.readDirectText()` |
| Chat nhóm UDP multicast | `ChatFxApp.sendCurrentText()` | `ChatService.sendGroupText()` | `ChatServer.sendGroupText()` → `UdpTextBroadcaster.multicast()` |
| Chat tất cả UDP broadcast | action gửi tất cả | `ChatService.sendGroupText()` | `UdpTextBroadcaster.broadcast()` |
| Tạo nhóm | `ChatFxApp.showCreateGroup()` | `ChatService.createGroup()` | `ChatServer.createGroup()` → `GroupRegistry` |
| Sửa/rời/xóa nhóm | menu nhóm | `ChatService.updateGroupMembers()`, `leaveGroup()`, `deleteGroup()` | method tương ứng trong `ChatServer`/`GroupRegistry` |
| Gửi file/audio/video | `ChatFxApp.sendCurrentFile()` | `ChatService.sendFile()` hoặc `sendGroupFile()` | `ClientConnection` → `UdpFileRelay` |
| ACK và retry file | tự động | `ClientUdpFiles` | `UdpFileRelay.deliverWithRetry()` |
| Theo dõi server | `ServerFxApp.startServer()` | `ChatServer.startAsync()` | `ServerListener` |
| Trạng thái UI (badge/toast) | `ChatFxApp.setStatus()`, `ChatFxApp.showToast()` | — | — |
| Hiệu ứng danh sách chat | `ChatFxApp.addBubble()`, `renderConversation()` | — | — |
| Tài khoản demo tự đăng nhập | `ChatFxApp.connectDemoAccount()` | `ChatService.connectDemo()` | `ClientConnection.connect(host, port, username)` → `ClientHandler.login()` |
| Mở nhiều client demo | `run-demo-clients.ps1` | cờ `--connect`/`--register` trong `ChatFxApp.start()` | — |

Khi đổi tên hoặc di chuyển method, cập nhật bảng này trong cùng commit.
