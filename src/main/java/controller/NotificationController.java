package controller;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Centre de notifications en session pour l'UI web.
 */
@Named("notificationController")
@SessionScoped
public class NotificationController implements Serializable {

    private final List<NotificationItem> notifications = new ArrayList<>();

    public void info(String titre, String message) {
        push("INFO", titre, message);
    }

    public void success(String titre, String message) {
        push("SUCCESS", titre, message);
    }

    public void warning(String titre, String message) {
        push("WARNING", titre, message);
    }

    public void clearAll() {
        notifications.clear();
    }

    public void markAllAsRead() {
        for (NotificationItem item : notifications) {
            item.setRead(true);
        }
    }

    public int getUnreadCount() {
        int count = 0;
        for (NotificationItem item : notifications) {
            if (!item.isRead()) {
                count++;
            }
        }
        return count;
    }

    public List<NotificationItem> getRecentNotifications() {
        return Collections.unmodifiableList(notifications);
    }

    private void push(String type, String titre, String message) {
        NotificationItem item = new NotificationItem();
        item.setType(type);
        item.setTitre(titre);
        item.setMessage(message);
        item.setCreatedAt(new Date());
        item.setRead(false);

        notifications.add(0, item);
        if (notifications.size() > 20) {
            notifications.remove(notifications.size() - 1);
        }
    }

    public static class NotificationItem implements Serializable {
        private String type;
        private String titre;
        private String message;
        private Date createdAt;
        private boolean read;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTitre() {
            return titre;
        }

        public void setTitre(String titre) {
            this.titre = titre;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }

        public boolean isRead() {
            return read;
        }

        public void setRead(boolean read) {
            this.read = read;
        }
    }
}