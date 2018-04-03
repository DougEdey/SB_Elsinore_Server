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
        this.showNotification = true;
    }

    @Override
    public void clearNotification() {
        this.showNotification = false;
    }

    @Override
    public String getNotification() {
        if (this.showNotification) {
            return this.notificationMessage;
        }
        return null;
    }

    @Override
    public void setMessage(String message) {
        this.notificationMessage = message;
    }

    @Override
    public String getMessage() {
        return this.notificationMessage;
    }
}
