package phanB.bai5;

import java.util.Locale;

public class Main {

    // An toàn với hàng null/rỗng - không chia cho 0
    static double trungBinh(int[] row) {
        if (row == null || row.length == 0) return Double.NaN;
        long sum = 0;
        for (int val : row) sum += val;
        return sum / (double) row.length;
    }

    public static void main(String[] args) {
        int[][] diem = {
                {8, 9, 10},
                {5, 6},
                {},     // rỗng
                null,   // null
                {7, 7, 8, 10}
        };

        double best = Double.NEGATIVE_INFINITY;
        int bestIdx = -1;
        for (int sv = 0; sv < diem.length; sv++) {
            double tb = trungBinh(diem[sv]);
            System.out.printf(Locale.ROOT, "SV %d: TB = %s%n", sv,
                    Double.isNaN(tb) ? "(không có điểm)" : String.format(Locale.ROOT, "%.2f", tb));
            if (!Double.isNaN(tb) && tb > best) {
                best = tb;
                bestIdx = sv;
            }
        }
        System.out.printf(Locale.ROOT, "Cao nhất: SV %d (%.2f)%n", bestIdx, best);
    }
}
