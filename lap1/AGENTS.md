# Quy tắc Lab 1 — Chat

- Phạm vi là Chat Client–Server: TCP điều khiển/chat riêng, UDP multicast/broadcast cho text và UDP ACK/retry cho file.
- Giữ các cổng mặc định: TCP `5000`, UDP file `5001`, UDP text `5002`; cho phép cấu hình khi chạy.
- Mọi frame mới phải khai báo tập trung trong `Protocol` và ghi vào `PROTOCOL.md`.
- Client không được cập nhật JavaFX từ network thread; luôn chuyển UI update qua `Platform.runLater`.
- Tính năng chat phải ánh xạ đến method cụ thể trong `FEATURE_MAP.md` và có kịch bản 5–6 client.
- Không thay TCP bằng UDP hoặc ngược lại nếu chưa giải thích mục đích học tập và cập nhật tài liệu.
