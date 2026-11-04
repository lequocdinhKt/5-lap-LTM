# Kiểm thử Lab 5

## Tự động

Chạy tại `lap5`:

```powershell
.\mvnw.cmd clean verify
```

Kết quả gần nhất ngày 04/11/2026 bằng Microsoft OpenJDK 21.0.12.1: Maven thành công, 52 test được phát hiện, 51 chạy thành công và 1 integration test được skip theo mặc định. Các test bao phủ rule tồn kho/lock, RMI adapter, đăng nhập/đăng ký, CRUD, rollback nguồn, transaction trùng, cấu hình ưu tiên, client service bất đồng bộ, vòng đời server, cấu trúc JavaFX và timeline.

Integration test thật cần MySQL Server local đang chạy và `mysql.exe` khả dụng:

```powershell
.\run-integration-tests.ps1
```

Script tạo hai schema tạm `lab5_test_a`, `lab5_test_b` trên MySQL local, chạy hai server bằng port `1199/1200–1202` và `1299/1300–1302`, kiểm tra đồng bộ/rollback rồi xóa đúng hai schema tạm.

Kết quả ngày 04/11/2026 bằng Microsoft OpenJDK 21.0.12.1 trên dịch vụ MySQL80 local: integration test thành công. Đã kiểm tra đăng ký/login qua peer, CRUD A→B và B→A, nhập kho/lịch sử, transaction trùng, tranh lock, lease hết hạn, fencing token cũ, dữ liệu không hợp lệ, port bận, database lỗi và rollback nguồn khi peer offline. Script kết thúc với mã `0` và xóa hai schema tạm.

Smoke JavaFX ngày 04/11/2026: `run-client.ps1` và `run-server.ps1` đều mở được cửa sổ; stylesheet được nạp không còn CSS parse error. Hai tiến trình được đóng ngay sau kiểm tra, chưa thay thế cho ảnh/video demo bốn máy.

## Cổng kiểm thử còn thiếu

- Demo LAN trên hai máy thật và hai VM, kèm ảnh/video.

Không đánh dấu Lab 5 hoàn thành trước khi các mục trên và `DEFINITION_OF_DONE.md` đều có bằng chứng.
