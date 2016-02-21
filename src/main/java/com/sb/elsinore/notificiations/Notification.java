package com.sb.elsinore.notificiations;

/**
 * An interface that allows custom notifications to be used.
 * Created by Doug Edey on 01/03/15.
 */
public interface Notification {
    /**
     * Notify the current notification with the existing message
     */
    void sendNotification();

    /**
     * Clear the current notification (incase any cleanup needs to be made).
     */
    void clearNotification();

    /**
     * Get the current notification, return null if no notification is to be displayed.
     */
    String getNotification();

    /**
     * Set the notification message.
     * @param message The message to set.
     */
    void setMessage(String message);

    /**
     * Get the current notification message.
     * @return The current message in this notification.
     */
    String getMessage();
}
