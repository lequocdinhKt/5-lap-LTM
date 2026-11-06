# Feature Map Lab 2

| Chức năng | UI/API | Service | Client/Server |
|---|---|---|---|
| Kết nối/đăng nhập | `FtpClientApp.connect()` | `FtpService.connect()` | `FtpClientSession.connect()` → `ControlHandler.user()`, `pass()` |
| Liệt kê remote | `refreshRemote()` | `FtpService.browse()` | `FtpClientSession.list()` → `ControlHandler.list()` |
| Đổi thư mục | nhấp đúp remote | `FtpService.changeDirectory()` | `ControlHandler.changeDirectory()` |
| Upload + progress | `FtpClientApp.uploadSelected()` | `FtpService.upload()` | `FtpClientSession.upload()` → `ControlHandler.store()` |
| Download + progress | `FtpClientApp.downloadSelected()` | `FtpService.download()` | `FtpClientSession.download()` → `ControlHandler.retrieve()` |
| Tạo thư mục | `createRemoteDirectory()` | `FtpService.createDirectory()` | `ControlHandler.makeDirectory()` |
| Xóa file/thư mục | `deleteRemoteSelected()` | `FtpService.delete()` | `ControlHandler.delete()` |
| PASV | tự động trước data command | `FtpService` chuyển tiếp | `FtpClientSession.openPassive()` → `DataChannel` |
| PORT | client ngoài có thể gọi | không đổi | `ControlHandler.active()` |
| Xác thực DB | form kết nối | `FtpService.connect()` | USER/PASS → `MySqlDB.authenticate()` |
| Ghi log transfer | tự động | không đổi | `MySqlDB.logTransfer()` |

UI gọi `FtpService` bằng `FtpClientApp.runTask()` trên executor; JavaFX Application Thread chỉ cập nhật view.
