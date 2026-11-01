# Bối cảnh dự án

## Mục tiêu

Repository gồm năm bài thực hành môn Lập trình mạng. Bài được đánh giá theo mức hoàn thiện, khả năng chạy demo nhiều máy và khả năng chỉ đúng class/method khi giảng viên hỏi.

| Lab | Bài toán | Yêu cầu trọng tâm | Hiện trạng |
|---|---|---|---|
| 1 | Chat Client–Server | 5–6 client, TCP/UDP, multicast, broadcast, emoji, đính kèm | Có code, JavaFX |
| 2 | FTP Server + Client | FTP, database server, tương thích FileZilla | Có code, client Swing cần chuyển JavaFX |
| 3 | Mail Client + local server | SMTP, POP3, Gmail và localhost, database | Có code, JavaFX |
| 4 | Web Server + Browser | HTTP, GET/HEAD/POST, history, download, media | Có code, browser Swing cần chuyển JavaFX |
| 5 | RMI đồng bộ hai server | Hai server máy thật, client máy ảo | Chờ code từ thành viên khác |

## Quyết định chung

- Java 21 trên hai máy Windows; Maven Wrapper ở từng lab.
- Maven hiện biên dịch ở mức Java 17 để tương thích; chỉ nâng khi cả nhóm thống nhất.
- JavaFX là UI mục tiêu cho cả năm lab. AtlantaFX/ControlsFX có thể dùng, CSS riêng quyết định nhận diện.
- Docker Desktop chạy MySQL 8.4; ứng dụng Java chạy trực tiếp trên Windows.
- Hai máy thật dùng cùng Wi-Fi/LAN và có Internet. Máy nào cũng có thể chạy server hoặc client.
- Code identifier dùng tiếng Anh; UI, tài liệu, comment giải thích và commit dùng tiếng Việt.
- Ưu tiên code dễ học và dễ trình bày hơn abstraction phức tạp.

## Phạm vi giai đoạn hiện tại

1. Hoàn thiện project harness: tài liệu chung, tài liệu từng lab và `AGENTS.md`.
2. Chuẩn hóa cấu trúc Maven và package cho Lab 1–4 mà không đổi hành vi.
3. Chuyển UI còn lại sang JavaFX và áp dụng design system.
4. Bổ sung test, script nhiều client, ảnh, video và bộ câu hỏi bảo vệ.
5. Không xây Lab 5 cho tới khi code của thành viên khác được đưa lên.

## Nguồn sự thật

- Yêu cầu và phạm vi: `REQUIREMENTS.md` của từng lab.
- Vị trí code: `FEATURE_MAP.md`.
- Cách truyền dữ liệu: `PROTOCOL.md`.
- Cách chạy: `README.md` và `DEMO.md`.
- Tiêu chí hoàn thành: `DEFINITION_OF_DONE.md`.
