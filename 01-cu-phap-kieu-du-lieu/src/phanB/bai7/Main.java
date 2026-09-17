package phanB.bai7;

public class Main {

    // Ném ArithmeticException nếu tổng vượt phạm vi int
    static int safeSum(int[] nums) {
        int acc = 0;
        for (int n : nums) {
            acc = Math.addExact(acc, n);
        }
        return acc;
    }

    // Cộng dồn vào long để so sánh hành vi - không bao giờ tràn với input là int[]
    static long sum(int[] nums) {
        long acc = 0;
        for (int n : nums) acc += n;
        return acc;
    }

    public static void main(String[] args) {
        int[] ok = {1, 2, 3, 4, 5};
        int[] tran = {Integer.MAX_VALUE, 1};

        System.out.println("safeSum(ok) = " + safeSum(ok));   // 15
        System.out.println("sum(tran)   = " + sum(tran));     // 2147483648 (long, không tràn)

        try {
            safeSum(tran);
        } catch (ArithmeticException e) {
            System.out.println("safeSum(tran) -> ArithmeticException: " + e.getMessage());
        }
    }
}
