package phanB.bai3;

public class Main {

    public static void main(String[] args) {
        Employee e1 = new Employee("Pho", 20_000_000);
        Employee e2 = new Employee("Huynh", 25_000_000);
        Employee e3 = new Employee("Gia", 30_000_000);

        System.out.println("--- Trước khi đổi bonus rate (10%) ---");
        for (Employee e : new Employee[]{e1, e2, e3}) {
            System.out.printf("%s: %.0f%n", e.getName(), e.calculateFinalSalary());
        }

        Employee.updateBonusRate(0.20); // Chỉ gọi 1 LẦN, qua class - không qua instance nào cả

        System.out.println("--- Sau khi đổi bonus rate (20%) ---");
        for (Employee e : new Employee[]{e1, e2, e3}) {
            System.out.printf("%s: %.0f%n", e.getName(), e.calculateFinalSalary());
        }
    }
}
