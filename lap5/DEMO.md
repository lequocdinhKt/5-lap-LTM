# Demo Lab 5 — kịch bản dự kiến

## Topology

- Máy thật A: Server A + storage A.
- Máy thật B: Server B + storage B.
- VM 1: Client 1 kết nối Server A.
- VM 2: Client 2 kết nối Server B.

## Kịch bản mục tiêu

1. Khởi động hai registry/server, chỉ địa chỉ và remote service name.
2. Client 1 tạo dữ liệu ở A; Client 2 đọc dữ liệu đó từ B.
3. Client 2 cập nhật ở B; kiểm tra kết quả ở A nếu thiết kế hai chiều.
4. Tắt Server B, thao tác ở A và giải thích trạng thái pending/failure.
5. Bật lại B, chứng minh đồng bộ hội tụ.
6. Thực hiện hai update cạnh tranh và chỉ chiến lược xung đột.

## Câu hỏi giảng viên có thể hỏi

- RMI registry, stub và remote object khác nhau thế nào?
- Vì sao DTO phải Serializable?
- `RemoteException` xuất hiện ở đâu và xử lý thế nào?
- Hai server thật sự độc lập dữ liệu bằng cách nào?
- Khi hai máy cùng cập nhật, server nào thắng và vì sao?
- Khi một server offline, dữ liệu được retry hay rollback?
- Method nào thực hiện replication?

Các câu trả lời chi tiết chỉ hoàn thiện sau khi có code thật.
