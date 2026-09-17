package baitap.bai2;

public class Main {

    public static void main(String[] args) {

        // ===== 1) Instance field: mỗi object 1 bản riêng =====
        Student s1 = new Student("Pho", 22, 3.5);
        Student s2 = new Student("An", 20, 1.8);
        System.out.println("--- 1) Instance field riêng từng object ---");
        s1.printInfo();   // Pho ... (Đạt)
        s2.printInfo();   // An  ... (Chưa đạt)

        // ===== 2) Static field: 1 bản DUY NHẤT, chia sẻ toàn class =====
        System.out.println("\n--- 2) Static field dùng chung ---");
        Student.printTotal();          // Tổng số sinh viên đã tạo: 2
        System.out.println(Student.PASS_GPA); // 2.0 — hằng số dùng chung

        // ===== 3) Truy cập static QUA reference instance — hợp lệ nhưng gây hiểu lầm =====
        System.out.println("\n--- 3) Truy cập static qua instance reference ---");
        System.out.println(s1.totalStudents);   // biên dịch OK, nhưng thực chất là Student.totalStudents
                                                  // (IDE sẽ cảnh báo "static member accessed via instance reference")

        // static field không NPE dù reference là null — vì chỉ cần KIỂU của reference, không cần mở object
        Student s3 = null;
        System.out.println(s3.totalStudents);   // vẫn in ra 2, KHÔNG NullPointerException
        // System.out.println(s3.name);         // <- nếu bỏ comment: NullPointerException (instance field cần object thật)

        // ===== 4) static method KHÔNG bị override — chỉ bị "hiding" =====
        System.out.println("\n--- 4) static hiding (khác override) ---");
        Animal a = new Dog();               // reference kiểu Animal, object thật là Dog
        System.out.println(Animal.sound()); // "..."
        System.out.println(Dog.sound());    // "Gâu"
        // a.sound() cũng biên dịch OK nhưng compiler phân giải theo KIỂU TĨNH của "a" (Animal),
        // không theo object thật (Dog) -> in ra "..." của Animal, KHÔNG PHẢI "Gâu"

        // ===== 5) Class initialization: static block chạy khi nào =====
        System.out.println("\n--- 5) Class initialization ---");
        System.out.println("Trước khi đụng Config.MAX...");
        System.out.println("Config.MAX = " + Config.MAX);
        // Không thấy dòng ">> [Config] static block chạy" ở trên, vì MAX là hằng compile-time
        // -> compiler đã inline giá trị 100 ngay lúc biên dịch, không cần nạp class Config.

        System.out.println("Giờ truy cập Config.data (không phải hằng compile-time)...");
        System.out.println(Config.data.get("timeout"));
        // Lần này class Config MỚI thực sự được khởi tạo -> dòng ">> [Config] static block chạy" xuất hiện
    }
}
