# Lab 5 - Hệ thống quản lý kho phân tán bằng Java RMI

Mã nguồn được nhập từ bài `Lap8_LTM` của thành viên nhóm. Tên artifact/JAR vẫn là `Lap8_LTM` để giữ tương thích với script chạy. Lab này là project độc lập dùng Swing và MariaDB, chưa chuyển sang JavaFX hoặc tích hợp Docker/setup chung của Lab 1–4. Chạy mọi lệnh dưới đây trong thư mục `lap5`.

Tài liệu: [Feature Map](FEATURE_MAP.md), [Contract RMI](PROTOCOL.md), [Kiểm thử](TESTING.md), [Yêu cầu](REQUIREMENTS.md).

Project này xây dựng một hệ thống quản lý kho phân tán bằng **Java 17, Java RMI, Swing và MariaDB**. Hệ thống có hai server độc lập, mỗi server kết nối tới một database riêng và đồng bộ dữ liệu với server còn lại.

## 1. Mô hình triển khai trên 2 máy chính và 2 máy ảo

Mô hình khuyến nghị:

```text
Máy chính A                         Máy chính B
└── Máy ảo A                        └── Máy ảo B
    ├── Server A (lock coordinator)     ├── Server B
    ├── MariaDB A                       ├── MariaDB B
    └── Client A                        └── Client B
```

Server A và Server B phải liên lạc được với nhau qua mạng LAN. Hai database phải độc lập; không để cả hai server cùng trỏ tới một database.

Ví dụ địa chỉ:

| Thành phần | Địa chỉ ví dụ | Nhiệm vụ |
|---|---:|---|
| VM A | `192.168.1.101` | Server A, MariaDB A, Client A |
| VM B | `192.168.1.102` | Server B, MariaDB B, Client B |

Địa chỉ trên chỉ là ví dụ. Thay bằng IPv4 thật của hai máy ảo. Có thể chạy client trên máy chính thay vì trong máy ảo, nhưng client vẫn phải cấu hình tới IP của VM tương ứng.

## 2. Yêu cầu phần mềm

Trên cả hai máy ảo cần cài:

- JDK 17 (khuyến nghị dùng đúng JDK 17 để khớp cấu hình project);
- MariaDB 10.4 trở lên;
- Maven 3.8 trở lên nếu build trực tiếp trên máy đó;
- Cho phép Java chạy qua Windows Firewall.

Kiểm tra:

```bat
java -version
javac -version
mvn -version
```

Hai lệnh `java -version` và `javac -version` phải cùng trỏ tới JDK 17. Nếu Windows đang chọn Java khác, đặt `JAVA_HOME` tới thư mục JDK 17 và đưa `%JAVA_HOME%\bin` lên trước các Java khác trong `PATH`, sau đó mở lại Command Prompt.

Nên dùng **Bridged Adapter** cho hai máy ảo để mỗi VM nhận một IP trong cùng mạng LAN. Không dùng `localhost` hoặc `127.0.0.1` để cấu hình kết nối giữa hai VM.

## 3. Build project

Thực hiện một lần trên máy phát triển:

```bat
mvn clean test package
```

Nếu chưa cài Maven, dùng `.\mvnw.cmd clean verify` trên Windows với JDK 21 (mã nguồn vẫn biên dịch ở mức Java 17). Lần chạy Wrapper đầu tiên cần Internet.

Sau khi build thành công, file cần dùng là:

```text
target\Lap8_LTM-1.0.0-SNAPSHOT-all.jar
```

Chép file JAR này và các file cấu hình tương ứng sang VM A, VM B. JAR có hậu tố `-all` đã chứa các thư viện phụ thuộc.

## 4. Chuẩn bị MariaDB trên hai máy ảo

Trên VM A tạo database cho Server A, ví dụ:

```sql
CREATE DATABASE warehouse_a CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Trên VM B tạo database riêng cho Server B:

```sql
CREATE DATABASE warehouse_b CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Trong HeidiSQL, chọn database tương ứng, chạy `scripts\schema.sql` để tạo bảng, sau đó chạy `scripts\demo-seed.sql` trên **từng database**. Schema dành cho database mới, không tự nâng cấp bảng cũ. Nếu database đã có dữ liệu, không chạy lại script seed để tránh trùng dữ liệu. Đồng bộ trực tiếp hiện không cần bảng `sync_events`.

Đảm bảo hai database có cùng tài khoản và sản phẩm ban đầu. Ứng dụng không tự tạo hoặc reset schema khi khởi động.

