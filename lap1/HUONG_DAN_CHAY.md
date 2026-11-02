# Hướng dẫn chạy Lab 1 - Chat TCP/UDP

Tài liệu này hướng dẫn từ A-Z: cài môi trường, cấu hình MySQL, biên dịch và chạy server/client.

## 1. Yêu cầu môi trường

| Thành phần | Phiên bản / Ghi chú |
|------------|---------------------|
| JDK | 17 trở lên, **kèm JavaFX** (bản OpenJFX hoặc JDK có sẵn JavaFX) |
| MySQL Server | 8.x, chạy tại `localhost:3306` |
| Driver MySQL | [`lib/mysql-connector-j-9.5.0.jar`](lib/mysql-connector-j-9.5.0.jar) (đã có sẵn) |
| Hệ điều hành | Linux / macOS / Windows |

Kiểm tra nhanh:

```bash
java -version
javac -version
mysql --version
```

> Nếu `java -version` không có JavaFX, hãy cài OpenJFX (ví dụ trên Ubuntu: `sudo apt install openjfx`) hoặc dùng một JDK đi kèm JavaFX (như Liberica Full JDK).

## 2. Cấu hình MySQL

### 2.1. Tạo user và database (khuyến nghị)

Đăng nhập MySQL bằng tài khoản admin rồi chạy:

```sql
CREATE DATABASE IF NOT EXISTS lab1_chat
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'lap1'@'localhost' IDENTIFIED BY 'lap1pass';
GRANT ALL PRIVILEGES ON lab1_chat.* TO 'lap1'@'localhost';
FLUSH PRIVILEGES;
```

> Server cũng tự gọi `database.ensureReady()` để tạo database/bảng nếu user có quyền. Tuy nhiên tạo sẵn như trên sẽ tránh lỗi quyền.

### 2.2. Kiểm tra file cấu hình

Mở [`db.properties`](db.properties) và đảm bảo khớp với MySQL của bạn:

```properties
db.url=jdbc:mysql://localhost:3306
db.name=lab1_chat
db.user=lap1
db.password=lap1pass
```

Nếu MySQL dùng mật khẩu khác, sửa `db.password` tương ứng.

## 3. Biên dịch

Chạy tại thư mục `lap1/`:

### Linux / macOS

```bash
mkdir -p out
javac --release 17 -encoding UTF-8 \
  -cp "lib/mysql-connector-j-9.5.0.jar" \
  -d out $(find src -name "*.java")
```

### Windows (PowerShell)

```powershell
New-Item -ItemType Directory -Force -Path out | Out-Null
$sources = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
javac --release 17 -encoding UTF-8 -cp "lib\mysql-connector-j-9.5.0.jar" -d out $sources
```

Nếu biên dịch thành công, thư mục `out/` chứa các file `.class`.

## 4. Chạy Server

Server có hai chế độ: **giao diện JavaFX** (mặc định) và **console**.

### 4.0. Cấu hình JavaFX module path (quan trọng)

Nếu JDK của bạn **không kèm JavaFX** (như OpenJDK trên EndeavourOS/Arch), bạn phải trỏ tới các file JavaFX jar. Trên máy này, JavaFX 21.0.6 nằm trong Maven cache:

```bash
MP="/home/xiaoying/.m2/repository/org/openjfx/javafx-base/21.0.6/javafx-base-21.0.6-linux.jar:/home/xiaoying/.m2/repository/org/openjfx/javafx-graphics/21.0.6/javafx-graphics-21.0.6-linux.jar:/home/xiaoying/.m2/repository/org/openjfx/javafx-controls/21.0.6/javafx-controls-21.0.6-linux.jar:/home/xiaoying/.m2/repository/org/openjfx/javafx-fxml/21.0.6/javafx-fxml-21.0.6-linux.jar"
```

Sau đó thêm `--module-path "$MP" --add-modules javafx.controls,javafx.fxml` vào mọi lệnh chạy server/client giao diện. Nếu bạn dùng JDK có sẵn JavaFX (Liberica Full, Zulu FX...), có thể bỏ qua bước này.

### 4.1. Chế độ giao diện (khuyến nghị để demo)

```bash
java --module-path "$MP" --add-modules javafx.controls,javafx.fxml \
  -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.server.ChatServer
```

Trên Windows thay `:` bằng `;`:

```powershell
java -cp "out;lib\mysql-connector-j-9.5.0.jar" lab1.server.ChatServer
```

Cửa sổ server hiển thị:

- IP LAN của máy chạy server.
- Ba cổng: TCP `5000`, UDP text `5002`, UDP file `5001`.
- Nút **Khởi động server** / **Dừng server**.
- Bảng client online và log sự kiện.

Nhấn **Khởi động server** để bắt đầu lắng nghe.

### 4.2. Chế độ console (không cần JavaFX)

```bash
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.server.ChatServer --console
```

Có thể chỉ định cổng: `--console <tcp> <udpText> <udpFile>`, ví dụ:

```bash
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.server.ChatServer --console 5000 5002 5001
```

