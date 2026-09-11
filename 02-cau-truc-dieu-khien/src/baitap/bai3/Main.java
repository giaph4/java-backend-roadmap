package baitap.bai3;

public class Main {

    private static boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }

        return true;
    }

    ;

    public static void main(String[] args) {
        for (int checkNum : new int[]{0, 1, 2, 3, 4, 9, 15, 17, 25, 97, 100}) {
            System.out.println(checkNum + " -> " +
                    (isPrime(checkNum) ? "So nguyen to" : "Khong phai so nguyen to"));
        }
    }
}