Tên `warehouse_a` và `warehouse_b` chỉ là ví dụ. Có thể dùng tên `lap8_ltm` trên cả hai VM, nhưng phải tạo hai database độc lập (mỗi database nằm trên một máy hoặc có tên/schema riêng).

Nếu MariaDB chạy trên chính VM, có thể dùng:

```properties
db.url=jdbc:mariadb://127.0.0.1:3306/warehouse_a
```

và tương tự đổi thành `warehouse_b` trên VM B. Nếu dùng tên `lap8_ltm`, thay tên database trong cả hai file cấu hình. Nếu database chạy ở máy khác, thay `127.0.0.1` bằng IP máy chứa MariaDB và mở cổng TCP `3306`.

## 5. Mở cổng mạng

Mở các cổng sau trên firewall của VM A và VM B:

| Cổng | Thành phần |
|---:|---|
| `1099` | RMI Registry |
| `1100` | Warehouse service |
| `1101` | Sync service |
| `1102` | Lock coordinator trên A và lock proxy trên B |
| `3306` | MariaDB, chỉ cần mở nếu database được truy cập từ máy khác |

Kiểm tra từ VM A tới VM B và ngược lại:

```powershell
ping 192.168.1.102
Test-NetConnection 192.168.1.102 -Port 1099
Test-NetConnection 192.168.1.102 -Port 1100
Test-NetConnection 192.168.1.102 -Port 1101
```

Đổi `192.168.1.102` thành IP thật của VM B khi kiểm tra. Nếu dùng client trên máy chính, máy chính cũng phải truy cập được cổng `1099` của VM.

## 6. Cấu hình Server A và Server B

Sao chép file mẫu:

```bat
copy src\main\resources\server-a.properties.example server-a.properties
copy src\main\resources\server-b.properties.example server-b.properties
```

### Cấu hình trên VM A - Server A

File `server-a.properties`:

```properties
server.id=server-a
server.host=192.168.1.101
rmi.registry.port=1099
rmi.service.port=1100
peer.server.host=192.168.1.102
peer.rmi.port=1099
lock.coordinator.host=192.168.1.101
lock.coordinator.port=1099
db.url=jdbc:mariadb://127.0.0.1:3306/warehouse_a
db.username=root
db.password=
lock.lease.seconds=30
sync.retry.delay.seconds=10
```

Server A là **lock coordinator duy nhất**, vì vậy `lock.coordinator.host` của Server A trỏ tới chính Server A.

### Cấu hình trên VM B - Server B

File `server-b.properties`:

```properties
server.id=server-b
server.host=192.168.1.102
rmi.registry.port=1099
rmi.service.port=1100
peer.server.host=192.168.1.101
peer.rmi.port=1099
lock.coordinator.host=192.168.1.101
lock.coordinator.port=1099
db.url=jdbc:mariadb://127.0.0.1:3306/warehouse_b
db.username=root
db.password=
lock.lease.seconds=30
sync.retry.delay.seconds=10
```

Không đặt `server.host=auto` khi triển khai liên máy nếu máy có nhiều card mạng. Ghi rõ IP LAN mà máy kia có thể truy cập.

## 7. Cấu hình Client A và Client B

Trên VM A tạo `client-a.properties`:

```properties
server.host=192.168.1.101
rmi.registry.port=1099
client.id=client-a
client.username=staff_a
```

Trên VM B tạo `client-b.properties`:

```properties
server.host=192.168.1.102
rmi.registry.port=1099
client.id=client-b
client.username=staff_b
```

Client cần truy cập cổng `1099` (RMI Registry), `1100` (warehouse service) và `1102` (lock service) của server tương ứng. Server B cũng cần truy cập cổng `1102` của Server A. Có thể nhập hoặc sửa IP trong màn hình kết nối; ứng dụng sẽ lưu IP thành công gần nhất vào file properties.

## 8. Thứ tự chạy

### Bước 1: Chạy Server A trước

Trên VM A, mở Command Prompt tại thư mục gốc project và chạy:

```bat
scripts\run-server-a.bat
```

Trong cửa sổ Server Monitor:

1. Kiểm tra địa chỉ local là IP của VM A.
2. Nhập IP peer là IP của VM B.
3. Bấm nút khởi động server.
4. Chờ trạng thái database và RMI khởi động thành công.

### Bước 2: Chạy Server B

Trên VM B:

```bat
scripts\run-server-b.bat
```

Trong Server Monitor, kiểm tra:

1. Địa chỉ local là IP của VM B.
2. Peer là IP của VM A.
3. Lock coordinator là IP của VM A.
4. Server B kết nối được tới Server A.

### Bước 3: Chạy Client A và Client B

Trên VM A:

```bat
scripts\run-client-a.bat
```

Trên VM B:

```bat
scripts\run-client-b.bat
```

Ở mỗi client, bấm **Kiểm tra kết nối** hoặc **Kết nối**, sau đó đăng nhập bằng tài khoản đã tạo trong database tương ứng.

Tóm tắt các lệnh chạy ngắn (trong PowerShell, thêm `.\` ở đầu lệnh):

```bat
scripts\run-server-a.bat
scripts\run-server-b.bat
scripts\run-client-a.bat
scripts\run-client-b.bat
```

Bạn cũng có thể mở thư mục `scripts` và nhấp đúp file `.bat` tương ứng, không cần gõ lệnh Java dài. Script tự chuyển về thư mục gốc project và giữ cửa sổ khi có lỗi để bạn đọc thông báo.

Khi chép sang VM, giữ cấu trúc `target\Lap8_LTM-1.0.0-SNAPSHOT-all.jar`, thư mục `scripts` và các file properties ở thư mục gốc. Máy chạy cần Java 17; chỉ máy build cần Maven.

## 9. Kiểm tra hoạt động

Chỉ kiểm tra đầy đủ đồng bộ sau khi máy B đã có database riêng, schema và dữ liệu mẫu giống máy A, đồng thời cả hai server đã khởi động. Khi chưa có máy B, có thể kiểm tra khởi động Server A, kết nối client, đăng nhập và xem dữ liệu có sẵn; thao tác đăng ký/thêm/sửa/xóa/nhập/xuất sẽ bị từ chối khi peer chưa kết nối.

Khi cập nhật phiên bản, build và chép cùng JAR mới sang cả hai máy. Tài khoản đăng ký mới được đồng bộ cùng UUID sang peer; tài khoản tạo từ phiên bản cũ cần được đối soát thủ công trước khi demo.

Thực hiện các kịch bản sau:

1. Thêm sản phẩm trên Client A, kiểm tra sản phẩm xuất hiện ở Client B.
2. Nhập hoặc xuất hàng, kiểm tra số lượng tồn trên cả hai server.
3. Mở thao tác sửa cùng một sản phẩm trên Client A, sau đó thử sửa sản phẩm đó trên Client B để kiểm tra lock.
4. Tắt client đang giữ lock, chờ khoảng 30 giây cho lease hết hạn rồi xin lock lại.
5. Ngắt Server B và thử ghi dữ liệu từ Client A. Thao tác ghi phải bị từ chối, không chỉ cập nhật database cục bộ.
6. Thử xuất số lượng lớn hơn tồn kho; giao dịch phải bị từ chối.
7. Xem lịch sử giao dịch để kiểm tra không có bản ghi trùng.

## 10. Xử lý lỗi thường gặp

### Client không kết nối được server

- Kiểm tra `server.host` có phải IP LAN của VM hay không.
- Kiểm tra Server tương ứng đã khởi động chưa.
- Kiểm tra cổng `1099` bằng `Test-NetConnection`.
- Không dùng `localhost` khi client và server ở hai máy khác nhau.

### Server B không đồng bộ được với Server A

- Phải chạy Server A trước.
- Kiểm tra `peer.server.host` của hai server trỏ đúng IP đối phương.
- Kiểm tra cổng `1099`, `1100` và `1101`.
- Kiểm tra `lock.coordinator.host` trên Server B trỏ tới Server A.

### Server không khởi động được

- Kiểm tra MariaDB đang chạy.
- Kiểm tra database, username và password trong file properties.
- Kiểm tra database đã có đúng schema.
- Không để giá trị `replace-me` hoặc bỏ trống ở các thuộc tính bắt buộc.

### Hai database không giống nhau

Ứng dụng đồng bộ trực tiếp qua RMI, không dùng giao dịch phân tán hai pha. Nếu peer đã commit nhưng kết nối mất trước khi server gọi commit cục bộ, hai database có thể lệch. Khi đó cần kiểm tra và đối soát thủ công trước khi tiếp tục demo.

## 11. Ghi chú bảo mật

Mật khẩu trong phiên bản lab được lưu dạng văn bản thường để phục vụ demo; không sử dụng cấu hình và tài khoản này trong môi trường thật. Không commit các file properties chứa mật khẩu hoặc thông tin mạng nội bộ.
