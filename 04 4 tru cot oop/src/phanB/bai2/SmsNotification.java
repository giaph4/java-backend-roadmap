package phanB.bai2;

public class SmsNotification implements Notifiable {
    @Override
    public void send(String msg) {
        System.out.println("[SMS] " + msg);
    }
}
