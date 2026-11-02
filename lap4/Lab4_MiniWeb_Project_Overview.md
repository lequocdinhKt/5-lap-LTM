# Tổng quan dự án Lab 4 --- Mini Web Server và Mini Web Browser bằng Java

> Tài liệu bàn giao bối cảnh dự án để tiếp tục triển khai bằng Codex.\
> Ngôn ngữ trao đổi và báo cáo: tiếng Việt.\
> Ưu tiên: code dễ hiểu, dễ demo, dễ giải thích khi giảng viên hỏi.

## 1. Mục tiêu đề tài

Xây dựng một hệ thống gồm **Mini Web Server** và **Mini Web Browser (Web
Client)** bằng Java. Hai thành phần giao tiếp bằng giao thức **HTTP/1.1
chạy trên TCP Socket**.

Điểm bắt buộc của bài tập là **tự lập trình việc trao đổi và phân tích
HTTP trên TCP Socket**, không dùng `HttpServer`, Spring Boot, Apache
HttpClient, OkHttp hoặc thư viện HTTP cấp cao để thay thế phần xử lý
giao thức.

Tên đề tài gợi ý:

**Xây dựng hệ thống Web Server và Web Client sử dụng giao thức HTTP trên
nền TCP Socket bằng Java.**

## 2. Phạm vi chức năng

### 2.1. Mini Web Server

-   Mở TCP server tại cổng mặc định `8080` bằng `java.net.ServerSocket`.
-   Chấp nhận kết nối từ client.
-   Đọc và phân tích HTTP request: request line, headers và body.
-   Hỗ trợ tối thiểu ba phương thức:
    -   `GET`: lấy trang HTML hoặc tài nguyên.
    -   `HEAD`: trả headers tương ứng với GET nhưng không gửi response
        body.
    -   `POST`: nhận dữ liệu biểu mẫu gửi đến endpoint `/submit`.
-   Phục vụ nhiều loại tài nguyên tĩnh:
    -   HTML: `text/html; charset=UTF-8`
    -   PDF: `application/pdf`
    -   MP3: `audio/mpeg`
    -   MP4: `video/mp4`
-   Trả các status code cơ bản:
    -   `200 OK`
    -   `400 Bad Request` khi request không hợp lệ (nếu xử lý được)
    -   `404 Not Found`
    -   `405 Method Not Allowed`
    -   `500 Internal Server Error` khi có lỗi nội bộ
-   Trả `Content-Length` chính xác theo số byte của response body.
-   Dùng `Connection: close`; mỗi request được xử lý trên một kết nối
    rồi đóng.
-   Có log console đơn giản để quan sát method, path và status.
-   Nên xử lý nhiều kết nối bằng `ExecutorService` với thread pool có
    giới hạn.

### 2.2. Mini Web Browser / Client

Tạo giao diện tối giản bằng **Java Swing**. Đây không phải trình duyệt
hiện đại đầy đủ; chỉ cần đáp ứng mục tiêu thực hành HTTP.

-   Ô nhập URL, ví dụ `http://localhost:8080/`.
-   Nút `Go` để gửi request `GET`.
-   Nút `HEAD` để xem status và headers.
-   Chức năng `POST`: nhập tên và nội dung rồi gửi đến `/submit`.
-   Khu vực hiển thị HTML cơ bản.
-   Hiển thị HTTP status và/hoặc response headers để hỗ trợ demo.
-   Lịch sử truy cập (History), tối thiểu có thể chọn URL cũ để mở lại.
-   Back/Forward là chức năng nên có nếu không làm tăng độ phức tạp quá
    nhiều.
-   Download tài nguyên nhị phân bằng cách lưu response body vào file,
    dùng `JFileChooser`.
-   Xử lý lỗi kết nối, URL không hợp lệ và lỗi response bằng thông báo
    dễ hiểu.

**Giới hạn có chủ đích:** `JEditorPane` chỉ hiển thị HTML đơn giản,
không phải trình duyệt đầy đủ và không hỗ trợ đầy đủ JavaScript/CSS hiện
đại. PDF, MP3, MP4 có thể được tải xuống; không cần tự xây dựng trình
xem PDF hoặc trình phát media.

