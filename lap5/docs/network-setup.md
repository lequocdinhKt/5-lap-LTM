# Thiết lập mạng

Dùng Bridged Adapter cho hai VM để bốn node truy cập lẫn nhau. Chọn IPv4 của card LAN/Wi-Fi thật, không dùng loopback, VPN hoặc adapter ảo làm `server.host`.

Trên hai máy server, mở TCP `1099–1102` bằng `configure-firewall.ps1`. Kiểm tra từ peer và VM:

```powershell
Test-NetConnection <server-ip> -Port 1099
Test-NetConnection <server-ip> -Port 1100
Test-NetConnection <server-ip> -Port 1101
Test-NetConnection <server-ip> -Port 1102
```

RMI stub quảng bá `server.host`; nếu registry kết nối được nhưng gọi object lỗi, IP quảng bá hoặc port object thường đang sai.
