# Tiến độ dự án 5 Lab Lập trình mạng

- [x] ✅ **03/11/2026:** Đã nhập mã nguồn kho RMI vào `lap5`; chỉ cập nhật Lab 5 và tài liệu trạng thái chung. Không thay đổi Lab 1–4. Đã có bằng chứng 49 unit test và 43 kiểm tra MariaDB/RMI trên máy A; Maven clean verify với JDK 21 đã thành công (49 test); chưa nghiệm thu LAN.

Cập nhật gần nhất: **02/11/2026**

Baseline đã kiểm tra: commit `7265f1e` (nhánh `main`)

Trạng thái hiện tại: **Giai đoạn 4 đã xong; Lab 1–4 build sạch, `ChatIntegrationTest` Lab 1 đạt trên MySQL Docker, còn chờ demo hai máy, ảnh và video**.

## Cách đọc trạng thái

- [x] ✅ Hoàn thành: đã có kết quả và bằng chứng kiểm tra.
- [ ] 🟡 Đang làm: đang được thực hiện trong task hiện tại.
- [ ] ⬜ Chưa làm: chưa bắt đầu, không có blocker.
- [ ] ⏸️ Tạm dừng: đã xác định việc cần làm nhưng đang chờ điều kiện khác.
- [ ] 🚫 Bị chặn: không thể tiếp tục nếu thiếu người, máy, tài khoản hoặc code bên ngoài.

> Tại một thời điểm chỉ nên có một mục `🟡 Đang làm`. Khi kết thúc task phải đổi nó thành ✅, ⏸️ hoặc 🚫 và ghi lý do.

## Tổng quan theo giai đoạn

- [x] ✅ Giai đoạn 1 — Chuẩn hóa môi trường Windows, JDK, Maven Wrapper, Docker MySQL và firewall.
- [x] ✅ Giai đoạn 2 — Tạo project harness, tài liệu chung, tài liệu từng lab và `AGENTS.md`.
- [x] ✅ Giai đoạn 3 — Chuẩn hóa Maven layout/package cho Lab 1–4.
- [x] ✅ Giai đoạn 4 — Áp dụng design system JavaFX cho cả Lab 1–4; cả bốn lab build sạch.
- [ ] ⏸️ Giai đoạn 5 — Test tự động Lab 1–4 đã xong; demo hai máy, ảnh, video và hồ sơ bảo vệ đang chờ Máy A/B và người thực hiện.
- [ ] ⏸️ Giai đoạn 6 — Đã nhập mã Lab 5 và build/test local; còn demo LAN và chuẩn hóa JavaFX/Docker.

## Việc đang làm

Hiện **không có task code đang chạy**. Giai đoạn 4 đã hoàn tất: Lab 1/3 có thêm sidebar, toast, skeleton và animation; script mở nhiều client đã có và đã chạy thử; 16 script `.ps1` được lưu UTF-8 có BOM để Windows PowerShell 5.1 đọc đúng tiếng Việt. Hệ thống đã chuyển sang MySQL 8.4.13 chạy Docker (`ltm-mysql`, `127.0.0.1:3307`) và `ChatIntegrationTest` của Lab 1 đã đạt.

Bước tiếp theo khi có điều kiện: dựng Máy A/B theo `DEMO_TOPOLOGY.md`, chạy demo hai máy, sau đó chụp ảnh và quay video bảo vệ.

## Hạ tầng và tài liệu chung

