import java.util.Arrays;
import java.util.Comparator;

public class Main {
    public static void main(String[] args) {

//        int dec  = 1_000_000;     // thập phân, '_' chỉ để dễ đọc (Java 7+)
//        int hex  = 0xFF;          // 255  — tiền tố 0x / 0X
//        int oct  = 0777;          // 511  — tiền tố 0 (BÁT PHÂN! nguồn bug kinh điển)
//        int bin  = 0b1010_0001;   // 161  — tiền tố 0b / 0B (Java 7+)
//        long big = 9_000_000_000L; // hậu tố L bắt buộc khi vượt phạm vi int (~2.1 tỷ)
//        System.out.println("Integers:");
//        System.out.println(dec + "\n" + hex + "\n" + oct + "\n" + bin + "\n" + big);
//
//        double d1 = 3.14;
//        double d2 = 1.5e3;        // 1500.0 — ký hiệu khoa học
//        float  f1 = 19.99f;       // hậu tố f/F bắt buộc (mặc định literal thực là double)
//        double hexFloat = 0x1.8p1; // 3.0 — literal thực hệ hex (hiếm dùng)
//        double under = 3_000.123_456;
//        System.out.println("Floating point numbers:");
//        System.out.println(d1 + "\n" + d2 + "\n" + f1 + "\n" + hexFloat + "\n" + under);

//        char a    = 'A';        // 65
//        char newl = '\n';       // xuống dòng
//        char tab  = '\t';
//        char quote= '\'';       // \'  \"  \\
//        char uni  = 'é';  // 'é' — Unicode escape: / '\\u' + 4 chữ số hex
//        char zero = '\0';       // ký tự NUL, giá trị 0 — cũng là giá trị mặc định của char
//        char num  = 74;         // hợp lệ: gán literal int vừa phạm vi cho char → 'A'
//        System.out.println("Characters:");
//        System.out.println(a + "\n" + newl + "\n" + tab + "\n" + quote + "\n" + uni + "\n" + zero + "\n" + num);
//        byte 18 bit -128 den 127
//        short 16 bit -32768 den 32767
//        int 32 bit -2147483648 den 2147483647
//        long 64 bit -9223372036854775808 den 9223372036854775807

//        int n = Integer.parseInt("42");        // ném NumberFormatException nếu sai định dạng
//        int hexN = Integer.parseInt("FF", 16);    // 255
//        Integer boxed = Integer.valueOf("42");     // trả Integer (có thể từ cache)
//        double d = Double.parseDouble("3.14");
//        long l = Long.parseLong("9000000000");
//
//        String bin = Integer.toBinaryString(10);   // "1010"
//        String hx = Integer.toHexString(255);     // "ff"
//        int bits = Integer.bitCount(255);        // 8
//
//        System.out.println("n = " + n + "\nhexN = " + hexN + "\nboxed = " + boxed + "\nd = " + d + "\nl = " + l);
//        System.out.println("bin = " + bin + "\nhx = " + hx + "\nbits = " + bits);

//        Object o = true ? Integer.valueOf(1) : Double.valueOf(2.0); // kiểu chung là Number
//        System.out.println(o);


//        double a = 0.3;
//        double b = 0.5;
//        double c = 0.8;
//        System.out.println( a + b);

        int[] arrays = {3, 3, 4, 1, 4, 5, 12, 3};
//        Arrays.sort(arrays, Comparator.reverseOrder());
        System.out.println(Arrays.toString(arrays));
    }
}