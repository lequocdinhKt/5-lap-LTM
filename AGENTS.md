# Quy tắc làm việc chung

- Đọc `PROJECT_CONTEXT.md`, `COMMON_ARCHITECTURE.md`, `CODING_RULES.md` và tài liệu của lab trước khi sửa code.
- Chỉ sửa một lab khi yêu cầu chỉ nhắc tới lab đó. Chỉ thay đổi nhiều lab khi yêu cầu nói rõ toàn bộ hoặc liệt kê nhiều lab.
- Mỗi lab là một Maven project độc lập; không tạo module Java dùng chung giữa các lab.
- Giữ ứng dụng chạy trực tiếp trên Windows, JDK 21 và Maven Wrapper; MySQL chạy bằng Docker Desktop.
- Code identifier bằng tiếng Anh. Giao diện, thông báo, tài liệu và commit bằng tiếng Việt.
- Ưu tiên code ngắn, tên method thể hiện hành động, một method một trách nhiệm và dễ chỉ ra khi giảng viên hỏi.
- UI gọi `service`; `service` gọi `network`/`db`. Không đặt socket, SQL hoặc xử lý file dài trong JavaFX Application Thread.
- Cấu hình host, port, tài khoản và đường dẫn phải nhận từ biến môi trường, tham số hoặc file cục bộ bị ignore; không hard-code secret.
- Mỗi thay đổi chức năng phải cập nhật `FEATURE_MAP.md`, `PROTOCOL.md`, `TESTING.md` hoặc `DEMO.md` liên quan.
- Không tuyên bố hoàn thành nếu chưa đối chiếu `DEFINITION_OF_DONE.md` và chạy kiểm tra phù hợp.
- Không xây mã nguồn Lab 5 cho tới khi người dùng yêu cầu; hiện chỉ duy trì tài liệu và quy tắc chuẩn bị tích hợp.
