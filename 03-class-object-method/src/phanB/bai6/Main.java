package phanB.bai6;

public class Main {

    static void describe(int x)      { System.out.println("describe(int): " + x); }
    static void describe(long x)     { System.out.println("describe(long): " + x); }
    static void describe(Integer x)  { System.out.println("describe(Integer): " + x); }
    static void describe(Object x)   { System.out.println("describe(Object): " + x); }
    static void describe(int... x)   { System.out.println("describe(int...): " + java.util.Arrays.toString(x)); }

    public static void main(String[] args) {
        // Java resolve overload qua 3 PHA, chọn pha SỚM NHẤT có ứng viên khớp:
        // Pha 1 - khớp CHÍNH XÁC kiểu, KHÔNG autobox, KHÔNG varargs.
        // Pha 2 - cho phép autobox/unbox, vẫn KHÔNG varargs.
        // Pha 3 - cho phép varargs (chỉ dùng khi pha 1 và 2 đều không có ứng viên).

        // Dự đoán:
        // describe(1)                    -> describe(int)      [pha 1: khớp chính xác int]
        // describe(1L)                   -> describe(long)     [pha 1: khớp chính xác long]
        // describe(Integer.valueOf(1))   -> describe(Integer)  [pha 1: khớp chính xác Integer, KHÔNG unbox
        //                                                        xuống int vì đã có ứng viên khớp thẳng]
        // describe("x")                  -> describe(Object)   [pha 1: String không khớp int/long/Integer
        //                                                        trực tiếp, nhưng khớp Object qua widening reference]
        // describe(1, 2, 3)              -> describe(int...)   [không ứng viên nào ở pha 1/2 nhận nhiều đối số
        //                                                        -> rơi xuống pha 3: varargs]
        // describe()                     -> describe(int...)   [chỉ varargs nhận được 0 đối số]

        describe(1);
        describe(1L);
        describe(Integer.valueOf(1));
        describe("x");
        describe(1, 2, 3);
        describe();
    }
}