- [x] ✅ JDK 21 và Maven Wrapper độc lập cho Lab 1–4.
- [x] ✅ Docker Compose MySQL và script `setup-windows.ps1`.
- [x] ✅ Script build chung `verify-all.ps1`.
- [x] ✅ Script firewall cho toàn bộ port Lab 1–4.
- [x] ✅ Xóa nested Git/gitlink và file build/config cục bộ khỏi Git.
- [x] ✅ Sáu tài liệu chung ở root.
- [x] ✅ Tám tài liệu bắt buộc cho từng Lab 1–5.
- [x] ✅ Rule chung và riêng từng lab bằng `AGENTS.md`; không còn `.cursor/rules`.
- [x] ✅ Feature Map Lab 1–4 trỏ tới class/method hiện tại.
- [x] ✅ Chuyển Lab 1–4 sang Maven layout `src/main/java`, `src/main/resources`, `src/test/java`.
- [x] ✅ Chuẩn hóa namespace `lab1.*`–`lab4.*` và package theo vai trò với số package tối thiểu.
- [x] ✅ Script mở tự động nhiều client: `lap1/run-demo-clients.ps1` (1–6 client, `-AutoConnect`) và `lap3/run-demo-clients.ps1` (1–4 client).
- [x] ✅ Lưu toàn bộ `.ps1` dạng UTF-8 có BOM; sửa lỗi parse của `setup-windows.ps1` và `configure-firewall.ps1` trên Windows PowerShell 5.1.
- [x] ✅ `setup-windows.ps1` chạy được trên Windows PowerShell 5.1: sửa `NativeCommandError` khi gọi `java -version` và `docker compose up -d`.
- [x] ✅ MySQL 8.4.13 chạy Docker (`ltm-mysql`, `127.0.0.1:3307`) vì cổng `3306` đã bị MySQL80 cài trực tiếp chiếm; `db.properties` của Lab 1–3 được sinh lại với `user=root`.

## Lab 1 — Chat TCP/UDP

- [x] ✅ Server/client, đăng ký và đăng nhập MySQL.
- [x] ✅ Chat riêng TCP, nhóm UDP multicast và “Tất cả” UDP broadcast.
- [x] ✅ Tạo/sửa/rời/xóa nhóm và danh sách online.
- [x] ✅ Emoji và gửi file/audio/video bằng UDP ACK/retry.
- [x] ✅ UI client/server đang dùng JavaFX.
- [x] ✅ Có `ChatIntegrationTest` và bot demo.
- [x] ✅ Chuẩn hóa Maven layout/package; integration test 6 client vượt qua.
- [x] ✅ Chạy lại `ChatIntegrationTest` ngày 02/11/2026 trên MySQL Docker: 13 dòng `[PASS]` và kết thúc bằng `TẤT CẢ KIỂM THỬ TÍCH HỢP ĐÃ THÀNH CÔNG.`
- [x] ✅ Design system: sidebar/topbar, status badge, toast, fade animation và layout co giãn (danh sách online đẩy realtime nên không cần skeleton).
- [x] ✅ Script chạy 5–6 client: `run-demo-clients.ps1 -ClientCount 6`, `-ServerHost` trỏ IP Máy A, `-AutoConnect` tự đăng nhập tài khoản demo.
- [ ] ⏸️ Demo thực tế hai máy — cần hai máy cùng LAN để xác nhận multicast/broadcast/firewall.
- [ ] ⏸️ Ảnh giao diện và video bảo vệ — chờ chụp/quay trên UI cuối.

## Lab 2 — FTP

- [x] ✅ FTP Server/Client tự cài bằng TCP socket.
- [x] ✅ USER/PASS, LIST, CWD, RETR, STOR, PASV, PORT và quản lý file/thư mục.
- [x] ✅ MySQL user/transfer log và home riêng cho từng user.
- [x] ✅ `FtpIntegrationTest` upload/LIST/download/so sánh byte/xóa file vượt qua.
- [x] ✅ Chuẩn hóa Maven layout/package.
- [x] ✅ Chuyển Swing `FileZillaFrame` sang JavaFX `FtpClientApp`; smoke test khởi động thành công.
- [x] ✅ Progress upload/download, toast, connection badge và responsive split pane.
- [ ] ⏸️ Kiểm tra FileZilla và demo hai máy — cần chạy với máy/client thật qua dải PASV.
- [ ] ⏸️ Ảnh giao diện và video bảo vệ — chờ chụp/quay trên UI cuối.

## Lab 3 — Mail

