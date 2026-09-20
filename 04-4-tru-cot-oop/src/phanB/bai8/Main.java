package phanB.bai8;

abstract class ReportGenerator {

    // Template Method - "final" để lớp con KHÔNG được đổi trình tự các bước
    public final void generate() {
        String data = loadData();
        String formatted = format(data);
        save(formatted);
    }

    protected abstract String loadData();
    protected abstract String format(String data);
    protected void save(String formatted) { // có bản mặc định, lớp con có thể override nếu cần
        System.out.println("Lưu file: " + formatted);
    }
}

class PdfReport extends ReportGenerator {
    @Override
    protected String loadData() {
        return "doanh_thu=1000000;chi_phi=400000";
    }

    @Override
    protected String format(String data) {
        return "[PDF] " + data.replace(";", " | ");
    }
}

class CsvReport extends ReportGenerator {
    @Override
    protected String loadData() {
        return "doanh_thu=1000000;chi_phi=400000";
    }

    @Override
    protected String format(String data) {
        return "doanh_thu,chi_phi\n1000000,400000";
    }

    @Override
    protected void save(String formatted) {
        System.out.println("Ghi file report.csv:\n" + formatted);
    }
}

public class Main {

    public static void main(String[] args) {
        System.out.println("--- PdfReport ---");
        new PdfReport().generate();

        System.out.println();
        System.out.println("--- CsvReport ---");
        new CsvReport().generate();

        System.out.println();
        System.out.println("Khung generate() CỐ ĐỊNH (final) và giống nhau cho mọi loại report - chỉ các bước");
        System.out.println("loadData()/format()/save() thay đổi theo từng lớp con. Đây là kế thừa HỢP LÝ vì các");
        System.out.println("lớp con không thay đổi TRÌNH TỰ thuật toán, chỉ cung cấp CHI TIẾT từng bước - ngược lại");
        System.out.println("với Bài 6 (LoggingList/LoggingSet), nơi lớp con vô tình phụ thuộc vào CHI TIẾT NỘI BỘ");
        System.out.println("(addAll gọi add) mà nó không kiểm soát được.");
    }
}
