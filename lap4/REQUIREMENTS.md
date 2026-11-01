# Yêu cầu Lab 4 — HTTP

## Bắt buộc theo đề

- Web Server kèm Web Client/Browser có giao thức HTTP.
- Server phục vụ nhiều loại tài nguyên như PDF, MP3, MP4.
- Browser có GET, HEAD, POST, history và download.

## Phạm vi đã chọn

- Tự cài HTTP/1.1 bằng TCP socket, không dùng HTTP framework/client cấp cao.
- GET/HEAD file tĩnh; POST form URL-encoded tại `/submit`.
- Status `200`, `400`, `404`, `405`, `500`; MIME cho HTML/PDF/MP3/MP4.
- Mini Browser xem body/status/header, history trong phiên và tải file.
- Một request trên một TCP connection với `Connection: close`.

## Tiêu chí nghiệm thu

- Browser và `curl` nhận đúng GET/HEAD/POST.
- File download có hash giống file server; HEAD không có body.
- Path traversal bị chặn; request lỗi không làm server dừng.
- UI mục tiêu chuyển từ Swing sang JavaFX và co giãn.

## Ngoài phạm vi

Không hỗ trợ HTTPS, keep-alive, chunked encoding, JavaScript engine hoặc duyệt Internet tùy ý.
