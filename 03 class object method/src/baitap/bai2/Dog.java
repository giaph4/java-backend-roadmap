package baitap.bai2;

public class Dog extends Animal {
    // Đây KHÔNG phải override — chỉ là "hiding" vì sound() là static.
    static String sound() {
        return "Gâu";
    }
}
