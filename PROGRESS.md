# Tiến độ dự án 5 Lab Lập trình mạng

Cập nhật gần nhất: **02/11/2026**

Baseline đã kiểm tra: commit `8f243ad`

Trạng thái hiện tại: **đã hoàn thành môi trường và tài liệu; chưa bắt đầu chuẩn hóa code/UI**.

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
- [ ] ⬜ Giai đoạn 3 — Chuẩn hóa Maven layout/package cho Lab 1–4.
- [ ] ⬜ Giai đoạn 4 — Chuẩn hóa JavaFX và áp dụng design system chung.
- [ ] ⬜ Giai đoạn 5 — Hoàn thiện test, demo hai máy, ảnh, video và hồ sơ bảo vệ.
- [ ] 🚫 Giai đoạn 6 — Tích hợp Lab 5; chờ code từ thành viên nhóm.

## Việc đang làm

Hiện **không có task code đang chạy**. Task gần nhất là tạo bộ theo dõi tiến độ này và đã hoàn thành. Việc tiếp theo đề xuất: **chuẩn hóa cấu trúc Lab 1 trước**, build/test, rồi lần lượt Lab 2–4.

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
- [ ] ⬜ Chuyển Lab 1–4 sang `src/main/java`, `src/main/resources`, `src/test/java`.
- [ ] ⬜ Chuẩn hóa package `config/model/network/service/ui` với số package tối thiểu.
- [ ] ⬜ Tạo script mở tự động 2–3 client trên mỗi máy.

## Lab 1 — Chat TCP/UDP

- [x] ✅ Server/client, đăng ký và đăng nhập MySQL.
- [x] ✅ Chat riêng TCP, nhóm UDP multicast và “Tất cả” UDP broadcast.
- [x] ✅ Tạo/sửa/rời/xóa nhóm và danh sách online.
- [x] ✅ Emoji và gửi file/audio/video bằng UDP ACK/retry.
- [x] ✅ UI client/server đang dùng JavaFX.
- [x] ✅ Có `ChatIntegrationTest` và bot demo.
- [ ] ⬜ Chuẩn hóa Maven layout/package.
- [ ] ⬜ Áp dụng design system, responsive layout, notification, skeleton và animation.
- [ ] ⬜ Script chạy 5–6 client trên hai máy.
- [ ] ⏸️ Demo thực tế hai máy — cần hai máy cùng LAN để xác nhận multicast/broadcast/firewall.
- [ ] ⬜ Ảnh giao diện và video bảo vệ.

## Lab 2 — FTP

- [x] ✅ FTP Server/Client tự cài bằng TCP socket.
- [x] ✅ USER/PASS, LIST, CWD, RETR, STOR, PASV, PORT và quản lý file/thư mục.
- [x] ✅ MySQL user/transfer log và home riêng cho từng user.
- [x] ✅ Có `FtpIntegrationTest`; build hiện tại thành công.
- [ ] ⬜ Chuẩn hóa Maven layout/package.
- [ ] ⏸️ Chuyển Swing `FileZillaFrame` sang JavaFX — thực hiện sau khi cấu trúc/test ổn định.
- [ ] ⬜ Progress upload/download, toast, connection badge và responsive split pane.
- [ ] ⏸️ Kiểm tra FileZilla và demo hai máy — cần chạy với máy/client thật qua dải PASV.
- [ ] ⬜ Ảnh giao diện và video bảo vệ.

## Lab 3 — Mail

- [x] ✅ JavaFX client, tài khoản ứng dụng và lịch sử gửi trong MySQL.
- [x] ✅ SMTP/POP3 localhost bằng GreenMail.
- [x] ✅ SMTP Gmail và POP3S Gmail đã có implementation.
- [x] ✅ Integration test SMTP/POP3 localhost vượt qua ngày 02/11/2026.
- [x] ✅ Có chế độ local mail server riêng cho demo LAN.
- [ ] ⬜ Chuẩn hóa Maven layout/package về `lab3.*`.
- [ ] ⬜ Áp dụng design system, sidebar mailbox, skeleton và trạng thái gửi/đọc.
- [ ] ⏸️ Test Gmail thật — cần Gmail demo, App Password và cấu hình POP của người dùng.
- [ ] ⏸️ Demo hai máy — cần Máy A chạy mail server và Máy B chạy client.
- [ ] ⬜ Ảnh giao diện và video bảo vệ.

## Lab 4 — HTTP

- [x] ✅ Mini Web Server tự cài HTTP/1.1 bằng TCP socket.
- [x] ✅ GET, HEAD, POST, status code, MIME, history và download.
- [x] ✅ Chặn path traversal và giới hạn header/body.
- [x] ✅ Build hiện tại thành công và có smoke test bằng curl trong tài liệu.
- [ ] ⬜ Viết automated integration test cho GET/HEAD/POST/404/405/traversal.
- [ ] ⬜ Chuẩn hóa Maven layout/package.
- [ ] ⏸️ Chuyển Swing `MiniBrowser` sang JavaFX — làm sau khi có integration test chống regression.
- [ ] ⬜ Stream media lớn thay vì giữ toàn bộ file trong `byte[]`.
- [ ] ⏸️ Demo hai máy và kiểm tra PDF/MP3/MP4 — cần file mẫu và máy thứ hai.
- [ ] ⬜ Ảnh giao diện và video bảo vệ.

## Lab 5 — RMI

- [x] ✅ Đã có README, requirements, architecture, feature map khung, protocol, demo, testing và troubleshooting.
- [ ] 🚫 Mã nguồn, Maven, remote interface và storage — chờ thành viên nhóm push.
- [ ] 🚫 Chốt registry/object port và chiến lược đồng bộ — phải review code thật trước.
- [ ] 🚫 Test/demo bốn máy — cần hai máy thật, hai máy ảo và code hoàn chỉnh.

## Vì sao công việc đang dừng

| Hạng mục | Trạng thái | Lý do | Điều kiện tiếp tục |
|---|---|---|---|
| Chuẩn hóa code Lab 1–4 | Chưa làm | Giai đoạn tài liệu vừa hoàn tất; cần thực hiện lần lượt để tránh phá nhiều lab cùng lúc | Bắt đầu từ Lab 1, build/test sau mỗi bước |
| JavaFX Lab 2/4 | Tạm dừng | Cần giữ hành vi FTP/HTTP ổn định trước khi thay UI | Chuẩn hóa cấu trúc và bổ sung test còn thiếu |
| Demo hai máy | Tạm dừng | Phiên hiện tại chỉ kiểm tra được trên một máy | Có Máy A/B cùng LAN và quyền firewall |
| Gmail thật | Tạm dừng | Không lưu App Password trong repo | Người dùng nhập tài khoản demo khi kiểm tra |
| Ảnh/video | Chưa làm | UI cuối chưa hoàn thiện | Chụp/quay sau khi design system được áp dụng |
| Lab 5 | Bị chặn | Chưa có code của thành viên nhóm | Thành viên nhóm push code lên repository |

## Lịch sử cập nhật

- **02/11/2026:** Tạo `PROGRESS.md`; ghi nhận môi trường và project harness đã hoàn thành, các giai đoạn code/UI/demo chưa bắt đầu.
