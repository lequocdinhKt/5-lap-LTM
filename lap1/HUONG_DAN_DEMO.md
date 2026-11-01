# Hướng dẫn Demo Lab 1 - Chat TCP/UDP

Kịch bản trình bày từng bước để chứng minh đủ các mô hình truyền thông: **TCP**, **UDP multicast**, **UDP broadcast**, **UDP unicast có ACK/retry**, cùng lưu trữ MySQL và quản lý nhóm.

## 0. Chuẩn bị trước khi demo

### 0.1. Chuẩn bị môi trường và MySQL

```powershell
# Chạy ở thư mục root 5lap trong lần cài đầu
.\setup-windows.ps1
```

### 0.2. Biên dịch (nếu chưa làm)

```powershell
.\build.ps1
```

### 0.3. Mở sẵn các cửa sổ

Chuẩn bị **4 cửa sổ terminal**:

| Cửa sổ | Vai trò |
|--------|---------|
| T1 | Server |
| T2 | Client `An` |
| T3 | Client `Binh` |
| T4 | Client `Chi` |

### 0.4. Khởi động server (T1)

```powershell
.\run-server.ps1
```

Trong cửa sổ server:

1. Ghi lại **IP LAN** hiển thị (ví dụ `192.168.1.10`).
2. Ghi lại ba cổng: TCP `5000`, UDP text `5002`, UDP file `5001`.
3. Bấm **Khởi động server**. Trạng thái chuyển sang **Đang chạy**.

### 0.5. Khởi động 3 client (T2, T3, T4)

```powershell
# Chạy ở ba terminal, rồi nhập An/Binh/Chi trong giao diện
.\run-client.ps1
```

> Lần đầu chạy, mỗi tài khoản sẽ được tự đăng ký vào MySQL. Lần sau dùng đúng mật khẩu đã đặt.

Sau khi cả ba đăng nhập, cửa sổ server hiển thị **Client online: 3** và bảng có `An`, `Binh`, `Chi`.

---

## 1. Demo TCP - Đăng nhập và danh sách online

**Mục tiêu:** Chứng minh TCP giữ kết nối lâu dài và đồng bộ trạng thái.

1. Chỉ vào cửa sổ server: bảng client có đủ 3 người, cột IP hiển thị đúng.
2. Trên client `An`, quan sát danh sách người online bên trái: thấy `Binh`, `Chi`.
3. Đóng cửa sổ client `Chi`.
4. Trên server và trên client `An`, danh sách cập nhật còn 2 người.
5. Mở lại client `Chi`, danh sách trở lại 3 người.

**Điểm nhấn khi thuyết trình:**

- TCP đảm bảo thứ tự và không mất sự kiện.
- Server giữ một `Socket` cho mỗi client trong `ClientRegistry`.
- Khi socket đóng, server phát sự kiện `onClientsChanged` tới mọi client còn lại.

---

## 2. Demo TCP - Chat riêng 1-1 (DIRECT)

**Mục tiêu:** Chứng minh tin nhắn riêng chỉ tới đúng người nhận qua TCP.

1. Trên client `An`, chọn chế độ **Gửi riêng** (DIRECT).
2. Chọn người nhận `Binh`.
3. Gõ: `Xin chào Binh, đây là tin riêng qua TCP.` và gửi.
4. Quan sát:
   - `An` thấy tin của mình.
   - `Binh` nhận được tin.
   - `Chi` **không** thấy tin này.
5. Trên server, log ghi nhận sự kiện gửi tin DIRECT.

**Điểm nhấn:**

- Tin DIRECT đi qua kết nối TCP của từng người nhận.
- Server tra `ClientRegistry.find(username)` rồi ghi xuống `DataOutputStream` của người đó.
- Nếu người nhận offline, server trả lỗi cho người gửi (thử gửi cho một tên đã đóng).

---

## 3. Demo UDP Multicast - Chat nhóm

**Mục tiêu:** Chứng minh UDP multicast chỉ tới thành viên nhóm.

1. Trên client `An`, bấm **Tạo nhóm**.
2. Đặt tên nhóm: `Nhóm Java`.
3. Chọn thành viên: `Binh` (không chọn `Chi`).
4. Xác nhận tạo.
5. Quan sát:
   - `An` và `Binh` thấy nhóm mới trong danh sách.
   - `Chi` **không** thấy nhóm này.