- [x] ✅ JavaFX client, tài khoản ứng dụng và lịch sử gửi trong MySQL.
- [x] ✅ SMTP/POP3 localhost bằng GreenMail.
- [x] ✅ SMTP Gmail và POP3S Gmail đã có implementation.
- [x] ✅ Integration test SMTP/POP3 localhost vượt qua ngày 02/11/2026.
- [x] ✅ Có chế độ local mail server riêng cho demo LAN.
- [x] ✅ Chuẩn hóa Maven layout và package về namespace `lab3.*`.
- [x] ✅ Design system: sidebar mailbox, điều hướng ba trang, skeleton inbox, toast và trạng thái gửi/đọc.
- [x] ✅ Script mở nhiều mail client: `run-demo-clients.ps1 -ClientCount 2`.
- [ ] ⏸️ Test Gmail thật — cần Gmail demo, App Password và cấu hình POP của người dùng.
- [ ] ⏸️ Demo hai máy — cần Máy A chạy mail server và Máy B chạy client.
- [ ] ⏸️ Ảnh giao diện và video bảo vệ — chờ chụp/quay trên UI cuối.

## Lab 4 — HTTP

- [x] ✅ Mini Web Server tự cài HTTP/1.1 bằng TCP socket.
- [x] ✅ GET, HEAD, POST, status code, MIME, history và download.
- [x] ✅ Chặn path traversal và giới hạn header/body.
- [x] ✅ Build hiện tại thành công và có smoke test bằng curl trong tài liệu.
- [x] ✅ Viết automated integration test cho GET/HEAD/POST/404/405/traversal.
- [x] ✅ Chuẩn hóa Maven layout/package.
- [x] ✅ Chuyển Swing `MiniBrowser` sang JavaFX `MiniBrowserApp`; smoke test khởi động thành công.
- [x] ✅ Stream media lớn: `FileService.load()` trả metadata, `HttpResponse.file()` ghi body bằng `InputStream.transferTo()`; chỉ HTML nhỏ giữ trong `byte[]`.
- [ ] ⏸️ Demo hai máy và kiểm tra PDF/MP3/MP4 — cần file mẫu và máy thứ hai.
- [ ] ⏸️ Ảnh giao diện và video bảo vệ — chờ chụp/quay trên UI cuối.

## Lab 5 — RMI

- [x] ✅ Đã có README, requirements, architecture, feature map khung, protocol, demo, testing và troubleshooting.
- [x] ✅ Đã nhập mã nguồn, Maven Wrapper, remote interface và schema MariaDB vào lap5; Maven clean verify JDK 21 đạt 49 test.
- [x] ✅ Đã ghi contract RMI, port 1099–1102, đồng bộ hai chiều và giới hạn commit vào lap5/PROTOCOL.md.
- [ ] 🚫 Test/demo bốn máy — cần hai máy thật, hai máy ảo và code hoàn chỉnh.

## Vì sao công việc đang dừng

| Hạng mục | Trạng thái | Lý do | Điều kiện tiếp tục |
|---|---|---|---|
| Chuẩn hóa code Lab 1–4 | Hoàn thành | Đã chuyển Maven layout/package và build/test từng lab | Duy trì cùng cấu trúc cho thay đổi sau |
| Design system Lab 1–4 | Hoàn thành | Lab 1/3 đã thêm sidebar, toast, skeleton, animation; Lab 2/4 đã có progress/toast | Kiểm tra trực quan trên nhiều độ phân giải |
| Script nhiều client | Hoàn thành | `run-demo-clients.ps1` ở Lab 1/3 đã chạy thử trên một máy | Truyền `-ServerHost` là IPv4 Máy A khi demo hai máy |
| Script `.ps1` trên PowerShell 5.1 | Hoàn thành | 4 script lỗi parse vì thiếu UTF-8 BOM; đã thêm BOM cho cả 16 script | Giữ BOM khi tạo/sửa script trên Windows |
| MySQL cho Lab 1–3 | Hoàn thành | Đã chuyển sang MySQL 8.4.13 chạy Docker (container `ltm-mysql`, `127.0.0.1:3307`) và `setup-windows.ps1` sinh lại `db.properties` với `user=root` | Chạy `docker compose up -d` trước khi demo; mỗi máy giữ `.env` cục bộ riêng |
| Demo hai máy | Tạm dừng | Phiên hiện tại chỉ kiểm tra được trên một máy | Có Máy A/B cùng LAN và quyền firewall |
| Gmail thật | Tạm dừng | Không lưu App Password trong repo | Người dùng nhập tài khoản demo khi kiểm tra |
| Ảnh/video | Tạm dừng | UI cuối đã xong nhưng cần thao tác trên giao diện thật | Chụp/quay sau khi demo hai máy chạy được |
| Lab 5 | Tạm dừng nghiệm thu | Code và build local đã có; chưa demo LAN hoặc chuẩn hóa JavaFX/Docker | Xem lap5/TESTING.md và chạy hai máy thật |

