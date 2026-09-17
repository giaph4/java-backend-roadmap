package phanB.bai8;

class Animal {
    static { System.out.println("static Animal"); }
    { System.out.println("instance Animal"); }
    Animal() { System.out.println("ctor Animal"); }
}

class Dog extends Animal {
    static { System.out.println("static Dog"); }
    { System.out.println("instance Dog"); }
    Dog() { System.out.println("ctor Dog"); }
}

public class Main {
    public static void main(String[] args) {
        // Dự đoán output:
        // static Animal      <- static init block của lớp cha, chạy TRƯỚC, chỉ 1 lần khi class được load
        // static Dog         <- static init block của lớp con, cũng chỉ 1 lần khi class được load
        // instance Animal    <- instance init block cha, chạy mỗi lần new Dog()
        // ctor Animal        <- constructor cha, luôn chạy trước instance init + ctor của con
        // instance Dog
        // ctor Dog
        // ----
        // instance Animal    <- lần new Dog() thứ 2: KHÔNG có "static ..." nữa vì class đã được load rồi
        // ctor Animal
        // instance Dog
        // ctor Dog
        new Dog();
        System.out.println("----");
        new Dog();

        System.out.println();
        System.out.println("Giải thích:");
        System.out.println("- static block/field chỉ chạy ĐÚNG 1 LẦN khi class được JVM LOAD lần đầu (class loading),");
        System.out.println("  không phụ thuộc số lần gọi 'new' - đó là lý do 'static Animal'/'static Dog' chỉ in ra 1 lần");
        System.out.println("  dù new Dog() được gọi 2 lần. Thứ tự: cha trước con (Animal cần được load trước vì Dog extends Animal).");
        System.out.println("- Mỗi lần 'new', instance init block + constructor chạy LẠI từ đầu, theo thứ tự:");
        System.out.println("  instance block cha -> constructor cha -> instance block con -> constructor con");
        System.out.println("  (constructor con luôn ngầm gọi super() TRƯỚC khi chạy thân của chính nó,");
        System.out.println("  và instance init block của một class luôn chạy TRƯỚC thân constructor của chính class đó).");
    }
}
