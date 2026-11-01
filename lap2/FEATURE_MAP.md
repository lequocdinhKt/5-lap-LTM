# Feature Map Lab 2

| Chức năng | UI/API | Client | Server |
|---|---|---|---|
| Kết nối/đăng nhập | `FileZillaFrame.connect()` | `FtpClientSession.connect()` | `ControlHandler.user()`, `pass()` |
| Liệt kê remote | `refreshRemote()` | `list()` → `openPassive()` | `ControlHandler.passive()`, `list()` |
| Đổi thư mục | nhấp đúp remote | `changeDirectory()` | `ControlHandler.changeDirectory()` |
| Upload | `FileZillaFrame.upload()` | `FtpClientSession.upload()` | `ControlHandler.store()` |
| Download | `FileZillaFrame.download()` | `FtpClientSession.download()` | `ControlHandler.retrieve()` |
| Tạo thư mục | `makeDirectory()` | `FtpClientSession.makeDirectory()` | `ControlHandler.makeDirectory()` |
| Xóa file/thư mục | `deleteRemote()` | `delete()` | `ControlHandler.delete()` |
| PASV | tự động trước data command | `openPassive()` | `passive()` → `DataChannel` |
| PORT | client ngoài có thể gọi | lệnh `PORT` | `ControlHandler.active()` |
| Xác thực DB | form kết nối | USER/PASS | `MySqlDB.authenticate()` |
| Ghi log transfer | tự động | — | `MySqlDB.logTransfer()` |

UI JavaFX sau này phải giữ tên use case rõ tương ứng; cập nhật bảng khi thay class/method.
