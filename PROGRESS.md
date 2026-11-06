# Tiến độ dự án 5 Lab Lập trình mạng

Cập nhật gần nhất: **06/11/2026**

Baseline Lab 5 đã khóa: commit `b30d3ee` (`origin/main`), triển khai trên nhánh `codex/lab5-standardize`.

Trạng thái hiện tại: **Lab 1–5 build sạch bằng JDK 21; Lab 1, Lab 2 và Lab 5 đã chạy integration trên MySQL local; Lab 5 smoke JavaFX đạt, chỉ còn demo nhiều máy và bằng chứng ảnh/video**.

## Cách đọc trạng thái

- [x] ✅ Hoàn thành: đã có kết quả và bằng chứng kiểm tra.
- [ ] 🟡 Đang làm: đang được thực hiện trong task hiện tại.
- [ ] ⬜ Chưa làm: chưa bắt đầu, không có blocker.
- [ ] ⏸️ Tạm dừng: đã xác định việc cần làm nhưng đang chờ điều kiện khác.
- [ ] 🚫 Bị chặn: không thể tiếp tục nếu thiếu người, máy, tài khoản hoặc code bên ngoài.

> Tại một thời điểm chỉ nên có một mục `🟡 Đang làm`. Khi kết thúc task phải đổi nó thành ✅, ⏸️ hoặc 🚫 và ghi lý do.

## Tổng quan theo giai đoạn

- [x] ✅ Giai đoạn 1 — Chuẩn hóa môi trường Windows, JDK, Maven Wrapper, MySQL local và firewall.
- [x] ✅ Giai đoạn 2 — Tạo project harness, tài liệu chung, tài liệu từng lab và `AGENTS.md`.
- [x] ✅ Giai đoạn 3 — Chuẩn hóa Maven layout/package cho Lab 1–4.
- [x] ✅ Giai đoạn 4 — Áp dụng design system JavaFX cho cả Lab 1–4; cả bốn lab build sạch.
- [ ] ⏸️ Giai đoạn 5 — Test tự động Lab 1–4 đã xong; demo hai máy, ảnh, video và hồ sơ bảo vệ đang chờ Máy A/B và người thực hiện.
- [ ] ⏸️ Giai đoạn 6 — Đã chuẩn hóa, test JDK 21, integration MySQL local và smoke JavaFX Lab 5; chờ demo hai máy thật/hai VM.

## Việc đang làm

- [x] ✅ Sửa nhận diện client và luồng lock Lab 5 để hai client khác nhau không thể cùng giữ quyền sửa/nhập/xuất trên một sản phẩm; test lock/client và build đã đạt.
- [x] ✅ UI Lab 5 chỉ mở dialog Sửa/Nhập kho/Xuất kho sau khi lấy lock thành công; client khác nhận thông báo và không mở giao diện thao tác.
- [x] ✅ Sửa câu SQL upsert lock của Lab 5 để tương thích MariaDB 10.4 và MySQL 8; test lock và build Lab 5 đã đạt.
- [x] ✅ Đã đồng bộ thư mục chính `5lap` với `origin/main`, gỡ worktree Lab 5 tạm và chạy `verify-all.ps1`; cả năm Maven project build thành công trước khi bàn giao cho thành viên pull.
- [x] ✅ Đã refactor tối giản Lab 1, 2 và 4 theo luồng `UI → service → network/db`; thêm `ChatService`, `FtpService`, `BrowserService`, giữ nguyên giao thức, cổng và giao diện. Cả ba lab `clean verify` thành công; ba integration test chạy qua service mới đều đạt.

Bước tiếp theo: demo hai máy và kiểm tra trực quan đầy đủ các thao tác UI; riêng Lab 4 có cảnh báo CSS `-fx-font-weight` cũ cần xử lý trong một task giao diện riêng nếu muốn log sạch.

## Hạ tầng và tài liệu chung

