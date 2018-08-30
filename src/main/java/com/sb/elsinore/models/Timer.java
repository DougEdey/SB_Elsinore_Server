package com.sb.elsinore.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Entity
public class Timer implements Comparable<Timer> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    private int position = -1;
    private String mode;
    private int target_mins = -1;
    private boolean inverted;
    private long currentValue = 0;
    private Date startTime = null;

    public Timer() {
    }

    public Timer(String newName) {
        this.name = newName;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Set the name of this timer.
     *
     * @param newName The new position to use.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    public int getPosition() {
        return this.position;
    }

    /**
     * Set the new position of this timer.
     *
     * @param newPos The new position to use.
     */
    public void setPosition(int newPos) {
        this.position = newPos;
    }

    @Override
    public int compareTo(Timer other) {
        return Integer.compare(this.position, other.getPosition());
    }

    public String getMode() {
        return this.mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getTarget() {
        return this.target_mins;
    }

    public void setTarget(String target) {
        if (target == null || target.equals("")) {
            this.target_mins = -1;
            return;
        }
        this.target_mins = Integer.parseInt(target.replace(",", "."));
    }

    public boolean getInverted() {
        return this.inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    public void startTimer() {
        if (this.startTime == null && this.currentValue == 0) {
            // Start the timer
            this.startTime = new Date();
            this.currentValue = 0;
        } else if (this.startTime != null && this.currentValue != 0) {
            // Resume the timer
            this.startTime = new Date(new Date().getTime() - this.currentValue);
            this.currentValue = 0;
        } else {
            // pause the timer
            this.currentValue = new Date().getTime() - this.startTime.getTime();
        }
    }

    public void resetTimer() {
        this.startTime = null;
        this.currentValue = 0;
    }
}
