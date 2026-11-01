# HTTP/1.1 Lab 4

## Transport

TCP `8080`, một request/connection, timeout socket 10 giây và `Connection: close`.

## Request hỗ trợ

```http
GET /files/document.pdf HTTP/1.1
Host: 192.168.1.10:8080
Connection: close
```

```http
POST /submit HTTP/1.1
Host: 192.168.1.10:8080
Content-Type: application/x-www-form-urlencoded
Content-Length: ...

name=An&message=Xin+chao
```

- GET trả header và body.
- HEAD trả header giống GET nhưng không gửi body.
- POST chỉ hỗ trợ `/submit` và form URL-encoded UTF-8.

## Response

`HttpResponse.writeTo()` tạo status line, `Content-Type`, `Content-Length`, `Connection: close` và body khi được phép. Status: `200`, `400`, `404`, `405`, `500`; `405` có `Allow: GET, HEAD, POST`.

## Giới hạn parser

- Header tối đa 16 KB; request body tối đa 1 MB.
- Chỉ HTTP target hợp lệ; path được URL-decode và validate.
- Không hỗ trợ chunked request/response hoặc keep-alive.

## MIME

`MimeTypes` ánh xạ ít nhất HTML/CSS/text, PDF, MP3 và MP4. File không biết loại dùng binary mặc định. Nguồn sự thật là `src/lab4/server/MimeTypes.java`.