6. Trên client `An`, mở nhóm `Nhóm Java`, gõ: `Chào nhóm, đây là UDP multicast.` và gửi.
7. Quan sát:
   - `An` và `Binh` nhận tin nhóm.
   - `Chi` không nhận.
8. Trên server, log ghi nhận multicast tới địa chỉ `239.10.x.x` của nhóm.

**Điểm nhấn:**

- Mỗi nhóm được cấp một địa chỉ multicast riêng (`GroupRegistry`).
- Client `joinGroup` vào địa chỉ đó; chỉ thành viên mới nhận.
- UDP multicast là **best effort**: không ACK, có thể mất gói nếu mạng yếu.

---

## 4. Demo UDP Broadcast - Nhóm "Tất cả"

**Mục tiêu:** Chứng minh broadcast tới mọi client online.

1. Trên client `An`, mở nhóm **Tất cả** (nhóm hệ thống có sẵn).
2. Gõ: `Xin chào tất cả, đây là UDP broadcast.` và gửi.
3. Quan sát: cả `An`, `Binh`, `Chi` đều nhận tin.
4. Trên server, log ghi nhận broadcast tới địa chỉ broadcast LAN và loopback.

**Điểm nhấn:**

- Nhóm "Tất cả" dùng địa chỉ broadcast của interface LAN và thêm loopback để máy đơn vẫn demo được.
- `UdpTextBroadcaster` gửi hai bản: một cho LAN, một cho loopback.
- Broadcast không định tuyến qua Internet, chỉ trong LAN.

---

## 5. Demo UDP Unicast có ACK/retry - Gửi file

**Mục tiêu:** Chứng minh chia gói, ACK và truyền lại khi mất gói.

### 5.1. Gửi file riêng

1. Trên client `An`, chọn chế độ **Gửi riêng**, chọn `Binh`.
2. Bấm biểu tượng đính kèm, chọn một file nhỏ (ảnh, PDF, txt).
3. Gửi.
4. Quan sát:
   - `Binh` nhận thông báo file và có thể mở thư mục lưu.
   - `Chi` không nhận.
5. So sánh file gốc và file nhận (kích thước, nội dung).

### 5.2. Gửi file vào nhóm

1. Trên client `An`, mở nhóm `Nhóm Java`.
2. Đính kèm một file audio hoặc video ngắn.
3. Gửi.
4. Quan sát `Binh` nhận file; `Chi` không nhận.

### 5.3. Chứng minh giới hạn 20 MB

1. Tạo một file lớn hơn 20 MB (ví dụ `truncate -s 21M big.bin`).
2. Thử gửi.
3. Client báo lỗi ngay: file vượt giới hạn 20 MB.

**Điểm nhấn:**

- File được chia thành các chunk 4 KB (`Protocol.UDP_CHUNK_SIZE`).
- Mỗi chunk có ACK; nếu không nhận ACK trong thời gian chờ, gửi lại tối đa 8 lần.
- Server làm relay: sender → server → receiver, mỗi chặng đều ACK.
- File lưu vào thư mục `received/` phía người nhận.

---

## 6. Demo quản lý nhóm

**Mục tiêu:** Chứng minh sửa thành viên, rời nhóm, xóa nhóm.

### 6.1. Sửa thành viên

1. Trên client `An` (trưởng nhóm), mở `Nhóm Java`, chọn **Sửa thành viên**.
2. Bỏ `Binh`, thêm `Chi`.
3. Gửi một tin nhóm mới.
4. Quan sát: `Chi` nhận, `Binh` không nhận nữa.

### 6.2. Rời nhóm

1. Trên client `Chi`, mở `Nhóm Java`, chọn **Rời nhóm**.
2. Gửi tin nhóm từ `An`.
3. Quan sát `Chi` không nhận.

### 6.3. Xóa nhóm

1. Trên client `An`, chọn **Xóa nhóm**.
2. Quan sát nhóm biến mất trên mọi client từng là thành viên.

**Điểm nhấn:**

