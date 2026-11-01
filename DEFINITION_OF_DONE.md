# Definition of Done

Một lab chỉ được xem là hoàn thành khi có đủ bằng chứng bên dưới.

## Code và kiến trúc

- [ ] Build bằng `mvnw.cmd clean verify` trên Windows sạch.
- [ ] Cấu trúc Maven chuẩn và package đúng trách nhiệm.
- [ ] UI JavaFX theo `DESIGN_SYSTEM.md`, co giãn được khi mở nhiều client.
- [ ] Không có secret, IP cá nhân, file build hoặc cấu hình máy cục bộ trong Git.
- [ ] Network/database không block JavaFX Application Thread.

## Chức năng và test

- [ ] Tất cả yêu cầu trong `REQUIREMENTS.md` có trạng thái và bằng chứng.
- [ ] Có test tự động cho protocol/use case quan trọng.
- [ ] Có test lỗi: sai tài khoản, mất kết nối, port bận, dữ liệu không hợp lệ.
- [ ] Demo được giữa hai máy; Lab 1 chịu được 5–6 client.
- [ ] Script mở server/client và nhiều client không cần sửa code.

## Tài liệu bảo vệ

- [ ] `FEATURE_MAP.md` trỏ tới class và method hiện tại.
- [ ] `PROTOCOL.md` mô tả message, port, luồng và lỗi.
- [ ] `ARCHITECTURE.md` có sơ đồ luồng.
- [ ] `DEMO.md` có kịch bản ngắn, kịch bản đầy đủ và phương án dự phòng.
- [ ] `TESTING.md`, `TROUBLESHOOTING.md` đã được thử trên máy thứ hai.
- [ ] Có danh sách câu hỏi giảng viên và câu trả lời ngắn.

## Bằng chứng giao diện

- [ ] Ảnh màn hình server, client và trạng thái chính.
- [ ] Video demo có mở đầu, luồng thành công, một lỗi có chủ đích và kết luận.
- [ ] Ảnh/video ghi rõ commit được demo; không lộ mật khẩu hoặc dữ liệu cá nhân.