Nhấn `Ctrl+C` để dừng (có shutdown hook đóng socket an toàn).

## 5. Chạy Client

Mỗi client là một cửa sổ JavaFX. Mở nhiều terminal để mô phỏng nhiều người dùng.

> **Lưu ý:** Áp dụng `--module-path "$MP" --add-modules javafx.controls,javafx.fxml` như mục 4.0 cho mọi lệnh client bên dưới nếu JDK không kèm JavaFX.

### 5.1. Cú pháp

```bash
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.client.ChatClient [host] [port] [username] [password] [--connect]
```

| Tham số | Mặc định | Ý nghĩa |
|---------|----------|---------|
| `host` | `localhost` | IP/hostname của server |
| `port` | `5000` | Cổng TCP của server |
| `username` | rỗng | Tên đăng nhập (nếu muốn tự điền) |
| `password` | rỗng | Mật khẩu (nếu muốn tự điền) |
| `--connect` | tắt | Tự đăng nhập ngay khi mở |

### 5.2. Ví dụ

Mở cửa sổ đăng nhập trống:

```bash
java --module-path "$MP" --add-modules javafx.controls,javafx.fxml \
  -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.client.ChatClient
```

Tự điền sẵn thông tin:

```bash
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.client.ChatClient localhost 5000 alice matkhau123
```

Tự đăng nhập ngay (bỏ qua bước bấm nút):

```bash
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.client.ChatClient localhost 5000 alice matkhau123 --connect
```

> Lần đầu đăng nhập, nếu tài khoản chưa tồn tại, ứng dụng sẽ tự **đăng ký** rồi đăng nhập (tùy luồng UI). Nếu đã có, dùng đúng mật khẩu.

### 5.3. Chạy nhiều client trên cùng một máy

Mở 2-3 terminal khác nhau và chạy lệnh client với `username` khác nhau:

```bash
# Terminal 1
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.client.ChatClient localhost 5000 alice matkhau123

# Terminal 2
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.client.ChatClient localhost 5000 bob matkhau123

# Terminal 3
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.client.ChatClient localhost 5000 carol matkhau123
```

### 5.4. Chạy client từ máy khác trong LAN

1. Trên máy server, xem IP LAN hiển thị trong cửa sổ server (hoặc chạy `ip addr` / `ifconfig`).
2. Trên máy client, truyền IP đó:

```bash
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.client.ChatClient 192.168.1.10 5000 alice matkhau123
```

> Đảm bảo firewall mở các cổng `5000/tcp`, `5001/udp`, `5002/udp`. UDP multicast cần mạng LAN hỗ trợ multicast (thường mặc định có).

## 6. Chạy kiểm thử tích hợp

Bài test tự khởi động server trên cổng ngẫu nhiên, tạo 6 client, kiểm tra đăng nhập, chat riêng, nhóm, file UDP và lưu MySQL.

```bash
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.test.ChatIntegrationTest
```

Kết quả mong đợi kết thúc bằng:

```text
TẤT CẢ KIỂM THỬ TÍCH HỢP ĐÃ THÀNH CÔNG.
```

> Test cần MySQL đang chạy và `db.properties` đúng. Nếu cổng `5000` đang bận, test vẫn chạy vì nó tự chọn cổng trống.

## 7. Chạy bot demo tự động (tuỳ chọn)

Bot `LiveDemoBot` kết nối với tên `BotDemo`, chờ một người dùng có tên chỉ định rồi gửi tin nhắn/file minh họa.

```bash
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.test.LiveDemoBot [host] [port] [tenNguoiDung]
```

Ví dụ chờ người dùng `An`:

```bash
java -cp "out:lib/mysql-connector-j-9.5.0.jar" lab1.test.LiveDemoBot localhost 5000 An
```

## 8. Xử lý sự cố thường gặp

| Triệu chứng | Nguyên nhân | Cách khắc phục |
|-------------|-------------|----------------|
| `Khong ket noi duoc MySQL` | MySQL chưa chạy / sai `db.properties` | Khởi động MySQL, kiểm tra user/mật khẩu |
| `Address already in use` | Cổng 5000/5001/5002 đang bận | Dừng tiến trình cũ hoặc đổi cổng khi chạy |
| Không thấy JavaFX | JDK không kèm JavaFX | Cài OpenJFX hoặc dùng JDK Full |
| Client không kết nối được từ máy khác | Firewall chặn cổng | Mở `5000/tcp`, `5001/udp`, `5002/udp` |
| Chat nhóm không tới máy khác | Mạng không hỗ trợ multicast | Kiểm tra switch/router, thử cùng LAN |
| File không tới | UDP bị chặn hoặc quá 20 MB | Mở UDP 5001, giảm kích thước file |

## 9. Tắt ứng dụng sạch

- Server giao diện: bấm **Dừng server** rồi đóng cửa sổ.
- Server console: `Ctrl+C`.
- Client: đóng cửa sổ hoặc chọn thoát trong ứng dụng.
