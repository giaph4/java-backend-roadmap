package phanB.bai5;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("isBlank(\"\") = " + StringUtils.isBlank(""));
        System.out.println("isBlank(\"  \") = " + StringUtils.isBlank("  "));
        System.out.println("isBlank(\"abc\") = " + StringUtils.isBlank("abc"));
        System.out.println("reverse(\"hello\") = " + StringUtils.reverse("hello"));
        System.out.println("capitalize(\"pho huynh\") = " + StringUtils.capitalize("pho huynh"));
        System.out.println("repeat(\"ab\", 3) = " + StringUtils.repeat("ab", 3));

        // "new StringUtils()" ngay tại đây sẽ KHÔNG compile được (constructor private)
        // -> dùng reflection để chứng minh constructor thật sự ném AssertionError khi bị ép gọi
        Constructor<StringUtils> ctor = StringUtils.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
        } catch (InvocationTargetException e) {
            System.out.println("Không tạo được instance StringUtils (đúng như kỳ vọng): " + e.getCause());
        }
    }
}
