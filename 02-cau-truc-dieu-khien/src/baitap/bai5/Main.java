package baitap.bai5;

import java.util.Arrays;

public class Main {

    private static int[] findFirst(int[][] m, int target) {
        for (int i = 0; i < m.length; i++) {
            for (int j = i; j < m[i].length; j++) {
                if (m[i][j] == target) {
                    return new int[]{i, j};
                }
            }
        }

        return new int[]{-1, -1};
    }

    public static void main(String[] args) {
        int[][] matrix = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };

        int target = 3;
        int[] result = findFirst(matrix, target);
        System.out.println(Arrays.toString(result));
    }
}
