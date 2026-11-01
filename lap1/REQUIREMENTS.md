# Yêu cầu Lab 1 — Chat

## Bắt buộc theo đề

- Giao diện thân thiện; chạy được 5–6 client hoặc peer.
- Có broadcast và multicast; giải thích được tình huống dùng TCP/UDP.
- Có emoji, text, audio, video và đính kèm.
- Có database là điểm cộng.

## Phạm vi đã chọn

- Mô hình Client–Server, tài khoản MySQL.
- TCP: đăng nhập/đăng ký, danh sách online, chat riêng, quản lý nhóm và signaling file.
- UDP multicast: chat nhóm; UDP broadcast: chat “Tất cả”.
- UDP unicast ACK/retry: file/audio/video tối đa 20 MB.

## Tiêu chí nghiệm thu

- 5–6 client chạy phân bố trên hai máy và cùng quan sát được trạng thái online.
- Demo chat riêng, chat nhóm, chat tất cả, emoji và một file media.
- Tắt một client không làm server hoặc client khác dừng.
- `ChatIntegrationTest` vượt qua và tài liệu trỏ đúng method hiện tại.

## Ngoài phạm vi có chủ đích

- Không mã hóa end-to-end; không lưu nhóm và lịch sử chat lâu dài.
- UDP text không đảm bảo tới nơi; đây là hành vi cần giải thích, không phải lỗi phải che giấu.
