package phanB.bai2;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        NotificationService service = new NotificationService();

        List<Notifiable> channels = List.of(
                new EmailNotification(),
                new SmsNotification(),
                new PushNotification(),
                new SlackNotification()
        );

        service.broadcast(channels, "Hello, this is a notification message!");
    }
}
