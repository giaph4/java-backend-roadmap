package baitap.bai6;

public class Main {

    private static int sumOfDigits(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n phai >= 0");
        }

        if (n < 1) {
            return n;
        }

        return n % 10 + sumOfDigits(n / 10);
    }

    public static int sumOfDigitsLoop(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n phai >= 0");
        }

        if (n < 10) {
            return n;
        }

        int sum = 0;
        while (n > 0) {
            sum += n % 10;
            n /= 10;
        }

        return sum;
    }

    public static void main(String[] args) {
        int[] tests = { 0, 7, 10, 99, 12345, 1000000007 };
        for (int t : tests) {
            System.out.printf("n=%-12d  đệ quy=%-3d  vòng lặp=%-3d%n",
                    t, sumOfDigits(t), sumOfDigitsLoop(t));
        }
    }
}