### 2.3. Không làm trong phiên bản đầu

Để giữ dự án đơn giản, không triển khai: - HTTPS/TLS. - JavaScript
engine hoặc trình duyệt tương đương Chrome. - HTTP keep-alive. -
`Transfer-Encoding: chunked`. - Hỗ trợ mọi loại HTTP header và mọi
trường hợp trong đặc tả HTTP. - Đăng nhập, phân quyền hoặc database. -
Streaming video/audio nâng cao. - Spring Boot hay framework web. - Tự
động mở website Internet tùy ý; ưu tiên kiểm thử với server do dự án
quản lý.

## 3. Công nghệ và ràng buộc

-   Java 17 trở lên (ưu tiên JDK 17 hoặc 21).
-   Java Socket API: `ServerSocket`, `Socket`, `InputStream`,
    `OutputStream`.
-   Java Swing cho giao diện client.
-   Không dùng thư viện HTTP cấp cao để phân tích/gửi request hoặc tạo
    response.
-   Có thể dùng các thư viện chuẩn của JDK cho tiện ích không thay thế
    HTTP, ví dụ `Files`, `Path`, `JFileChooser`, `ExecutorService`,
    `URI`, `URLDecoder`.
-   Không bắt buộc Maven/Gradle; có thể bắt đầu với dự án Java đơn giản
    trong IntelliJ IDEA hoặc Eclipse.
-   Tên lớp, biến và comment trong code nên dễ hiểu; phần giải thích và
    báo cáo viết bằng tiếng Việt.

## 4. Kiến trúc và luồng xử lý

### 4.1. Luồng tổng thể

1.  Người dùng nhập URL trong Mini Browser.
2.  Client phân tích URL, lấy host, port và path.
3.  Client tạo TCP `Socket` tới server.
4.  Client tự tạo và gửi HTTP request bằng byte.
5.  Server nhận kết nối qua `ServerSocket.accept()`.
6.  Server đọc header đến ranh giới `\r\n\r\n`.
7.  Server phân tích request line và headers.
8.  Nếu có request body, đọc đúng số byte theo `Content-Length`.
9.  Server xử lý method/path, đọc file hoặc xử lý POST.
10. Server tạo status line, headers và response body.
11. Server ghi response qua `OutputStream`.
12. Client phân tích status line, headers và body.
13. Client hiển thị HTML hoặc metadata; nếu download thì lưu body thành
    file.
14. Hai đầu đóng socket.

### 4.2. Quy tắc kỹ thuật quan trọng

-   TCP là **luồng byte**, không bảo đảm một lần `read()` nhận được trọn
    một HTTP request.
-   Không giả định một lần `read()` tương ứng với một thông điệp HTTP
    hoàn chỉnh.
-   Phải đọc header đến `\r\n\r\n` và xử lý body riêng.
-   Dùng `Content-Length` để xác định số byte body cần đọc trong phạm vi
    bài tập.
-   Không dùng `BufferedReader` để đọc toàn bộ response rồi xử lý file
    nhị phân như văn bản. Có thể dùng cách đọc byte hoặc một bộ đọc HTTP
    tự xây dựng để giữ chính xác ranh giới header/body.
-   Header HTTP được phân cách bằng CRLF (`\r\n`); dòng trống `\r\n\r\n`
    đánh dấu hết header.
-   `Content-Length` tính theo **byte**, không phải số ký tự.
-   Với GET file nhị phân, phải giữ nguyên các byte của file.
-   Với HEAD, response phải có header thích hợp và **không có body**.
-   Khi tạo response, `Content-Length` phải khớp chính xác số byte body
    sẽ gửi. Với HEAD, có thể đặt `Content-Length` bằng kích thước body
    mà GET tương ứng sẽ trả về nhưng không gửi body.
-   Server nên chuẩn hóa/kiểm tra path để ngăn path traversal như
    `../../...`; chỉ cho phép đọc file bên trong thư mục `www`.
-   Đóng socket/stream đúng cách bằng `try-with-resources`.
-   Không để tác vụ mạng chạy lâu trực tiếp trên Swing Event Dispatch
    Thread; dùng `SwingWorker` hoặc worker thread để giao diện không bị
    treo.
