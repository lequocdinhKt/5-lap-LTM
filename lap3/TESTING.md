# Kiểm thử Lab 3

## Build

```powershell
.\mvnw.cmd -q clean verify
```

## Integration test localhost

```powershell
.\mvnw.cmd -q test-compile exec:java "-Dexec.mainClass=lab3.LocalMailIntegrationTest" "-Dexec.classpathScope=test"
```

Test tự khởi động GreenMail, gửi SMTP từ user1, đọc POP3 user2 và kiểm tra subject/body. Đây là test bắt buộc trong `verify-all.ps1`.

## Bằng chứng đã chạy

- **02/11/2026:** `.\mvnw.cmd -B -q clean verify` tại `lap3` → thành công.
- **02/11/2026:** `.\mvnw.cmd -q test-compile exec:java "-Dexec.mainClass=lab3.LocalMailIntegrationTest" "-Dexec.classpathScope=test"` → in `PASS: SMTP và POP3 localhost hoạt động.`
- **02/11/2026:** `.\run-demo-clients.ps1 -ClientCount 2 -EmbeddedMailServer` → chặn đúng với thông báo
  `Chỉ mở 1 client khi dùng -EmbeddedMailServer vì mỗi client sẽ chiếm port 3025/3110.`
- **02/11/2026:** `.\run-demo-clients.ps1 -ServerHost 127.0.0.1 -ClientCount 1` → script chạy hết
  và in `Đã mở 1 cửa sổ. Đăng nhập tài khoản ứng dụng, rồi gửi và đọc thư bằng SMTP/POP3.`

## Test thủ công

- Sai password local/Gmail; email thiếu/không hợp lệ.
- Local server chưa chạy, host sai, firewall chặn từng port.
- Gửi Unicode, subject rỗng, body dài và HTML/multipart.
- Hai client đọc cùng mailbox trên hai máy.
- Đóng app khi task đang chạy; tài nguyên vẫn được đóng.

## Nguyên tắc Gmail

Không đưa Gmail vào test CI vì phụ thuộc Internet, tài khoản và rate limit. Ghi bằng chứng thủ công trong video/ảnh nhưng che email/App Password.