- [x] ✅ Maven Wrapper độc lập cho Lab 1–5; cấu hình mục tiêu JDK 21.
- [x] ✅ MySQL Server local và script `setup-windows.ps1` tự tìm `mysql.exe`, kiểm tra kết nối, sinh cấu hình cục bộ.
- [x] ✅ Script build chung `verify-all.ps1`.
- [x] ✅ Script firewall cho toàn bộ port Lab 1–5, gồm RMI TCP 1099–1102.
- [x] ✅ Xóa nested Git/gitlink và file build/config cục bộ khỏi Git.
- [x] ✅ Sáu tài liệu chung ở root.
- [x] ✅ Tám tài liệu bắt buộc cho từng Lab 1–5.
- [x] ✅ Rule chung và riêng từng lab bằng `AGENTS.md`; không còn `.cursor/rules`.
- [x] ✅ Feature Map Lab 1–4 trỏ tới class/method hiện tại.
- [x] ✅ Chuyển Lab 1–4 sang Maven layout `src/main/java`, `src/main/resources`, `src/test/java`.
- [x] ✅ Chuẩn hóa namespace `lab1.*`–`lab4.*` và package theo vai trò với số package tối thiểu.
- [x] ✅ Script mở tự động nhiều client: `lap1/run-demo-clients.ps1` (1–6 client, `-AutoConnect`) và `lap3/run-demo-clients.ps1` (1–4 client).
- [x] ✅ Lưu toàn bộ `.ps1` dạng UTF-8 có BOM; sửa lỗi parse của `setup-windows.ps1` và `configure-firewall.ps1` trên Windows PowerShell 5.1.
- [x] ✅ `setup-windows.ps1` chạy được trên Windows PowerShell 5.1, đọc mật khẩu kín và không đưa secret lên argv.
- [x] ✅ MySQL80 local chạy tại `127.0.0.1:3306`; `db.properties` của Lab 1–3 được sinh lại với cấu hình bị ignore.

## Lab 1 — Chat TCP/UDP

- [x] ✅ Server/client, đăng ký và đăng nhập MySQL.
- [x] ✅ Chat riêng TCP, nhóm UDP multicast và “Tất cả” UDP broadcast.
- [x] ✅ Tạo/sửa/rời/xóa nhóm và danh sách online.
- [x] ✅ Emoji và gửi file/audio/video bằng UDP ACK/retry.
- [x] ✅ UI client/server đang dùng JavaFX.
- [x] ✅ Có `ChatIntegrationTest` và bot demo.
- [x] ✅ Chuẩn hóa Maven layout/package; integration test 6 client vượt qua.
- [x] ✅ Chạy lại `ChatIntegrationTest` ngày 04/11/2026 trên MySQL80 local: 13 dòng `[PASS]` và kết thúc bằng `TẤT CẢ KIỂM THỬ TÍCH HỢP ĐÃ THÀNH CÔNG.`
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

- [x] ✅ Maven `com.ltm:lab5-rmi`, package `lab5.*`, JavaFX 21 và MySQL Connector/J; bỏ Swing/MariaDB/Jackson/outbox cũ.
- [x] ✅ UI gọi service; SQL nằm trong repository; transaction nguồn qua `TransactionManager`; RMI adapter giữ contract nghiệp vụ.
- [x] ✅ Giữ đồng bộ trực tiếp hai chiều, rollback nguồn, lease 30 giây, heartbeat 10 giây, fencing và idempotency.
- [x] ✅ Có script cấu hình/chạy/integration, schema/seed mới, firewall 1099–1102 và tài liệu tiếng Việt đồng bộ.
- [x] ✅ `mvnw.cmd clean verify`: 52 test, 51 đạt, 1 integration test skip; `verify-all.ps1` Lab 1–5 đạt trên JDK 25.
- [x] ✅ Integration MySQL local bằng JDK 21 — hai schema tạm, hai RMI server và toàn bộ ma trận chính đã đạt; script trả mã `0` và schema tạm được xóa.
- [x] ✅ Kiểm chứng JDK 21 — `verify-all.ps1` đạt bằng Microsoft OpenJDK 21.0.12.1.
- [x] ✅ Smoke JavaFX client/server — hai cửa sổ mở thành công, không còn CSS parse error.
- [ ] ⏸️ Demo bốn máy — cần hai máy thật, hai VM, ảnh và video.

## Vì sao công việc đang dừng

| Hạng mục | Trạng thái | Lý do | Điều kiện tiếp tục |
|---|---|---|---|
| Chuẩn hóa code Lab 1–4 | Hoàn thành | Đã chuyển Maven layout/package và build/test từng lab | Duy trì cùng cấu trúc cho thay đổi sau |
| Design system Lab 1–4 | Hoàn thành | Lab 1/3 đã thêm sidebar, toast, skeleton, animation; Lab 2/4 đã có progress/toast | Kiểm tra trực quan trên nhiều độ phân giải |
| Script nhiều client | Hoàn thành | `run-demo-clients.ps1` ở Lab 1/3 đã chạy thử trên một máy | Truyền `-ServerHost` là IPv4 Máy A khi demo hai máy |
| Script `.ps1` trên PowerShell 5.1 | Hoàn thành | 4 script lỗi parse vì thiếu UTF-8 BOM; đã thêm BOM cho cả 16 script | Giữ BOM khi tạo/sửa script trên Windows |
| MySQL cho Lab 1–3 | Hoàn thành | MySQL80 local `127.0.0.1:3306`; `setup-windows.ps1` kiểm tra kết nối và sinh `db.properties` | Giữ Windows service MySQL chạy; mỗi máy giữ `.env` cục bộ riêng |
| Demo hai máy | Tạm dừng | Phiên hiện tại chỉ kiểm tra được trên một máy | Có Máy A/B cùng LAN và quyền firewall |
| Gmail thật | Tạm dừng | Không lưu App Password trong repo | Người dùng nhập tài khoản demo khi kiểm tra |
| Ảnh/video | Tạm dừng | UI cuối đã xong nhưng cần thao tác trên giao diện thật | Chụp/quay sau khi demo hai máy chạy được |
| Lab 5 | Tạm dừng nghiệm thu | Code/tài liệu, JDK 21, integration MySQL local và smoke UI đã đạt; chưa có topology bốn máy/ảnh/video | Demo hai máy thật/hai VM và lưu bằng chứng |

