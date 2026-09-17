package phanB.bai2;

public class Main {

    // Đảo chuỗi theo CODE POINT (không theo char) - an toàn với emoji/surrogate pair
    static String reverseString(String input) {
        if (input == null || input.isEmpty()) return input;
        int[] cps = input.codePoints().toArray();
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = cps.length - 1; i >= 0; i--) {
            sb.appendCodePoint(cps[i]);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(reverseString("Java Backend"));   // dnekcaB avaJ
        System.out.println(reverseString("ab😀cd"));          // dc😀ba - emoji không bị vỡ
        System.out.println(reverseString(""));                // ""
        System.out.println(reverseString("a"));                // a
    }
}
