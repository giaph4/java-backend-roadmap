package phanB.bai9;

public class Main {

    // Đệ quy đuôi - Java KHÔNG tối ưu tail-call, vẫn tốn 1 stack frame mỗi lần gọi
    private static int sumRecursiveTail(int[] a, int i, int acc) {
        if (i == a.length) return acc;
        return sumRecursiveTail(a, i + 1, acc + a[i]);
    }

    // Khử đệ quy đuôi bằng for - O(1) bộ nhớ stack
    private static int sumLoop(int[] a) {
        int acc = 0;
        for (int i = 0; i < a.length; i++) {
            acc += a[i];
        }
        return acc;
    }

    public static void main(String[] args) {
        int[] small = {1, 2, 3, 4, 5};
        System.out.println("sumRecursiveTail(small) = " + sumRecursiveTail(small, 0, 0));
        System.out.println("sumLoop(small) = " + sumLoop(small));

        int[] bigArray = new int[1_000_000];
        for (int i = 0; i < bigArray.length; i++) bigArray[i] = 1;

        System.out.println("sumLoop(bigArray) = " + sumLoop(bigArray));

        try {
            System.out.println("sumRecursiveTail(bigArray) = " + sumRecursiveTail(bigArray, 0, 0));
        } catch (StackOverflowError e) {
            System.out.println("sumRecursiveTail(bigArray) -> StackOverflowError!");
            System.out.println(
                    "Lý do: Java không có tail-call optimization (TCO) như một số ngôn ngữ khác " +
                    "(Scala, Kotlin với 'tailrec', hay các compiler LISP/Scheme). Dù lời gọi đệ quy " +
                    "nằm ở vị trí cuối cùng (tail position), JVM vẫn tạo MỘT stack frame mới cho mỗi " +
                    "lần gọi thay vì tái sử dụng frame hiện tại -> với mảng 1 triệu phần tử, " +
                    "1 triệu frame chồng lên nhau vượt quá kích thước stack mặc định (-Xss, thường 512KB-1MB) " +
                    "-> StackOverflowError. Cách khắc phục triệt để: viết lại bằng vòng lặp 'for' (sumLoop) " +
                    "- luôn chỉ dùng 1 stack frame bất kể kích thước dữ liệu."
            );
        }
    }
}
