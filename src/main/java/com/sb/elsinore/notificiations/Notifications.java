package com.sb.elsinore.notificiations;

import com.sb.elsinore.BrewServer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * The main notifications handler. Controls when notifications are created or destroyed.
 * Created by Doug Edey on 01/03/15.
 */
public class Notifications {
    private static Notifications notificationsInstance = null;
    private ArrayList<Notification> notificationsList = new ArrayList<>();

    /**
     * The private constructor. This is a singleton.
     */
    private Notifications() {}

    /**
     * Get the current instance of this notifications list.
     * @return The current instance.
     */
    public static Notifications getInstance() {
        if (notificationsInstance == null) {
            notificationsInstance = new Notifications();
        }

        return notificationsInstance;
    }

    /**
     * Add a notification to the list, return its index.
     * @param newNotification The new notification object to add.
     * @return THe index of the new notification, or -1 if there was an error.
     */
    public int addNotification(Notification newNotification) {
        if (newNotification == null) {
            BrewServer.LOG.warning("Null notification supplied.");
            new RuntimeException().printStackTrace();
            return -1;
        }
        this.notificationsList.add(newNotification);
        if (newNotification.getMessage() != null) {
            BrewServer.LOG.info("Notification added with message: " + newNotification.getMessage());
        } else {
            BrewServer.LOG.info("Notification added with no message");
        }
        return this.notificationsList.indexOf(newNotification);
    }

    /**
     * Clear the notification at the specified index.
     * @param index The index of the notifcation to clear.
     * @return True if the notifcation was cleared, or false if there was an issue.
     */
    public boolean clearNotification(int index) {
        Notification toDelete = this.notificationsList.get(index);
        if (toDelete == null) {
            BrewServer.LOG.warning("Could not find notification: " + index + " to clear.");
            BrewServer.LOG.warning(this.getNotificationStatus().toString());
            return false;
        }

        toDelete.clearNotification();
        this.notificationsList.remove(index);
        BrewServer.LOG.info("Cleared notification: " + index);
        return true;
    }

    /**
     * @return a {JSONArray} representing the current notitifcations list status.
     */
    public JSONArray getNotificationStatus() {
        JSONArray nStatus = new JSONArray();
        JSONObject nObject = new JSONObject();

        for (int i = 0; i < this.notificationsList.size(); i++) {
            Notification n = this.notificationsList.get(i);
            if (n.getNotification() == null) {
                continue;
            }
            nObject.put("message", n.getMessage());
            nObject.put("tag", "elsinore-" + i);
            nStatus.add(nObject);
        }
        return nStatus;
    }

    public void clearNotification(Notification notification) {
        if (notification == null || this.notificationsList.indexOf(notification) == -1) {
            return;
        }
        clearNotification(this.notificationsList.indexOf(notification));
    }
}
