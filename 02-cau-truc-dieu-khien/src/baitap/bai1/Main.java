package baitap.bai1;

import java.util.Arrays;

public class Main {

    private static boolean isTriangle(double a, double b, double c) {
        return a + b > c && a + c > b && b + c > a;
    }

    private static boolean isEquilateral(double a, double b, double c) {
        return a == b && b == c;
    }

    private static boolean isIsosceles(double a, double b, double c) {
        return a == b || b == c || a == c;
    }

    private static boolean isRightTriangle(double a, double b, double c) {
        double[] sides = {a, b, c};
        Arrays.sort(sides);
        return Math.abs(sides[0] * sides[0] + sides[1] * sides[1] - sides[2] * sides[2]) < 1e-9;
    }

    private static boolean isScalene(double a, double b, double c) {
        return a != b && b != c && a != c;
    }


    private static String classifyTriangle(double a, double b, double c) {
        return isTriangle(a, b, c) ? isEquilateral(a, b, c) ? "Tam giác đều" :
                                     isIsosceles(a, b, c) ? "Tam giác cân" :
                                     isRightTriangle(a, b, c) ? "Tam giác vuông" :
                                     isScalene(a, b, c) ? "Tam giác thường" : "Không xác định" :
                "Không phải tam giác";

    }

    public static void main(String[] args) {
        double a = 3, b = 4, c = 5;
        System.out.printf("Các cạnh: %.2f, %.2f, %.2f\n", a, b, c);
        System.out.println("Phân loại: " + classifyTriangle(a, b, c));
    }
}
