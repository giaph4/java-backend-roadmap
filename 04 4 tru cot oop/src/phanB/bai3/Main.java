package phanB.bai3;

import java.util.List;

abstract class Employee {
    protected final String name;
    protected final long baseSalaryCents;

    public Employee(String name, long baseSalaryCents) {
        this.name = name;
        this.baseSalaryCents = baseSalaryCents;
    }

    public abstract long calculateSalaryCents();

    public abstract String employeeType();

    public String getName() {
        return name;
    }
}

class FulltimeEmployee extends Employee {

    private static final double BONUS_RATE = 0.10;

    public FulltimeEmployee(String name, long baseSalaryCents) {
        super(name, baseSalaryCents);
    }

    @Override
    public long calculateSalaryCents() {
        return baseSalaryCents + (long) (baseSalaryCents * BONUS_RATE);
    }

    @Override
    public String employeeType() {
        return "Full-time";
    }
}

class Manager extends Employee {
    private static final double BONUS_RATE = 0.50;
    private final int teamSize;

    public Manager(String name, long baseSalaryCents, int teamSize) {
        super(name, baseSalaryCents);
        this.teamSize = teamSize;
    }

    @Override
    public long calculateSalaryCents() {
        return baseSalaryCents +
                (long) (baseSalaryCents * BONUS_RATE) +
                teamSize * 50_000L;
    }

    @Override
    public String employeeType() {
        return "Manager";
    }
}

class Intern extends Employee {

    public Intern(String name, long baseSalaryCents) {
        super(name, baseSalaryCents);
    }

    @Override
    public long calculateSalaryCents() {
        return baseSalaryCents;
    }

    @Override
    public String employeeType() {
        return "Intern";
    }
}

class PayRoll {
    public static long totalPayrollCents(List<Employee> employees) {
        long total = 0;
        for (Employee e : employees) {
            total += e.calculateSalaryCents();
        }

        return total;
    }
}

public class Main {

    public static void main(String[] args) {
        List<Employee> employees = List.of(
                new FulltimeEmployee("Pho", 20_000_000_00L),
                new Manager("Huynh", 30_000_000_00L, 5),
                new Intern("Gia", 5_000_000_00L)
        );

        for (Employee e : employees) {
            System.out.printf("%s (%s): %,d cent%n", e.getName(), e.employeeType(), e.calculateSalaryCents());
        }

        System.out.printf("Tổng quỹ lương: %,d cent%n", PayRoll.totalPayrollCents(employees));
    }
}
