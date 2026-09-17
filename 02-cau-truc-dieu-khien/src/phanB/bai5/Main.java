package phanB.bai5;

import java.util.Arrays;

public class Main {

    private static int[] findFirst(int[][] m, int target) {
        int foundRow = -1, foundCol = -1;

        search:
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[i].length; j++) {
                if (m[i][j] == target) {
                    foundRow = i;
                    foundCol = j;
                    break search; // labeled break - thoát cả 2 vòng lặp ngay khi tìm thấy
                }
            }
        }

        return new int[]{foundRow, foundCol};
    }

    public static void main(String[] args) {
        int[][] matrix = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };

        for (int target : new int[]{3, 4, 9, 99}) {
            int[] result = findFirst(matrix, target);
            System.out.println("target=" + target + " -> " + Arrays.toString(result));
        }
    }
}