## Lịch sử cập nhật

- **02/11/2026:** Tạo `PROGRESS.md`; ghi nhận môi trường và project harness đã hoàn thành, các giai đoạn code/UI/demo chưa bắt đầu.
- **02/11/2026:** Bắt đầu Giai đoạn 3 — chuẩn hóa Maven layout/package cho Lab 1–4, sau đó JavaFX hóa Lab 2 và Lab 4.
- **02/11/2026:** Hoàn thành Maven layout/package Lab 1–4; chuyển Lab 2/4 sang JavaFX; thêm progress FTP và integration test HTTP.
- **02/11/2026:** Smoke test hai JavaFX app thành công; bỏ `WebView` khỏi Lab 4 để tương thích cả JDK 21 và JBR/JDK mới không có `jdk.jsobject`.
- **02/11/2026:** Áp dụng nốt design system cho Lab 1 (sidebar/topbar/toast/animation, cờ `--connect`/`--register`) và Lab 3 (sidebar mailbox, skeleton inbox, toast).
- **02/11/2026:** Thêm `lap3/run-demo-clients.ps1`; nâng `lap1/run-demo-clients.ps1` với `-Password`/`-AutoConnect`/`-Register`; smoke test mở 2 client Lab 1 và tự đăng nhập thành công.
- **02/11/2026:** Phát hiện script `.ps1` lỗi parse trên Windows PowerShell 5.1 do thiếu UTF-8 BOM; đã thêm BOM cho 16 script (gồm `setup-windows.ps1`, `configure-firewall.ps1`) và parse lại toàn bộ đạt.
- **02/11/2026:** Chuyển Lab 4 sang stream file tĩnh; chạy `mvnw clean verify` cho Lab 1–4, `LocalMailIntegrationTest` (lab3) và `HttpIntegrationTest` (lab4) đều đạt.
- **02/11/2026:** Chẩn đoán `ChatIntegrationTest` Lab 1: MySQL80 đang chạy (port 3306 mở) nhưng `lap1/db.properties` dùng `db.user=lap1` và bị `Access denied`; `.env` ở root không tồn tại. Đã ghi nhận vào bảng “Vì sao công việc đang dừng” và sửa lại phần bằng chứng trong `lap1/TESTING.md`.
- **02/11/2026:** Sửa `setup-windows.ps1` để chạy được trên Windows PowerShell 5.1 (NativeCommandError từ `java -version` và `docker compose up -d`), chuyển MySQL sang Docker ở cổng `3307` vì `3306` bị MySQL80 chiếm; cập nhật `README.md` và `.env.example`.
- **02/11/2026:** `ChatIntegrationTest` Lab 1 đạt 13/13 `[PASS]` trên MySQL Docker; cập nhật lại bằng chứng trong `lap1/TESTING.md` và trạng thái MySQL trong `PROGRESS.md` từ “Bị chặn” sang “Hoàn thành”.

- **03/11/2026:** Nhập mã kho RMI từ Lap8_LTM vào lap5, cập nhật tài liệu lab và thêm Maven Wrapper; giữ nguyên Lab 1–4. Bằng chứng local: 49 unit test, 43 kiểm tra MariaDB/RMI. Chưa nghiệm thu LAN/JavaFX/Docker; xem lap5/TESTING.md.
