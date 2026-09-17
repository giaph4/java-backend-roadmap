package phanB.bai1;

public class BankAccount {

    private final String accountNumber;
    private double balance;
    private String ownerName;

    private static int totalAccounts = 0;

    public BankAccount(String accountNumber, double balance, String ownerName) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.ownerName = ownerName;
        totalAccounts++;
    }

    public BankAccount(String ownerName) {
        this("ACC" + (totalAccounts + 1), 0, ownerName);
    }

    public void deposit(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Số tiền gửi phải >= 0");
        }
        balance += amount;
    }

    public void withdraw(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Số tiền rút phải >= 0");
        }
        if (balance < amount) {
            throw new IllegalArgumentException("Số tiền trong tài khoản không đủ");
        }
        balance -= amount;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public String getOwnerName() {
        return ownerName;

    }

    public static void printTotal() {
        System.out.println("Tổng số tài khoản đã tạo: " + totalAccounts);
    }

}
