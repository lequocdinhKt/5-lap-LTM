# 5 Lab Lập Trình Mạng - môi trường Windows thống nhất

## Project harness

- [Tiến độ hiện tại và lý do tạm dừng](PROGRESS.md)
- [Bối cảnh dự án](PROJECT_CONTEXT.md)
- [Kiến trúc chung](COMMON_ARCHITECTURE.md)
- [Design system JavaFX](DESIGN_SYSTEM.md)
- [Sơ đồ demo nhiều máy](DEMO_TOPOLOGY.md)
- [Quy tắc viết code](CODING_RULES.md)
- [Definition of Done](DEFINITION_OF_DONE.md)
- Rule cho Codex/AI nằm trong `AGENTS.md` ở root và từng thư mục lab; dự án không còn dùng `.cursor/rules`.

Repository dùng một quy ước chung cho hai máy Windows:

- Temurin JDK 21 x64 trên cả hai máy; mã nguồn biên dịch ở mức Java 17.
- Mỗi lab là một Maven project độc lập và có Maven Wrapper riêng.
- MySQL 8.4.13 chạy bằng Docker; toàn bộ ứng dụng giao diện dùng JavaFX và chạy trực tiếp trên Windows.
- Không dùng file `.class`, `target`, `out` hoặc JAR được build từ máy khác.

## Cài lần đầu trên mỗi máy

1. Cài Temurin JDK 21 x64 và Docker Desktop.
2. Clone repository.
3. Mở PowerShell tại thư mục này và chạy:

```powershell
.\setup-windows.ps1
.\verify-all.ps1
```

`setup-windows.ps1` tự tạo mật khẩu MySQL cục bộ trong `.env`, tạo `db.properties`
cho lab 1-3 và khởi động MySQL. Các file cấu hình cục bộ không được commit.

Docker Desktop phải **đang chạy** trước khi gọi `setup-windows.ps1`. Nếu máy đã cài MySQL trực tiếp
chiếm cổng `3306`, mở `.env` và đặt `MYSQL_PORT=3307` rồi chạy lại script; cổng đó được dùng cho cả
container lẫn `db.properties` của lab 1-3. Script chỉ **cảnh báo** (không dừng) khi JDK không phải 21.

Kiểm tra và dọn MySQL:

```powershell
docker compose ps        # ltm-mysql phải là Up (healthy)
docker compose down      # dừng và xoá container, giữ volume dữ liệu
```

Trong CI hoặc khi không muốn tạo file, cả ba lab cũng nhận các biến
`LTM_DB_URL`, `LTM_DB_NAME`, `LTM_DB_USER` và `LTM_DB_PASSWORD`.

Nếu máy đã cài MySQL trực tiếp, dùng:

```powershell
.\setup-windows.ps1 -SkipDocker
```

## Chạy giữa hai máy

- Đặt mạng Windows của cả hai máy ở chế độ **Private**.
- Máy A chạy server và MySQL; máy B chạy client, nhập IPv4 của máy A thay cho `localhost`.
- Trên máy A, mở PowerShell bằng quyền Administrator và chạy:

```powershell
.\configure-firewall.ps1
```

Các cổng sử dụng:

- Lab 1: TCP 5000, UDP 5001-5002.
- Lab 2: TCP 2121 và PASV TCP 30000-30020.
- Lab 3: SMTP 3025 và POP3 3110.
- Lab 4: HTTP 8080.

MySQL chỉ publish lên `127.0.0.1`; không cần mở cổng 3306 ra LAN khi server ứng dụng
và database chạy cùng máy.

## Quy ước script PowerShell

Các file `.ps1` được lưu UTF-8 **có BOM** để Windows PowerShell 5.1 đọc đúng tiếng Việt. Nếu một script
được tạo lại bằng editor mặc định "UTF-8 không BOM", nó sẽ lỗi parse với thông báo
`The string is missing the terminator` hoặc `Unexpected token`; hãy lưu lại kèm BOM.

## Lệnh chạy

Xem README trong từng thư mục lab. Các script đều tự chuyển về đúng thư mục nên có thể
gọi trực tiếp từ File Explorer hoặc PowerShell.

## Phân công Git cho hai người

Chỉ dùng repository root này. `lap3` đã được đưa vào cùng repository, không còn nested Git
hoặc submodule. Mỗi người tạo branch riêng, commit mã nguồn và mở pull request hoặc merge
vào `main`; không commit `db.properties`, `.env`, `target` hay `out`.