## Lịch sử cập nhật

- **06/11/2026:** Bắt đầu hợp nhất về một thư mục làm việc `5lap`; đã sao lưu trạng thái local cũ bằng nhánh backup và stash trước khi đồng bộ với `origin/main`.
- **06/11/2026:** Hoàn thành hợp nhất về `5lap`, gỡ worktree `5lap-lab5-standardize`, lưu bản Lab 5 độc lập cũ thành ZIP và chạy `verify-all.ps1` thành công cho Lab 1–5.
- **04/11/2026:** Bắt đầu chuẩn hóa Lab 5 từ commit `b30d3ee`: giữ nghiệp vụ RMI/lock hiện có, chuyển sang JavaFX, kiến trúc phân lớp, MySQL local và harness chung.
- **04/11/2026:** Hoàn tất phần triển khai local Lab 5: JavaFX client/server, package `lab5.*`, repository/transaction manager, cấu hình/script Windows và 52 test. Integration hai RMI server trên MySQL local đạt; chuyển Giai đoạn 6 sang tạm dừng vì JDK 21/UI/demo bốn máy chưa có.
- **04/11/2026:** Theo yêu cầu mới, xóa cấu hình và tài liệu phụ thuộc hạ tầng database ảo hóa; `setup-windows.ps1`, Lab 1–3 và integration Lab 5 chuyển hoàn toàn sang dịch vụ MySQL local/`mysql.exe`.
- **04/11/2026:** Kiểm chứng MySQL local: `ChatIntegrationTest` Lab 1, `FtpIntegrationTest` Lab 2 và `run-integration-tests.ps1` Lab 5 đều thành công; `verify-all.ps1` Lab 1–5 đạt.
- **04/11/2026:** Loại bỏ Swing/AWT còn sót trong Lab 5, sửa CSS font-weight, mở smoke hai JavaFX window thành công và chuyển thao tác đóng RMI/database ra background service.
- **04/11/2026:** Tìm thấy Microsoft OpenJDK 21.0.12.1 cục bộ; chạy `verify-all.ps1` bằng đúng JDK 21, cả Lab 1–5 cùng integration Lab 3/4 đều đạt.
- **04/11/2026:** Chạy lại `lap5/run-integration-tests.ps1` bằng Microsoft OpenJDK 21.0.12.1 trên MySQL80 local; toàn bộ ma trận hai RMI server đạt, script trả mã `0` và dọn hai schema tạm.
- **06/11/2026:** Bắt đầu refactor Lab 1, 2 và 4 theo kiến trúc service mỏng; không thay đổi Lab 3, Lab 5 hoặc protocol.
- **06/11/2026:** Hoàn thành refactor `ChatService`, `FtpService`, `BrowserService`; UI không còn gọi trực tiếp lớp network. `clean verify` của Lab 1/2/4 thành công; `ChatIntegrationTest`, `FtpIntegrationTest`, `HttpIntegrationTest` đều đạt qua service mới; ba client JavaFX khởi động được.
- **06/11/2026:** Bắt đầu rà soát cách chạy 5 lab trên VirtualBox/Tiny10; ưu tiên xác định topology đúng theo yêu cầu Lab 5 và không sửa mã nguồn.
- **06/11/2026:** Hoàn thành rà soát tài liệu chạy, cổng và cấu hình; kết luận Lab 1–4 có thể dùng máy thật làm server và VM làm client, còn Lab 5 khi nghiệm thu phải dùng hai server trên hai máy thật và hai client trên hai VM.
- **06/11/2026:** Phát hiện Lab 5 kết nối được database/RMI nhưng thao tác lock lỗi trên MariaDB 10.4 do cú pháp alias `VALUES (...) AS incoming`; đang thay bằng cú pháp `VALUES(column)` tương thích MariaDB và MySQL.
- **06/11/2026:** Sửa `lap5/src/main/java/lab5/db/LockRepository.java` dùng `VALUES(column)` cho upsert lock; `LockRulesTest`, `LockExportTest` và build package Lab 5 đều đạt.
- **06/11/2026:** Bắt đầu điều tra phản ánh hai client vẫn cùng thao tác một sản phẩm; cần bảo đảm mỗi tiến trình client có client ID thực sự duy nhất và kiểm tra lock ở server cho cả sửa, nhập kho, xuất kho.
- **06/11/2026:** ClientLauncher tạo runtime client ID duy nhất theo ID cấu hình, tên máy và UUID ngắn; lock server tiếp tục kiểm tra token/fencing/client ID cho sửa, xóa, nhập kho và xuất kho. Các test lock/client và build Lab 5 đạt.
- **06/11/2026:** Chốt guard tại `ClientView.withLease`: khi acquire lock thất bại hoặc trả lease rỗng, không gọi dialog/action; cập nhật kịch bản demo và contract UI trong tài liệu.
- **06/11/2026:** Bắt đầu đối chiếu luật lề code và cấu trúc package tối giản phục vụ việc chỉ class/method khi giảng viên hỏi.
- **06/11/2026:** Hoàn thành đối chiếu `CODING_RULES.md`, `COMMON_ARCHITECTURE.md` và package thực tế; đề xuất giữ ít package nhưng tách rõ UI, nghiệp vụ và I/O thay vì gom vào hai class lớn.
- **04/11/2026:** Bắt đầu chẩn đoán lỗi không khởi động được server Lab 1 và tổng hợp quy trình chạy Lab 1–5 theo tài liệu hiện có.
- **04/11/2026:** Xác nhận Lab 1 dừng với `Khong doc duoc db.properties`; ghi nhận Docker daemon chưa chạy, TCP 3306 đang bị tiến trình khác chiếm và JDK hiện tại là 25.0.2.
- **04/11/2026:** Đính chính hướng dẫn: Lab 1–3 hỗ trợ MySQL local bằng `-SkipDocker`; Lab 4 không dùng database; Lab 5 dùng MariaDB local riêng cho hai server.

