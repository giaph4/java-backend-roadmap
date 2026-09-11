package baitap.bai4;

public class Main {

    public static void main(String[] args) {

        int n = 5;
        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= n; i++) {
            sb.setLength(0);
            for (int j = 1; j <= i; j++) {
                if (j > 1) {
                    sb.append(' ');
                }
                sb.append(j);
            }
            System.out.println(sb);
        }
    }
}
