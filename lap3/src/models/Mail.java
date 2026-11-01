package models;

public class Mail {
    public String sender;
    public String receiver;
    public String subject;
    public String content;

    // Tạo một đối tượng thư điện tử.
    // Tham số: sender - người gửi; receiver - người nhận; subject - tiêu đề; content - nội dung thư.
    // Trả về: không có vì đây là phương thức khởi tạo.
    public Mail(String sender, String receiver, String subject, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.subject = subject;
        this.content = content;
    }
}
