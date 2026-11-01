# Design System — Network Labs

## Tinh thần

Tối giản, sáng, hiện đại và dễ quan sát khi mở nhiều client. Mobbin/shadcn chỉ là cảm hứng bố cục; implementation dùng JavaFX, CSS và có thể thêm AtlantaFX/ControlsFX.

## Token chung

| Token | Giá trị | Dùng cho |
|---|---|---|
| `primary` | `#2563EB` | Nút chính, selection, link |
| `primary-dark` | `#1D4ED8` | Hover/pressed |
| `background` | `#F8FAFC` | Nền ứng dụng |
| `surface` | `#FFFFFF` | Card, sidebar, top bar |
| `text` | `#0F172A` | Chữ chính |
| `muted` | `#64748B` | Chữ phụ |
| `border` | `#E2E8F0` | Viền nhẹ |
| `success` | `#16A34A` | Đã kết nối/thành công |
| `warning` | `#F59E0B` | Đang chờ/cảnh báo |
| `danger` | `#DC2626` | Lỗi/ngắt kết nối |

- Font: Inter được bundle trong resources; fallback `Segoe UI`.
- Radius: 8 px cho input/nút, 12 px cho card, 16 px cho dialog lớn.
- Spacing: bội số 4; khoảng thường dùng 8, 12, 16, 24, 32 px.
- Chiều cao control: 36 px; vùng bấm tối thiểu 32 px.

## Khung màn hình

- Sidebar 220 px, có thể thu về 64 px.
- Top bar 56 px: tên lab, host/port, trạng thái kết nối và action chính.
- Content co giãn; không đặt kích thước tuyệt đối cho vùng dữ liệu chính.
- Ở cửa sổ hẹp, ẩn label phụ và chuyển panel hai cột thành SplitPane co giãn.

## Component và trạng thái

- Connection badge: xanh lá khi online, vàng khi connecting, đỏ khi disconnected.
- Toast: thành công 3 giây; lỗi giữ tới khi đóng hoặc có hành động khắc phục.
- Progress: hiển thị phần trăm, số byte và tốc độ nếu biết tổng dung lượng.
- Skeleton: dùng cho danh sách đang tải; không dùng spinner toàn màn hình cho thao tác nhỏ.
- Empty state: mô tả ngắn và một hành động tiếp theo.

## Animation

- Chuyển trang: fade/translate 160–220 ms.
- Hover/press: 100–140 ms.
- Badge kết nối: đổi màu 180 ms; không nhấp nháy liên tục.
- Progress: cập nhật mượt nhưng không làm nghẽn JavaFX Application Thread.
- Tuân thủ nguyên tắc: animation giải thích thay đổi trạng thái, không chỉ để trang trí.

## Quy ước từng lab

- Lab 1: chat bubble, danh sách cuộc trò chuyện và composer luôn nhìn thấy.
- Lab 2: local/remote panes cân bằng, command log có thể thu gọn.
- Lab 3: sidebar mailbox, form soạn thư và danh sách inbox rõ cấp bậc.
- Lab 4: address bar, navigation, status/header inspector và download panel.
- Lab 5: dashboard trạng thái hai server và timeline đồng bộ, thực hiện sau.
