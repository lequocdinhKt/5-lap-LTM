# Lab 1 - Chat Client/Server đa giao thức (TCP + UDP)

Ứng dụng chat JavaFX tự cài đặt trên socket thô, minh họa rõ ba mô hình truyền thông của lập trình mạng:

- **TCP** cho điều khiển phiên, đăng nhập/đăng ký và chat riêng (đảm bảo, có thứ tự).
- **UDP multicast** cho chat nhóm (best effort, không ACK).
- **UDP broadcast** cho nhóm "Tất cả".
- **UDP unicast có ACK/retry** cho truyền file, audio, video (tối đa 20 MB).

Tài khoản lưu trong **MySQL**, nhóm chỉ lưu trong **RAM** (làm mới khi server khởi động lại).

## Chức năng

- Đăng ký / đăng nhập tài khoản (mật khẩu băm SHA-256, lưu MySQL).
- Danh sách người đang online, cập nhật thời gian thực.
- Chat riêng 1-1 qua **TCP**.
- Tạo nhóm, chọn thành viên, sửa thành viên, rời nhóm, xóa nhóm.
- Chat nhóm qua **UDP multicast** (mỗi nhóm một địa chỉ `239.10.x.x`).
- Chat "Tất cả" qua **UDP broadcast**.
- Gửi file/audio/video qua **UDP unicast** có chia gói, ACK và truyền lại.
- Giao diện JavaFX cho cả server (bảng client + log) và client (bong bóng chat, emoji, đính kèm).

## Kiến trúc giao thức

```text
                 ┌──────────────────────────────┐
                 │           ChatServer          │
                 │  TCP :5000  UDP text :5002    │
                 │             UDP file :5001    │
                 └──────────────────────────────┘
   TCP :5000  ── LOGIN/REGISTER, TEXT (riêng), nhóm, FILE_OFFER (báo hiệu)
   UDP :5002  ── MULTICAST (nhóm) / BROADCAST (Tất cả) tin nhắn text
   UDP :5001  ── UNICAST file, chia chunk 4 KB, ACK + retry, server relay
```

- **TCP** giữ một kết nối lâu dài cho mỗi client, dùng `DataInputStream`/`DataOutputStream` với khung lệnh dạng chuỗi (`Protocol.*`).
- **UDP text** phát trên LAN và cả loopback để demo được nhiều máy lẫn một máy.
- **UDP file** đi qua server làm relay: sender → server → receiver, mỗi chặng đều có ACK/retry.

## Cấu trúc mã nguồn

| Thư mục | Vai trò |
|---------|---------|
| [`src/lab1/common/`](src/lab1/common) | Hằng số giao thức, gói tin UDP, kiểu dữ liệu dùng chung |
| [`src/lab1/server/`](src/lab1/server) | Server TCP, relay UDP file, broadcaster UDP text, UI server |
| [`src/lab1/client/`](src/lab1/client) | API mạng client, nhận UDP, UI chat |
| [`src/lab1/db/`](src/lab1/db) | Lưu tài khoản trong MySQL |
| [`src/lab1/test/`](src/lab1/test) | Kiểm thử tích hợp và bot demo tự động |

Chi tiết từng file xem tại [`KIEN_THUC_LAP_TRINH_MANG.md`](KIEN_THUC_LAP_TRINH_MANG.md).

## Yêu cầu

- JDK 17 trở lên (có JavaFX).
- MySQL Server chạy tại `localhost:3306`.
- Driver [`lib/mysql-connector-j-9.5.0.jar`](lib/mysql-connector-j-9.5.0.jar) (đã kèm sẵn).

## Cấu hình MySQL

Sửa [`db.properties`](db.properties):

```properties
db.url=jdbc:mysql://localhost:3306
db.name=lab1_chat
db.user=lap1
db.password=lap1pass
```

Server tự tạo database `lab1_chat` và bảng `users` khi khởi động.

## Chạy nhanh

Xem hướng dẫn đầy đủ tại [`HUONG_DAN_CHAY.md`](HUONG_DAN_CHAY.md).

```bash
# Biên dịch
javac --release 17 -encoding UTF-8 -cp "lib/mysql-connector-j-9.5.0.jar" -d out $(find src -name "*.java")

# Chạy server (có giao diện)
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.server.ChatServer

# Chạy client (mở nhiều cửa sổ)
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.client.ChatClient
```

## Demo

Xem kịch bản trình bày tại [`HUONG_DAN_DEMO.md`](HUONG_DAN_DEMO.md).

## Kiểm thử tích hợp

```bash
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.test.ChatIntegrationTest
```

Kết quả cuối:

```text
TẤT CẢ KIỂM THỬ TÍCH HỢP ĐÃ THÀNH CÔNG.
```

## Giới hạn có chủ đích

- Nhóm chỉ lưu RAM, mất khi server dừng.
- UDP text là best effort, không đảm bảo tới nơi (đúng bản chất UDP).
- File giới hạn 20 MB, chunk 4 KB, tối đa 8 lần truyền lại mỗi chunk.
- Mật khẩu băm SHA-256 không salt, chỉ phục vụ mục đích học tập.
