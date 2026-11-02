# Lab 4 - Mini Web Server và Mini Web Browser

Dự án Java thuần minh họa HTTP/1.1 chạy trên TCP Socket. Server và client đều tự tạo, gửi và phân tích thông điệp HTTP; không dùng framework hoặc thư viện HTTP cấp cao.

## Yêu cầu

- JDK 17 trở lên.
- Chạy các lệnh bên dưới trực tiếp từ thư mục chính `lap4`.
- Chép `document.pdf`, `music.mp3`, `video.mp4` vào `www/files` nếu muốn demo tải media.

## Biên dịch trên PowerShell

```powershell
New-Item -ItemType Directory -Force out | Out-Null
$sources = Get-ChildItem -Path src -Recurse -Filter *.java
javac --release 17 -encoding UTF-8 -d out $sources.FullName
```

## Chạy

Mở terminal thứ nhất để chạy server:

```powershell
java -cp out lab4.server.MiniWebServer
```

Server mặc định dùng cổng `8080` và thư mục `www`. Có thể truyền tùy chọn:

```powershell
java -cp out lab4.server.MiniWebServer 8081 www
```

Mở terminal thứ hai để thử Raw HTTP Client dạng console:

```powershell
java -cp out lab4.client.RawHttpClient http://localhost:8080/
```

Chạy giao diện Mini Browser:

```powershell
java -cp out lab4.client.MiniBrowser
```

## Kiểm thử nhanh server

```powershell
curl.exe -i http://localhost:8080/
curl.exe -I http://localhost:8080/files/document.pdf
curl.exe -i -X POST -H "Content-Type: application/x-www-form-urlencoded" --data-urlencode "name=An" --data-urlencode "message=Xin chào" http://localhost:8080/submit
curl.exe -i http://localhost:8080/khong-ton-tai
curl.exe -i -X PUT http://localhost:8080/
curl.exe -i --path-as-is http://localhost:8080/../Lab4_MiniWeb_Project_Overview.md
```

Để kiểm tra file nhị phân không bị thay đổi:

```powershell
curl.exe -o downloaded.pdf http://localhost:8080/files/document.pdf
Get-FileHash www/files/document.pdf
Get-FileHash downloaded.pdf
```

Hai giá trị hash phải giống nhau.

## Phạm vi hỗ trợ

- GET và HEAD cho file tĩnh trong `www`.
- POST form URL-encoded tại `/submit`.
- Status `200`, `400`, `404`, `405`, `500`.
- MIME cho HTML, PDF, MP3 và MP4.
- Mỗi request dùng một kết nối với `Connection: close`.
- Thread pool 4 worker, giới hạn header 16 KB và request body 1 MB.
- Mini Browser có History trong phiên chạy, xem status/header và tải file.

Không hỗ trợ HTTPS, keep-alive, chunked encoding, JavaScript engine hoặc website Internet tùy ý. `JEditorPane` chỉ hiển thị HTML cơ bản.
