package com.sb.elsinore;

import org.json.simple.JSONObject;

import java.util.Date;

public class Timer implements Comparable<Timer> {

    public static final String ID = "id";
    public static final String POSITION = "position";
    public static final String TARGET = "target";
    private String name = "";
    private int position = -1;
    private String mode;
    private int target_mins = -1;
    private boolean inverted;
    private long currentValue = 0;
    private Date startTime = null;

    public Timer(String newName) {
        this.name = newName;
    }

    /**
     * Set the name of this timer.
     * @param newName The new position to use.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    public String getName() {
        return this.name;
    }
    
    /**
     * Set the new position of this timer.
     * @param newPos The new position to use.
     */
    public void setPosition(int newPos) {
        this.position = newPos;
    }

    public int getPosition() {
        return this.position;
    }

    @Override
    public int compareTo(Timer other) {
        return Integer.compare(this.position, other.getPosition());
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    public void setTarget(String target) {
        if (target == null || target.equals("")) {
            this.target_mins = -1;
            return;
        }
        this.target_mins = Integer.parseInt(target.replace(",", "."));
    }

    public int getTarget() {
        return this.target_mins;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    public void startTimer() {
        if (this.startTime == null && this.currentValue == 0)
        {
            // Start the timer
            startTime = new Date();
            this.currentValue = 0;
        }
        else if (this.startTime != null && this.currentValue != 0)
        {
            // Resume the timer
            this.startTime = new Date(new Date().getTime() - this.currentValue);
            this.currentValue = 0;
        }
        else {
            // pause the timer
            this.currentValue = new Date().getTime() - startTime.getTime();
        }
    }

    public void resetTimer()
    {
        this.startTime = null;
        this.currentValue = 0;
    }

    public JSONObject getStatus() {
        JSONObject status = new JSONObject();
        status.put("invert", Boolean.toString(this.inverted));
        long elapsedms = currentValue;
        if (this.currentValue == 0 && startTime != null) {
            elapsedms = new Date().getTime() - startTime.getTime();
        }
        status.put("elapsedms", elapsedms);
        status.put("durationms", this.target_mins*1000*60);
        status.put("position", position);
        String active = "off";
        if (this.currentValue == 0 && this.startTime == null)
        {
            active = "off";
        }
        else if (this.currentValue == 0 && this.startTime != null)
        {
            active = "running";
        }
        else
        {
            active = "paused";
        }

        status.put("mode", active);
        return status;
    }
}
