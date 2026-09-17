package phanB.bai4;

public class Main {

    public static void main(String[] args) {
        Student s1 = new Student("Pho", 22, "pho@example.com");
        System.out.println("Tạo thành công: " + s1.getName() + ", " + s1.getAge() + ", " + s1.getEmail());

        // Các trường hợp không hợp lệ - từng cái
        tryCreate("  ", 20, "a@b.com");        // tên trống/toàn khoảng trắng
        tryCreate("Huynh", 15, "a@b.com");     // tuổi quá nhỏ
        tryCreate("Huynh", 101, "a@b.com");    // tuổi quá lớn
        tryCreate("Huynh", 20, "khong-co-at"); // thiếu @
        tryCreate("Huynh", 20, "a@bcom");      // có @ nhưng không có . sau đó
    }

    private static void tryCreate(String name, int age, String email) {
        try {
            new Student(name, age, email);
            System.out.println("Tạo thành công (không mong đợi!): " + name);
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi bắt được: " + e.getMessage());
        }
    }
}
