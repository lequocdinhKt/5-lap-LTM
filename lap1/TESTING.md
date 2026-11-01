# Kiểm thử Lab 1

## Build

```powershell
.\mvnw.cmd -q clean verify
```

## Integration test hiện có

Khởi động MySQL theo README, sau đó:

```powershell
.\mvnw.cmd -q compile exec:java "-Dexec.mainClass=lab1.test.ChatIntegrationTest"
```

Test cần giữ: xác thực, nhiều client, chat TCP, multicast/broadcast, quản lý nhóm và truyền file so sánh byte.

## Test thủ công hai máy

- Kết nối 5–6 client bằng IP máy server.
- Thử trùng username, sai mật khẩu, server dừng giữa phiên.
- Gửi text rỗng/quá dài và file quá 20 MB.
- Kiểm tra client ngoài nhóm không nhận multicast của nhóm.
- Ngắt một receiver trong lúc chuyển file; UI phải báo lỗi, không treo.

## Sau khi chuẩn hóa

Chuyển test sang `src/test/java`, dùng JUnit cho unit/service test và giữ một integration test có thể chạy bằng script. `verify-all.ps1` phải gọi được kiểm tra không tương tác.
