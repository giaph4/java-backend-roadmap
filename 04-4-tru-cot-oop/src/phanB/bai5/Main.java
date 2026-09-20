package phanB.bai5;

class A {
    String who = "A";
    String whoMethod() { return "A"; }
}

class B extends A {
    String who = "B";
    @Override String whoMethod() { return "B"; }
}

public class Main {

    public static void main(String[] args) {
        A ref = new B();

        // Dự đoán trước khi chạy:
        // ref.who         -> "A"  (field KHÔNG đa hình - resolve theo kiểu TĨNH của biến "ref" là A)
        // ref.whoMethod() -> "B"  (method LÀ đa hình - resolve theo kiểu THỰC của object lúc runtime là B)
        // ((B) ref).who   -> "B"  (ép kiểu tĩnh về B -> field lấy từ B)
        System.out.println("ref.who = " + ref.who);
        System.out.println("ref.whoMethod() = " + ref.whoMethod());
        System.out.println("((B) ref).who = " + ((B) ref).who);

        System.out.println();
        System.out.println("Giải thích:");
        System.out.println("- Field truy cập qua kiểu TĨNH khai báo của biến (ở đây là A) -> ref.who luôn là field 'who' của A,");
        System.out.println("  dù object thực sự là B. Đây gọi là 'field hiding' (B.who chỉ CHE, không GHI ĐÈ A.who -");
        System.out.println("  cả 2 field cùng tồn tại song song trong bộ nhớ của object B).");
        System.out.println("- Method thông qua bảng phương thức ảo (vtable) của object THỰC SỰ lúc runtime (B) -> whoMethod() luôn là bản của B.");
        System.out.println("- Ép kiểu ((B) ref) chỉ đổi KIỂU TĨNH mà compiler dùng để resolve field, không tạo object mới ->");
        System.out.println("  vì vậy field lấy đúng theo B.");
    }
}
