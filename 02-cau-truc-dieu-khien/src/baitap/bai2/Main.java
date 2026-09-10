package baitap.bai2;

import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {

    record Item(String name, int price) {
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("Nhap lua chon (1-4 chon mon, 0 de thoat):");
            try {
                choice = scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Nhap khong phai so, nhap lai!");
                scanner.next();
                choice = -1;
                continue;
            }

            Item item = switch (choice) {
                case 1 -> new Item("Ca phe den", 15000);
                case 2 -> new Item("Ca phe sua", 20000);
                case 3 -> new Item("Bac xiu", 25000);
                case 4 -> new Item("Tra dao", 30000);
                case 0 -> null;
                default -> {
                    System.out.println("Lua chon ngoai pham vi (1-4 hoac 0), nhap lai!");
                    yield null;
                }
            };

            if (choice == 0) {
                System.out.println("Tam biet!");
            } else if (item != null) {
                System.out.println("Ban da chon: " + item.name() + " - " + item.price() + " VND");
            }
        } while (choice != 0);

        scanner.close();
    }
}