# Feature Map Lab 2

| Chức năng | UI/API | Client | Server |
|---|---|---|---|
| Kết nối/đăng nhập | `FtpClientApp.connect()` | `FtpClientSession.connect()` | `ControlHandler.user()`, `pass()` |
| Liệt kê remote | `refreshRemote()` | `list()` → `openPassive()` | `ControlHandler.passive()`, `list()` |
| Đổi thư mục | nhấp đúp remote | `changeDirectory()` | `ControlHandler.changeDirectory()` |
| Upload + progress | `FtpClientApp.uploadSelected()` | `FtpClientSession.upload(..., TransferProgress)` | `ControlHandler.store()` |
| Download + progress | `FtpClientApp.downloadSelected()` | `FtpClientSession.download(..., TransferProgress)` | `ControlHandler.retrieve()` |
| Tạo thư mục | `createRemoteDirectory()` | `FtpClientSession.makeDirectory()` | `ControlHandler.makeDirectory()` |
| Xóa file/thư mục | `deleteRemoteSelected()` | `delete()` | `ControlHandler.delete()` |
| PASV | tự động trước data command | `openPassive()` | `passive()` → `DataChannel` |
| PORT | client ngoài có thể gọi | lệnh `PORT` | `ControlHandler.active()` |
| Xác thực DB | form kết nối | USER/PASS | `MySqlDB.authenticate()` |
| Ghi log transfer | tự động | — | `MySqlDB.logTransfer()` |

UI chạy network bằng `FtpClientApp.runTask()` trên executor; JavaFX Application Thread chỉ cập nhật view.
