package phanB.bai2;

import java.util.List;

public class NotificationService {

    public void broadcast(List<Notifiable> channels, String message) {
        for (Notifiable notifiable : channels) {
            notifiable.send(message);
        }
    }
}
