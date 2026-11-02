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

## Automated integration test

```powershell
.\mvnw.cmd -q test-compile exec:java "-Dexec.mainClass=lab4.test.HttpIntegrationTest" "-Dexec.classpathScope=test"
```

Test dùng web root/port tạm và kiểm tra GET file stream đúng từng byte, HEAD, POST, 404, 405, path traversal rồi tự đóng server. Test độc lập UI.

## Bằng chứng đã chạy

- **02/11/2026:** `.\mvnw.cmd -B -q clean verify` tại `lap4` → thành công.
- **02/11/2026:** `.\mvnw.cmd -q test-compile exec:java "-Dexec.mainClass=lab4.test.HttpIntegrationTest" "-Dexec.classpathScope=test"` → in `PASS` cho từng nhóm kiểm tra và tự đóng server.
- File tĩnh/media được stream trực tiếp từ đĩa (`HttpResponse.file()` + `InputStream.transferTo()`), console chỉ in metadata nên không giữ file lớn trong RAM.

## Test hai máy

Kiểm tra URL bằng IPv4 Máy A, tải media lớn, nhiều request đồng thời, server dừng giữa download và history sau lỗi.
