package phanB.bai7;

public class Main {

    record Money(long amountCents, String currency) {

        // Compact constructor - validate trước khi field thật sự được gán
        Money {
            if (currency == null || currency.length() != 3 || !currency.equals(currency.toUpperCase())) {
                throw new IllegalArgumentException("currency phải đúng 3 ký tự in hoa, nhận được: " + currency);
            }
            if (amountCents < 0) {
                throw new IllegalArgumentException("amountCents không được âm: " + amountCents);
            }
        }

        Money plus(Money other) {
            if (!this.currency.equals(other.currency)) {
                throw new IllegalArgumentException(
                        "Không thể cộng hai Money khác currency: " + this.currency + " vs " + other.currency);
            }
            return new Money(this.amountCents + other.amountCents, this.currency);
        }

        String format() {
            return String.format("%d.%02d %s", amountCents / 100, amountCents % 100, currency);
        }
    }

    public static void main(String[] args) {
        Money a = new Money(1234, "USD");
        Money b = new Money(1234, "USD");

        System.out.println("a = " + a);
        System.out.println("a.format() = " + a.format());
        System.out.println("a.equals(b) = " + a.equals(b)); // true - record tự sinh equals() theo GIÁ TRỊ field

        Money c = new Money(500, "USD");
        System.out.println("a.plus(c) = " + a.plus(c).format());

        try {
            Money invalid = new Money(-100, "USD");
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi bắt được (amount âm): " + e.getMessage());
        }

        try {
            Money invalid = new Money(100, "usd");
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi bắt được (currency sai định dạng): " + e.getMessage());
        }

        try {
            Money vnd = new Money(1000, "VND");
            a.plus(vnd);
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi bắt được (khác currency): " + e.getMessage());
        }
    }
}
