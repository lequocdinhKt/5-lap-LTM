# Kiểm thử Lab 1

## Build

```powershell
.\mvnw.cmd -q clean verify
```

## Integration test hiện có

Khởi động MySQL theo README, sau đó:

```powershell
.\mvnw.cmd -q test-compile exec:java "-Dexec.mainClass=lab1.test.ChatIntegrationTest" "-Dexec.classpathScope=test"
```

Test cần giữ: xác thực, nhiều client, chat TCP, multicast/broadcast, quản lý nhóm và truyền file so sánh byte.

## Test thủ công hai máy

- Kết nối 5–6 client bằng IP máy server.
- Thử trùng username, sai mật khẩu, server dừng giữa phiên.
- Gửi text rỗng/quá dài và file quá 20 MB.
- Kiểm tra client ngoài nhóm không nhận multicast của nhóm.
- Ngắt một receiver trong lúc chuyển file; UI phải báo lỗi, không treo.

## Cấu trúc test

Integration test đã nằm trong `src/test/java`. Test cần MySQL nên chạy riêng sau khi `setup-windows.ps1` hoàn tất; build thông thường vẫn không phụ thuộc UI.
