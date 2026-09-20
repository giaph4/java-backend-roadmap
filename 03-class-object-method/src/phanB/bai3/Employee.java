package phanB.bai3;

public class Employee {

    private String name;
    private double baseSalary;

    private static double companyBonusRate = 0.10;

    public Employee(String name, double baseSalary) {
        this.name = name;
        this.baseSalary = baseSalary;
    }

    public double calculateFinalSalary() {
        return baseSalary + baseSalary * companyBonusRate;
    }

    public static void updateBonusRate(double newRate) {
        companyBonusRate = newRate;
    }

    public String getName() {
        return name;
    }

}