-   Nếu POST gửi `application/x-www-form-urlencoded`, cần đọc body theo
    `Content-Length` và giải mã form data đúng cách.
-   Bài tập chỉ hỗ trợ một tập con rõ ràng của HTTP/1.1; ghi giới hạn
    này trong báo cáo, không tuyên bố hỗ trợ đầy đủ mọi trường hợp của
    chuẩn.

## 5. Cấu trúc thư mục đề xuất

``` text
Lab4MiniWeb/
├── src/
│   └── lab4/
│       ├── server/
│       │   ├── MiniWebServer.java
│       │   ├── ClientHandler.java
│       │   ├── HttpRequest.java
│       │   ├── HttpResponse.java
│       │   ├── FileService.java
│       │   └── MimeTypes.java
│       └── client/
│           ├── MiniBrowser.java
│           ├── RawHttpClient.java
│           ├── BrowserHistory.java
│           └── DownloadService.java
├── www/
│   ├── index.html
│   └── files/
│       ├── document.pdf
│       ├── music.mp3
│       └── video.mp4
└── README.md
```

Đây là cấu trúc dự kiến, không phải yêu cầu bắt buộc phải tạo tất cả lớp
ngay từ đầu. Có thể gộp lớp nếu thực sự giúp đơn giản hóa, nhưng cần giữ
phần giao thức HTTP dễ kiểm thử.

### Vai trò các lớp

  -----------------------------------------------------------------------
  Lớp                                 Trách nhiệm
  ----------------------------------- -----------------------------------
  `MiniWebServer`                     Khởi động `ServerSocket`, lắng nghe
                                      cổng 8080 và giao kết nối cho
                                      worker

  `ClientHandler`                     Xử lý từng kết nối, phân tích
                                      request và điều phối xử lý

  `HttpRequest`                       Lưu method, path, HTTP version,
                                      headers và body

  `HttpResponse`                      Tạo status line, headers và body
                                      phản hồi

  `FileService`                       Đọc tài nguyên từ thư mục `www`,
                                      kiểm tra path và trả dữ liệu byte

  `MimeTypes`                         Ánh xạ phần mở rộng sang MIME type

  `MiniBrowser`                       Giao diện Swing và sự kiện người
                                      dùng

  `RawHttpClient`                     Tạo TCP socket, gửi HTTP request và
                                      phân tích HTTP response

  `BrowserHistory`                    Quản lý danh sách URL và vị trí
                                      Back/Forward nếu có

  `DownloadService`                   Lưu response body ra file
  -----------------------------------------------------------------------

## 6. Ví dụ endpoint và request

Giả sử server chạy trên `localhost:8080`.

  ---------------------------------------------------------------------------------
  URL/Endpoint            Method                  Kết quả dự kiến
  ----------------------- ----------------------- ---------------------------------
  `/`                     GET                     Trả `www/index.html`, `200 OK`

  `/files/document.pdf`   GET                     Trả PDF,
                                                  `Content-Type: application/pdf`

  `/files/music.mp3`      GET                     Trả MP3,
                                                  `Content-Type: audio/mpeg`

  `/files/video.mp4`      GET                     Trả MP4,
                                                  `Content-Type: video/mp4`

  `/files/document.pdf`   HEAD                    Trả headers, không có body

  `/submit`               POST                    Nhận dữ liệu form và trả
                                                  trang/thông báo kết quả

  `/missing.pdf`          GET                     `404 Not Found`

  `/`                     PUT                     `405 Method Not Allowed`
  ---------------------------------------------------------------------------------

Ví dụ request GET tối thiểu:

``` http
GET / HTTP/1.1
Host: localhost:8080
Connection: close
```

Ví dụ POST:

``` http
POST /submit HTTP/1.1
Host: localhost:8080
Content-Type: application/x-www-form-urlencoded
Content-Length: 25
Connection: close

name=An&message=Hello
```

Khi triển khai, phải tính `Content-Length` theo byte thực tế của body,
không sao chép một giá trị cố định từ ví dụ nếu body thay đổi.

## 7. Kế hoạch triển khai theo giai đoạn

### Giai đoạn 1 --- TCP Server và GET HTML

