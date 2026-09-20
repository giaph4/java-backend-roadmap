package phanB.bai4;

import java.util.ArrayList;
import java.util.List;

class ShoppingCart {
    private List<String> items = new ArrayList<>();

    public ShoppingCart(List<String> initial) {
        this.items.addAll(initial); // lỗi 1 đã fix: copy dữ liệu vào, không giữ tham chiếu "initial"
    }

    public void addItem(String s) {
        items.add(s);
    }

    public List<String> getItems() {
        return new ArrayList<>(items); // lỗi 2 đã fix: trả bản sao, không trả thẳng field
    }
}

public class Main {

    public static void main(String[] args) {
        List<String> external = new ArrayList<>(List.of("Sách", "Bút"));
        ShoppingCart cart = new ShoppingCart(external);

        // Tấn công 1: sửa list gốc "external" sau khi truyền vào constructor
        external.add("Hack qua constructor");
        System.out.println("Sau khi sửa list gốc bên ngoài, giỏ hàng: " + cart.getItems());

        // Tấn công 2: sửa list lấy được từ getItems()
        List<String> leaked = cart.getItems();
        leaked.add("Hack qua getter");
        System.out.println("Sau khi sửa list lấy từ getItems(), giỏ hàng thật: " + cart.getItems());

        cart.addItem("Thước kẻ");
        System.out.println("Giỏ hàng sau addItem hợp lệ: " + cart.getItems());
    }
}
