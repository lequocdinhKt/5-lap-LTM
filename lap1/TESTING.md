# Kiểm thử Lab 1

## Build

```powershell
.\mvnw.cmd -q clean verify
```

## Bằng chứng đã chạy

- **02/11/2026:** `.\mvnw.cmd -B -q clean verify` tại `lap1` → thành công (compile + test-compile, không cần MySQL).
- **02/11/2026:** `.\run-demo-clients.ps1 -ServerHost localhost -ClientCount 2 -UsernamePrefix smoke -AutoConnect`
  → in `Đang mở 2 client tới localhost:5000 ...` và `Đã mở 2 cửa sổ và tự đăng nhập tài khoản smoke<STT>.`;
  kiểm tra tiến trình thấy 4 JVM mang `lab1.client.ChatClient`, sau đó đã dọn tiến trình.
- **02/11/2026:** `.\mvnw.cmd -q test-compile exec:java "-Dexec.mainClass=lab1.test.ChatIntegrationTest" "-Dexec.classpathScope=test"`
  → 13 dòng `[PASS]` (6 client đăng ký/đăng nhập MySQL, TCP DIRECT, multicast, broadcast, file UDP đúng byte,
  từ chối file > 20 MB, khởi động lại giữ tài khoản MySQL) và kết thúc bằng
  `TẤT CẢ KIỂM THỬ TÍCH HỢP ĐÃ THÀNH CÔNG.`
- Test chạy trên MySQL 8.4.13 của `compose.yaml` (container `ltm-mysql`, `127.0.0.1:3307`).
  Trên máy đã có MySQL cài trực tiếp chiếm `3306`, đặt `MYSQL_PORT=3307` trong `.env` rồi chạy `setup-windows.ps1`.

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