-   Tạo `MiniWebServer`.
-   Lắng nghe cổng 8080.
-   Nhận một kết nối.
-   Phân tích request GET tối thiểu.
-   Trả trang HTML mẫu.
-   Kiểm thử bằng Chrome/Firefox hoặc `curl`.

### Giai đoạn 2 --- Hoàn thiện HTTP Server

-   Tách phần đọc request và tạo response.
-   Bổ sung HEAD và POST.
-   Phục vụ HTML/PDF/MP3/MP4.
-   Thêm MIME type, `Content-Length`, `Connection: close`.
-   Thêm 404, 405 và xử lý lỗi cơ bản.
-   Bổ sung thread pool giới hạn để xử lý nhiều client.
-   Kiểm tra path traversal.

### Giai đoạn 3 --- Raw HTTP Client

-   Tạo `RawHttpClient` dùng `Socket`.
-   Gửi GET, HEAD và POST.
-   Tự phân tích status line, headers và body.
-   Xử lý response có body nhị phân.
-   Kiểm thử bằng console trước khi làm giao diện.

### Giai đoạn 4 --- Mini Browser Swing

-   URL bar và nút Go.
-   Hiển thị HTML cơ bản.
-   Nút HEAD và giao diện gửi POST.
-   Hiển thị status/headers.
-   History và Back/Forward nếu khả thi.
-   Download bằng `JFileChooser`.
-   Dùng worker thread để không khóa giao diện.

### Giai đoạn 5 --- Kiểm thử, README và báo cáo

-   Chạy toàn bộ test case.
-   Ghi kết quả thực tế, chụp ảnh màn hình.
-   Viết hướng dẫn build/run.
-   Viết báo cáo giải thích TCP, HTTP, kiến trúc và giới hạn.

**Nguyên tắc làm việc:** Hoàn thành và kiểm thử từng giai đoạn trước khi
chuyển sang giai đoạn tiếp theo. Không tạo toàn bộ code trong một lần
nếu chưa kiểm chứng từng phần.

## 8. Test case dự kiến

  --------------------------------------------------------------------------
  ID                      Kiểm thử                Kết quả mong đợi
  ----------------------- ----------------------- --------------------------
  TC01                    GET `/`                 `200 OK`, HTML hiển thị

  TC02                    HEAD tới PDF            `200 OK`, headers hợp lệ,
                                                  không body

  TC03                    POST dữ liệu hợp lệ tới Server đọc dữ liệu và trả
                          `/submit`               phản hồi

  TC04                    GET PDF                 Byte trả về đúng, MIME
                                                  `application/pdf`

  TC05                    GET MP3                 MIME `audio/mpeg`, file
                                                  không bị hỏng

  TC06                    GET MP4                 MIME `video/mp4`, file
                                                  không bị hỏng

  TC07                    Download từ Mini        File được lưu và có thể mở
                          Browser                 

  TC08                    History                 URL truy cập được lưu và
                                                  mở lại

  TC09                    GET đường dẫn không tồn `404 Not Found`
                          tại                     

  TC10                    Gửi PUT                 `405 Method Not Allowed`

  TC11                    Hai client gửi request  Server xử lý được cả hai
                          gần đồng thời           

  TC12                    URL không hợp lệ hoặc   Client hiển thị lỗi rõ
                          server không chạy       ràng

  TC13                    Request POST có body    Đọc đúng số byte body

  TC14                    Đường dẫn cố truy cập   Bị từ chối, không đọc file
                          ngoài `www`             bên ngoài
  --------------------------------------------------------------------------

Kết quả thực tế chỉ được ghi sau khi chạy kiểm thử; không đánh dấu tất
cả là thành công trước khi xác minh.

## 9. Kịch bản demo trước giảng viên

1.  Chạy `MiniWebServer`, cho thấy server lắng nghe tại cổng 8080.
2.  Chạy Mini Browser và truy cập `http://localhost:8080/`.
3.  Cho thấy trang HTML được trả về qua HTTP.
4.  Mở chức năng HEAD, cho thấy status và headers.
5.  Gửi POST từ biểu mẫu và chỉ ra dữ liệu server nhận được.
6.  Tải PDF, MP3 và MP4; xác minh file tải xuống có thể mở.
7.  Truy cập một đường dẫn không tồn tại để chứng minh 404.
8.  Truy cập một số URL rồi dùng History/Back nếu chức năng đã hoàn
    thiện.
