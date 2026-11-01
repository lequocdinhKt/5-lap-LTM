# Kiến trúc chung

## Cấu trúc mục tiêu

Mỗi lab là một Maven project độc lập:

```text
lapN/
├── pom.xml, mvnw, mvnw.cmd
├── README.md và tài liệu lab
├── scripts/                     # script chạy/demo sau khi chuẩn hóa
└── src/
    ├── main/
    │   ├── java/labN/
    │   │   ├── App.java
    │   │   ├── config/
    │   │   ├── model/
    │   │   ├── network/
    │   │   ├── service/
    │   │   └── ui/
    │   └── resources/{css,icons,fonts,views}/
    └── test/java/labN/
```

Đây là cấu trúc mục tiêu. Code hiện tại còn dùng `src/` tùy chỉnh; việc di chuyển phải thực hiện riêng từng lab và build/test sau mỗi bước.

## Trách nhiệm

- `config`: đọc host, port, biến môi trường và giá trị mặc định.
- `model`: dữ liệu thuần; không truy cập UI, socket hoặc database.
- `network`: socket, protocol parser/encoder, request/response và remote connection.
- `service`: use case và điều phối giữa UI, network, database.
- `ui`: JavaFX view/controller, binding và trạng thái hiển thị.
- `test`: kiểm thử protocol, service và luồng tích hợp server–client.

```mermaid
flowchart LR
    U[Người dùng] --> UI[JavaFX UI]
    UI --> S[Service]
    S --> N[Network / Protocol]
    S --> D[(Database)]
    N --> R[Server hoặc Client từ xa]
    N -- kết quả / lỗi --> S
    S -- view state --> UI
```

## Nguyên tắc phụ thuộc

- `ui → service → network/db`; không phụ thuộc ngược.
- Không dùng JavaFX type trong `network`, `service` hoặc `model`.
- Không chia sẻ JAR hoặc source giữa các lab; lặp lại một helper nhỏ dễ giải thích tốt hơn tạo coupling giữa bài.
- Network/database chạy trên background executor; chỉ render trên JavaFX Application Thread.
- Resource phải đóng bằng try-with-resources hoặc lifecycle `close()` rõ ràng.

## Trạng thái UI chuẩn

Mỗi thao tác mạng có các trạng thái `idle`, `loading`, `success`, `empty`, `error`, `disconnected`. Service trả kết quả/lỗi; UI quyết định thông báo, skeleton và animation.
