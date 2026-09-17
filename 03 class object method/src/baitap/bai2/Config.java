package baitap.bai2;

import java.util.HashMap;
import java.util.Map;

/**
 * Minh hoạ class initialization:
 * - static field initializer + static block chỉ chạy ĐÚNG 1 LẦN,
 *   khi class được khởi tạo lần đầu (new, gọi static method, truy cập
 *   static field không phải hằng compile-time...).
 * - MAX là hằng compile-time -> Config.MAX được inline lúc biên dịch,
 *   truy cập nó KHÔNG kích hoạt khởi tạo class -> static block KHÔNG chạy.
 */
public class Config {

    static final int MAX = 100;            // hằng compile-time -> bị inline, không kích hoạt init

    static Map<String, String> data;        // KHÔNG phải hằng compile-time

    static {
        System.out.println("  >> [Config] static block chạy — class đang được khởi tạo");
        data = new HashMap<>();
        data.put("timeout", "30");
    }
}
