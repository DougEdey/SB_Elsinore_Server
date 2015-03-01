package com.sb.elsinore.notificiations;

/**
 * Trigger a web notification
 * Created by doug on 01/03/15.
 */
public class WebNotification implements Notification {

    private boolean showNotification = false;
    private String notificationMessage = null;

    @Override
    public void sendNotification() {
        showNotification = true;
    }

    @Override
    public void clearNotification() {
        showNotification = false;
    }

    @Override
    public String getNotification() {
        if (showNotification) {
            return notificationMessage;
        }
        return null;
    }

    @Override
    public void setMessage(String message) {
        this.notificationMessage = message;
    }

    @Override
    public String getMessage() {
        return notificationMessage;
    }
}