- **02/11/2026:** Tạo `PROGRESS.md`; ghi nhận môi trường và project harness đã hoàn thành, các giai đoạn code/UI/demo chưa bắt đầu.
- **02/11/2026:** Bắt đầu Giai đoạn 3 — chuẩn hóa Maven layout/package cho Lab 1–4, sau đó JavaFX hóa Lab 2 và Lab 4.
- **02/11/2026:** Hoàn thành Maven layout/package Lab 1–4; chuyển Lab 2/4 sang JavaFX; thêm progress FTP và integration test HTTP.
- **02/11/2026:** Smoke test hai JavaFX app thành công; bỏ `WebView` khỏi Lab 4 để tương thích cả JDK 21 và JBR/JDK mới không có `jdk.jsobject`.
- **02/11/2026:** Áp dụng nốt design system cho Lab 1 (sidebar/topbar/toast/animation, cờ `--connect`/`--register`) và Lab 3 (sidebar mailbox, skeleton inbox, toast).
- **02/11/2026:** Thêm `lap3/run-demo-clients.ps1`; nâng `lap1/run-demo-clients.ps1` với `-Password`/`-AutoConnect`/`-Register`; smoke test mở 2 client Lab 1 và tự đăng nhập thành công.
- **02/11/2026:** Phát hiện script `.ps1` lỗi parse trên Windows PowerShell 5.1 do thiếu UTF-8 BOM; đã thêm BOM cho 16 script (gồm `setup-windows.ps1`, `configure-firewall.ps1`) và parse lại toàn bộ đạt.
- **02/11/2026:** Chuyển Lab 4 sang stream file tĩnh; chạy `mvnw clean verify` cho Lab 1–4, `LocalMailIntegrationTest` (lab3) và `HttpIntegrationTest` (lab4) đều đạt.
- **02/11/2026:** Chẩn đoán `ChatIntegrationTest` Lab 1: MySQL80 đang chạy (port 3306 mở) nhưng `lap1/db.properties` dùng `db.user=lap1` và bị `Access denied`; `.env` ở root không tồn tại. Đã ghi nhận vào bảng “Vì sao công việc đang dừng” và sửa lại phần bằng chứng trong `lap1/TESTING.md`.
- **02/11/2026:** Sửa `setup-windows.ps1` để chạy được trên Windows PowerShell 5.1 và cập nhật `README.md`, `.env.example`.
- **04/11/2026:** `ChatIntegrationTest` Lab 1 đạt 13/13 `[PASS]` trên MySQL80 local; bỏ hạ tầng database ảo hóa và chuẩn hóa toàn repo dùng MySQL cài trực tiếp.

- **03/11/2026:** Nhập mã kho RMI từ Lap8_LTM vào lap5, cập nhật tài liệu lab và thêm Maven Wrapper; giữ nguyên Lab 1–4. Bằng chứng local: 49 unit test, 43 kiểm tra MariaDB/RMI. Chưa nghiệm thu LAN/JavaFX; xem lap5/TESTING.md.
