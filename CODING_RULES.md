# Quy tắc viết code

## Mục tiêu học tập

Code phải giúp người học trả lời được: dữ liệu bắt đầu ở đâu, đi qua method nào, dùng giao thức gì và lỗi được xử lý ở đâu.

## Tên và cấu trúc

- Class là danh từ rõ vai trò: `FtpClientSession`, `MailProtocol`, `HttpRequest`.
- Method là động từ: `connect`, `sendMessage`, `parseRequest`, `saveTransferLog`.
- Tránh tên mơ hồ như `process`, `handleData`, `doStuff` nếu có thể nêu hành động cụ thể.
- Một method ưu tiên dưới 30 dòng; tách khi có nhiều hơn một trách nhiệm.
- Package ít nhưng rõ: `config`, `model`, `network`, `service`, `ui`.
- Constant giao thức và giá trị mặc định nằm tập trung, không rải magic number.

## Ngôn ngữ

- Tên package, class, method, variable và test bằng tiếng Anh.
- UI, log phục vụ demo, tài liệu và comment giải thích bằng tiếng Việt có dấu.
- Comment giải thích lý do hoặc giao thức; không lặp lại điều code đã nói rõ.

## Network và đồng thời

- Ghi rõ TCP/UDP, host, port, timeout và lifecycle kết nối.
- Không nuốt exception; chuyển thành thông báo có ngữ cảnh và log nguyên nhân.
- Mỗi socket/stream/thread pool có nơi tạo và nơi đóng xác định.
- Không block JavaFX Application Thread; dùng `Task`, executor hoặc service bất đồng bộ.
- UI update từ background thread phải qua `Platform.runLater` hoặc binding an toàn.

## Database và cấu hình

- Dùng prepared statement cho dữ liệu nhập từ người dùng.
- Không commit `.env`, `db.properties`, App Password hoặc token.
- IP/port có default phục vụ local nhưng phải override được cho demo LAN.

## Kiểm thử và tài liệu

- Tên test mô tả hành vi, ví dụ `uploadThenDownloadPreservesBytes`.
- Test protocol/service không phụ thuộc UI khi có thể.
- Khi thêm tính năng: code → test → `FEATURE_MAP.md` → `PROTOCOL.md` → `DEMO.md`.
- Commit tiếng Việt, ngắn và theo hành động, ví dụ `feat(lab2): thêm tiến trình tải file`.
