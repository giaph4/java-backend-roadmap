package phanB.bai1;

import java.util.Locale;

public class Main {

    // Tính tiền điện bậc thang: 50 kWh đầu giá 1.678 đ/kWh, từ kWh thứ 51 giá 2.014 đ/kWh
    static double tienDien(double kWh) {
        final double GIA_BAC_1 = 1678.0;
        final double GIA_BAC_2 = 2014.0;
        if (kWh <= 50) return kWh * GIA_BAC_1;
        return 50 * GIA_BAC_1 + (kWh - 50) * GIA_BAC_2;
    }

    public static void main(String[] args) {
        double[] hoTieuThu = {50, 75.5, 123.4, 10, 250.25, 0.1, 0.2};
        double tongDouble = 0;
        for (double kWh : hoTieuThu) {
            double tien = tienDien(kWh);
            System.out.printf(Locale.ROOT, "kWh=%.2f -> tiền = %.4f%n", kWh, tien);
            tongDouble += tien;
        }
        System.out.printf(Locale.ROOT, "Tổng (double) = %.10f%n", tongDouble);

        // Minh họa sai số double: 0.1 + 0.2 (kWh) không cho đúng bội số tuyến tính của giá bậc 1
        System.out.printf(Locale.ROOT, "tienDien(0.1)+tienDien(0.2) = %.15f  (kỳ vọng %.15f)%n",
                tienDien(0.1) + tienDien(0.2), 0.3 * 1678.0);
        System.out.println("=> double tích lũy sai số nhị phân khi cộng dồn nhiều số thập phân;");
        System.out.println("   giải pháp triệt để: BigDecimal hoặc lưu tiền theo đơn vị 'đồng nguyên' (long) - học ở module sau.");
    }
}
