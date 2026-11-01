# Giao thức FTP của Lab 2

## Kênh

- Control connection: TCP `2121`, tồn tại suốt phiên, dòng kết thúc CRLF.
- PASV data connection: TCP `30000-30020`, một kết nối cho một lệnh dữ liệu.
- PORT data connection: client mở port và server kết nối ngược.

## Phiên mẫu

```text
S: 220 ...
C: USER alice
S: 331 ...
C: PASS 123456
S: 230 ...
C: TYPE I
S: 200 ...
C: PASV
S: 227 Entering Passive Mode (h1,h2,h3,h4,p1,p2)
C: STOR demo.pdf
S: 150 ...
   [byte đi trên data connection]
S: 226 ...
```

## Lệnh hỗ trợ

`USER`, `PASS`, `QUIT`, `SYST`, `FEAT`, `OPTS`, `PWD`, `CWD`, `CDUP`, `TYPE`, `PASV`, `PORT`, `LIST`, `NLST`, `RETR`, `STOR`, `DELE`, `MKD`, `RMD`, `SIZE`, `NOOP`.

`FtpProtocol.parseCommand()` phân tách command/argument; `ControlHandler.execute()` và `executeAuthenticated()` là nơi dispatch. Reply dùng mã ba chữ số; client `readReply()` hỗ trợ cả reply một dòng và nhiều dòng.

## Quy tắc an toàn đường dẫn

- Server resolve đường dẫn tương đối từ home/current directory rồi normalize.
- Đường dẫn kết quả phải còn nằm trong home; không tin `..` hoặc tên từ client.
- Không theo symbolic link ra ngoài vùng FTP.

## Lỗi cần demo

- `530` khi chưa xác thực hoặc sai tài khoản.
- `425` khi chưa chuẩn bị data channel.
- `550` khi file/path không tồn tại hoặc thao tác không hợp lệ.
- `502` cho lệnh chưa hỗ trợ.
