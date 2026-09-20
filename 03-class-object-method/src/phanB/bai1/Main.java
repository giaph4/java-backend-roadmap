package phanB.bai1;

public class Main {

    public static void main(String[] args) {
        BankAccount a1 = new BankAccount("ACC-VIP-001", 500_000, "Pho");
        BankAccount a2 = new BankAccount("Huynh"); // tự sinh accountNumber

        System.out.println(a1.getAccountNumber() + " - " + a1.getOwnerName() + " - " + a1.getBalance());
        System.out.println(a2.getAccountNumber() + " - " + a2.getOwnerName() + " - " + a2.getBalance());

        a2.deposit(200_000);
        a2.withdraw(50_000);
        System.out.println("Số dư a2 sau giao dịch: " + a2.getBalance());

        try {
            a2.withdraw(1_000_000);
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi bắt được: " + e.getMessage());
        }

        BankAccount.printTotal();
    }
}
