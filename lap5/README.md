# Lab 5 — Quản lý kho phân tán bằng Java RMI

Lab 5 là Maven project độc lập dùng JavaFX 21, Java RMI, MySQL Server 8.x local, HikariCP và JUnit. Hai server đồng bộ trực tiếp hai chiều; Server A điều phối lock lease/fencing. Mã nguồn dùng package `lab5.*` và không tương thích JAR `com.warehouse.*` cũ.

## Yêu cầu

- Windows, JDK 21, MySQL Server 8.x cài local và Maven Wrapper trong repo.
- Hai máy thật chạy Server/MySQL riêng; hai VM chạy client.
- TCP `1099–1102` giữa client/server và giữa hai server.

## Build và kiểm thử

```powershell
cd lap5
.\mvnw.cmd clean verify
```

Maven tạo `target\lab5-rmi-1.0-SNAPSHOT.jar` và shaded JAR `target\lab5-rmi-1.0-SNAPSHOT-all.jar`.

## Cấu hình một server mới

MySQL dùng schema `lab5_warehouse`. Trên máy A:

```powershell
.\configure-server.ps1 -Role A -LocalHost 192.168.1.10 -PeerHost 192.168.1.20 -SeedDemo
```

Trên máy B đổi Role/IP tương ứng. `-SeedDemo` chỉ dùng cho database demo mới. Script đọc kết nối MySQL local từ `.env` root, tạo `server.properties` bị ignore và áp dụng schema bằng `mysql.exe`.

Thứ tự cấu hình là tham số dòng lệnh → biến môi trường `LAB5_*` → properties cục bộ → default local. Không commit `server.properties`, mật khẩu hoặc IP cá nhân.

## Chạy

```powershell
.\run-server.ps1
.\run-client.ps1 -ServerHost 192.168.1.10 -ClientId client-a -Username staff_a
```

Chạy Server A trước Server B. Dashboard server phải báo database, RMI local, peer và lock coordinator. Client có màn hình kết nối/đăng nhập và dashboard sản phẩm.

## Port và binding

- Registry `1099`: `warehouse`, `sync`, `lock`.
- Warehouse `1100`, Sync `1101`, Lock `1102`.
- Lease 30 giây, heartbeat 10 giây.

## Cấu trúc

- `config`, `model`, `network.rmi`, `db`, `service`: lớp dùng chung theo tầng.
- `server.service`, `server.sync`, `server.lock`: nghiệp vụ và RMI adapter server.
- `ui.client`, `ui.server`: JavaFX view.
- `scripts`: schema và demo seed.

## Nhất quán và giới hạn

Nguồn ghi trong transaction, gọi peer, đợi xác nhận rồi commit; peer lỗi làm nguồn rollback. Transaction tồn kho có UUID idempotent và fencing token. Cơ chế này không phải 2PC: nếu peer commit nhưng mất phản hồi thì hai database có thể lệch và cần đối soát thủ công.

Xem [PROTOCOL.md](PROTOCOL.md), [TESTING.md](TESTING.md), [DEMO.md](DEMO.md), [DEFENSE.md](DEFENSE.md) và [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
