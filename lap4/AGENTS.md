# Quy tắc Lab 4 — HTTP

- Tự tạo và phân tích HTTP/1.1 bằng TCP socket; không dùng HTTP server/client framework cấp cao.
- Giữ cổng mặc định `8080`, web root cấu hình được và chặn path traversal.
- Hỗ trợ tối thiểu GET, HEAD, POST; status, header, MIME và body phải được ghi rõ trong `PROTOCOL.md`.
- Không đọc toàn bộ file media lớn vào bộ nhớ khi có thể stream.
- Khi chuyển Mini Browser sang JavaFX, giữ phần network độc lập với UI và giữ lịch sử trong service/model.
- Mọi endpoint hoặc method mới phải có kiểm thử curl/raw client và ánh xạ trong `FEATURE_MAP.md`.
