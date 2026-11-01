package models;

public class User {
    public String username;
    public String password;

    // Tạo một đối tượng người dùng.
    // Tham số: username - tên đăng nhập; password - mật khẩu dạng văn bản thường.
    // Trả về: không có vì đây là phương thức khởi tạo.
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
