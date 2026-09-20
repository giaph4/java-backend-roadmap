package baitap.bai2;

/**
 * Minh hoạ static vs instance member.
 * - name/age/gpa: instance field -> mỗi object 1 bản riêng.
 * - totalStudents: static field  -> 1 bản DUY NHẤT, dùng chung cho cả class.
 * - PASS_GPA: static final       -> hằng số dùng chung, không đổi.
 */
public class Student {

    static int totalStudents = 0;          // chia sẻ toàn class
    static final double PASS_GPA = 2.0;    // hằng số dùng chung

    String name;                            // riêng từng object
    int age;
    double gpa;

    Student(String name, int age, double gpa) {
        this.name = name;
        this.age = age;
        this.gpa = gpa;
        totalStudents++;                    // sửa "cái gắn trên khuôn" -> MỌI object đều thấy
    }

    // instance method: dùng được CẢ instance member (name, age, gpa) LẪN static member (PASS_GPA)
    void printInfo() {
        System.out.println(name + " - " + age + " tuổi - GPA " + gpa
                + (isPassing() ? " (Đạt)" : " (Chưa đạt)"));
    }

    boolean isPassing() {
        return this.gpa >= PASS_GPA;        // instance method đọc static field -> luôn OK
    }

    // static method: KHÔNG được đọc trực tiếp instance member (name/age/gpa)
    // vì lúc gọi Student.printTotal() có thể CHƯA hề có object nào tồn tại.
    static void printTotal() {
        System.out.println("Tổng số sinh viên đã tạo: " + totalStudents);
        // System.out.println(name);   // <- nếu bỏ comment: lỗi compile
        //   "non-static field name cannot be referenced from a static context"
    }
}
