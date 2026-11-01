# Yêu cầu Lab 5 — RMI

## Bắt buộc theo đề

- Dùng Java RMI để đồng bộ dữ liệu trên hai server.
- Hai server chạy trên hai máy thật.
- Các client chạy trên máy ảo; topology nhóm chọn hai VM client.

## Cần chốt sau khi có code

- Loại dữ liệu được đồng bộ và thao tác client.
- Registry port, remote object port và cách quảng bá hostname.
- Lưu RAM, file hay database ở từng server.
- Đồng bộ chủ động/bị động, hai chiều hay primary–replica.
- Version dữ liệu, xử lý xung đột, retry và hành vi khi một server offline.

## Tiêu chí nghiệm thu dự kiến

- Client ghi qua Server A và đọc được kết quả từ Server B, và ngược lại nếu thiết kế hai chiều.
- Hai server có storage độc lập; không dùng chung folder/database để giả lập đồng bộ.
- Tắt một server không làm mất dữ liệu đã xác nhận; khi chạy lại có cơ chế hội tụ rõ ràng.
- Demo bốn máy, test tự động và Feature Map tới method thật.