9.  Giải thích request đi từ client tới server qua TCP Socket, rồi
    response đi ngược lại.

## 10. Dàn ý báo cáo

1.  **Giới thiệu đề tài:** mục tiêu, phạm vi và lý do chọn Java Socket.
2.  **Cơ sở lý thuyết:** TCP Socket, HTTP request/response, GET, HEAD,
    POST, status code, MIME type.
3.  **Phân tích và thiết kế:** mô hình client-server, luồng xử lý, sơ đồ
    lớp và cấu trúc thư mục.
4.  **Cài đặt:** mô tả các lớp quan trọng, cách đọc request và tạo
    response.
5.  **Kiểm thử:** bảng test case, ảnh minh chứng, kết quả thực tế.
6.  **Kết luận:** yêu cầu đạt được, hạn chế và hướng phát triển.

## 11. Hướng dẫn dành cho Codex

Khi tiếp tục dự án, hãy đọc file này trước và làm theo các nguyên tắc
sau:

1.  Kiểm tra cấu trúc dự án và các file đã tồn tại trước khi sửa.
2.  Không giả định code đã có; nếu chưa có, tạo từng phần theo giai
    đoạn.
3.  Giữ dự án đơn giản, dùng Java Socket API và Swing; không đưa
    framework web hoặc thư viện HTTP cấp cao vào.
4.  Giải thích ngắn gọn mục đích của mỗi lớp và những quyết định kỹ
    thuật quan trọng.
5.  Ưu tiên code rõ ràng, tên biến dễ hiểu và comment tiếng Việt ở các
    đoạn xử lý giao thức khó.
6.  Không gộp toàn bộ chương trình vào một file lớn.
7.  Không dùng `BufferedReader` để xử lý toàn bộ response có thể chứa dữ
    liệu nhị phân.
8.  Tính `Content-Length` bằng số byte; bảo toàn byte của file.
9.  Xử lý ranh giới header/body và `Content-Length` cẩn thận; không giả
    định một lần `read()` nhận đủ request.
10. Giữ xử lý mạng khỏi Swing Event Dispatch Thread.
11. Không báo test thành công nếu chưa chạy test.
12. Khi hoàn tất một giai đoạn, hãy báo: file đã tạo/sửa, chức năng hoàn
    thành, lệnh chạy, cách kiểm thử và vấn đề còn tồn tại.
13. Nếu cần thay đổi phạm vi hoặc thêm thư viện, hãy giải thích lý do
    trước.
14. Đảm bảo server chỉ phục vụ file trong thư mục `www`; kiểm tra path
    traversal.
15. Bắt đầu bằng **Giai đoạn 1: TCP Server trả HTML qua HTTP GET**, chưa
    làm GUI cho đến khi GET chạy ổn định.

## 12. Tiêu chí hoàn thành tối thiểu

-   [ ] Server tự xử lý HTTP trên TCP Socket.
-   [ ] GET, HEAD, POST hoạt động theo phạm vi đã mô tả.
-   [ ] Server trả HTML và các file PDF/MP3/MP4.
-   [ ] Client tự gửi và phân tích HTTP bằng TCP Socket.
-   [ ] Mini Browser có giao diện Swing.
-   [ ] History và download hoạt động.
-   [ ] Có xử lý lỗi 404/405 và lỗi kết nối cơ bản.
-   [ ] Các file nhị phân tải xuống không bị thay đổi.
-   [ ] Có hướng dẫn chạy và bộ test case.
-   [ ] Báo cáo có sơ đồ kiến trúc, giải thích giao thức và ảnh demo.

------------------------------------------------------------------------

**Trạng thái hiện tại:** Đây là tài liệu thiết kế tổng quan, chưa khẳng
định chương trình đã được lập trình hoặc kiểm thử. Bắt đầu triển khai từ
Giai đoạn 1 và cập nhật trạng thái thực tế trong `README.md` khi tiến
triển.