- Server cập nhật `GroupRegistry`, gửi lại catalog qua TCP.
- Client join/leave địa chỉ multicast tương ứng.
- Nhóm chỉ lưu RAM: khởi động lại server sẽ mất nhóm nhưng giữ tài khoản MySQL.

---

## 7. Demo lưu trữ MySQL

**Mục tiêu:** Chứng minh tài khoản lưu bền vững, nhóm không bền vững.

1. Dừng server (bấm **Dừng server**).
2. Khởi động lại server.
3. Đăng nhập lại bằng `An` với đúng mật khẩu: thành công (tài khoản còn trong MySQL).
4. Quan sát danh sách nhóm: chỉ còn nhóm hệ thống **Tất cả**, nhóm `Nhóm Java` đã mất.

Kiểm tra MySQL:

```sql
USE lab1_chat;
SELECT username, created_at FROM users;
```

**Điểm nhấn:**

- Bảng `users` lưu `username`, `password_hash` (SHA-256), `created_at`.
- Nhóm không có bảng: chỉ tồn tại trong `GroupRegistry` (RAM).

---

## 8. Demo bot tự động (tuỳ chọn)

Nếu muốn minh họa nhanh không cần thao tác tay:

1. Giữ client `An` đang online.
2. Chạy bot:

```powershell
.\mvnw.cmd -q compile exec:java "-Dexec.mainClass=lab1.test.LiveDemoBot" `
  "-Dexec.args=localhost 5000 An"
```

3. Bot tự:
   - Đăng nhập với tên `BotDemo`.
   - Gửi tin DIRECT cho `An`.
   - Gửi tin broadcast vào nhóm **Tất cả**.
   - Tạo một nhóm mới mời `An`.
4. Quan sát các tin nhắn xuất hiện trên client `An`.

---

## 9. Demo kiểm thử tích hợp (tuỳ chọn nâng cao)

Chạy toàn bộ test tự động để chứng minh độ đúng đắn:

```powershell
.\mvnw.cmd -q compile exec:java "-Dexec.mainClass=lab1.test.ChatIntegrationTest"
```

Test sẽ in từng dòng `[PASS]` và kết thúc bằng:

```text
TẤT CẢ KIỂM THỬ TÍCH HỢP ĐÃ THÀNH CÔNG.
```

Các kịch bản được kiểm tra:

- 6 client đăng ký/đăng nhập MySQL và kết nối đồng thời.
- Từ chối tên đang online.
- DIRECT chỉ tới người gửi và một người nhận.
- Báo lỗi khi người nhận offline.
- Trưởng nhóm chọn thành viên khi tạo nhóm.
- UDP multicast chỉ tới thành viên được chọn.
- Join/leave multicast thay đổi đúng khi sửa thành viên.
- Audio nhóm UDP đúng byte và đúng người nhận.
- Nhóm "Tất cả" broadcast tới đủ 6 client.
- Xóa nhóm biến mất trên mọi client.
- Truyền file UDP đúng byte và đúng người nhận.
- Từ chối file vượt 20 MB.
- Khởi động lại giữ tài khoản MySQL nhưng làm mới nhóm RAM.

---

## 10. Câu hỏi thường gặp khi bảo vệ

| Câu hỏi | Trả lời ngắn |
|---------|--------------|
| Vì sao chat riêng dùng TCP mà chat nhóm dùng UDP? | TCP đảm bảo tin riêng không mất; UDP multicast tiết kiệm băng thông khi gửi nhiều người. |
| Vì sao file dùng UDP mà vẫn đúng byte? | Chia chunk 4 KB + ACK từng chunk + retry tối đa 8 lần. |
| Vì sao server làm relay file? | Client sau NAT không mở được cổng; server trung gian nhận rồi chuyển tiếp. |
| Nhóm lưu ở đâu? | RAM (`GroupRegistry`), mất khi server dừng. |
| Tài khoản lưu ở đâu? | MySQL bảng `users`, băm SHA-256. |
| Multicast khác broadcast thế nào? | Multicast chỉ thành viên join nhóm nhận; broadcast mọi máy trong LAN nhận. |
| Vì sao có cả LAN broadcast và loopback? | Để demo được cả nhiều máy lẫn một máy đơn. |
