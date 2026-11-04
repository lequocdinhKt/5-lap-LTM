# Thiết lập MySQL Server 8.x local

1. Cài và khởi động MySQL Server 8.x trên Windows; bảo đảm `mysql.exe` có trong PATH hoặc thư mục cài mặc định.
2. Chạy `setup-windows.ps1` ở root để kiểm tra kết nối và tạo `.env` cục bộ.
3. Trong `lap5`, chạy `configure-server.ps1 -Role A|B -LocalHost <IP-local> -PeerHost <IP-peer> [-SeedDemo]`.
4. Kiểm tra `server.properties` trỏ tới `jdbc:mysql://127.0.0.1:<port>/lab5_warehouse`.
5. Không mở port MySQL ra LAN: server chỉ truy cập database cục bộ; đồng bộ đi qua RMI.

Hai máy dùng cùng schema name nhưng MySQL data directory độc lập. Script integration tạo `lab5_test_a` và `lab5_test_b` tạm trên máy phát triển rồi tự xóa.
