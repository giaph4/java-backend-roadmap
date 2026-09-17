package phanB.bai3;

public class Main {

    // Two-pointer, bỏ qua ký tự không phải chữ/số, không phân biệt hoa/thường, không tạo chuỗi đảo ngược
    static boolean isPalindrome(String s) {
        if (s == null) return false;
        int l = 0, r = s.length() - 1;
        while (l < r) {
            char cl = s.charAt(l);
            char cr = s.charAt(r);
            if (!Character.isLetterOrDigit(cl)) { l++; continue; }
            if (!Character.isLetterOrDigit(cr)) { r--; continue; }
            if (Character.toLowerCase(cl) != Character.toLowerCase(cr)) return false;
            l++;
            r--;
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(isPalindrome("A man, a plan, a canal: Panama")); // true
        System.out.println(isPalindrome("Backend"));                        // false
        System.out.println(isPalindrome(""));                               // true
        System.out.println(isPalindrome("a"));                              // true
    }
}
