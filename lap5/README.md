# Lab 5 — RMI đồng bộ dữ liệu hai Server

## Bộ tài liệu

[Yêu cầu](REQUIREMENTS.md) · [Kiến trúc](ARCHITECTURE.md) · [Feature Map](FEATURE_MAP.md) · [Contract RMI](PROTOCOL.md) · [Demo](DEMO.md) · [Kiểm thử](TESTING.md) · [Xử lý lỗi](TROUBLESHOOTING.md)

> Trạng thái: chỉ có tài liệu chuẩn bị. Mã nguồn sẽ do thành viên nhóm push sau; chưa có lệnh build/chạy chính thức.

## Mục tiêu

Hai Java RMI Server chạy trên hai máy thật và đồng bộ dữ liệu. Hai client chạy trên hai máy ảo, có thể thao tác và kiểm tra dữ liệu qua các server.

## Topology dự kiến

```text
VM Client 1 -> Máy thật A: RMI Server A <-> Máy thật B: RMI Server B <- VM Client 2
```

## Khi code được đưa lên

1. Đối chiếu `REQUIREMENTS.md` và cập nhật quyết định còn để mở.
2. Chuẩn hóa Maven/JDK/cấu trúc theo `COMMON_ARCHITECTURE.md`.
3. Điền class/method thật vào `FEATURE_MAP.md`.
4. Chốt registry/object port, remote interface và serialization trong `PROTOCOL.md`.
5. Viết integration test, script bốn máy và demo xung đột/mất kết nối.
