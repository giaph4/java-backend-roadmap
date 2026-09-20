package baitap.bai2;

/**
 * Minh hoạ: static method KHÔNG bị override, chỉ bị "che" (hiding).
 * Lời gọi static method được compiler phân giải theo KIỂU TĨNH của biến,
 * không theo object thật đang được trỏ tới (khác instance method thật sự override).
 */
public class Animal {
    static String sound() {
        return "...";
    }
}
