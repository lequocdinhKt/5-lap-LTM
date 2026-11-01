# Kiểm thử Lab 4

## Build

```powershell
.\mvnw.cmd -q clean verify
```

## Smoke test hiện tại

Chạy server rồi thực hiện:

```powershell
curl.exe -i http://localhost:8080/
curl.exe -I http://localhost:8080/files/document.pdf
curl.exe -i -X POST -H "Content-Type: application/x-www-form-urlencoded" --data "name=An&message=Xin+chao" http://localhost:8080/submit
curl.exe -i http://localhost:8080/khong-ton-tai
curl.exe -i -X PUT http://localhost:8080/
curl.exe -i --path-as-is http://localhost:8080/../README.md
```

## Kiểm tra byte

```powershell
curl.exe -o downloaded.pdf http://localhost:8080/files/document.pdf
Get-FileHash .\www\files\document.pdf
Get-FileHash .\downloaded.pdf
```

## Khoảng trống cần hoàn thiện

Hiện chưa có automated integration test cho HTTP. Cần thêm test khởi động server bằng port ngẫu nhiên, gọi `RawHttpClient` cho GET/HEAD/POST/404/405/path traversal và đóng server sau test. Test phải độc lập UI.

## Test hai máy

Kiểm tra URL bằng IPv4 Máy A, tải media lớn, nhiều request đồng thời, server dừng giữa download và history sau lỗi.
