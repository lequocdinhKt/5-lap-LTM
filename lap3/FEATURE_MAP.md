# Feature Map Lab 3

| Chức năng | UI/controller | Service | Network/database |
|---|---|---|---|
| Khởi động app | `MailApp.start()` | `new AppService()` | có thể tạo `LocalMailServer` nhúng |
| Đăng ký | `MailAppController.handleRegister()` | `AppService.register()` | `MySqlDB.registerUser()` |
| Đăng nhập | `handleLogin()` | `AppService.login()` | `MySqlDB.loginUser()` |
| Đăng xuất | `handleLogout()` | `AppService.logout()` | — |
| Gửi Gmail/local | `handleSend()` | `AppService.sendMail()` | `MailProtocol.sendMail()`, `MySqlDB.saveMail()` |
| Đọc Gmail/local | `handleRead()` | `AppService.readMails()` | `MailProtocol.readMails()` |
| Chạy nền | `runBackground()` | use case tương ứng | SMTP/POP3/DB không block UI |
| Hiển thị inbox | `MailAppView.showInbox()` | danh sách `Mail` | — |
| Server local riêng | script server | — | `LocalMailServerMain.main()` → `LocalMailServer.start()` |
| Sidebar mailbox & điều hướng | `MailAppView.createScene()`, `showPage()` | — | — |
| Trạng thái busy/toast/skeleton | `setBusy()`, `showInfo()`, `showError()`, `showInboxLoading()` | — | — |
| Mở nhiều client demo | `run-demo-clients.ps1` | — | — |
| Đóng tài nguyên | lifecycle app | `AppService.close()` | `LocalMailServer.close()` |

Mọi thay đổi tên method/package phải cập nhật bảng này cùng commit.
