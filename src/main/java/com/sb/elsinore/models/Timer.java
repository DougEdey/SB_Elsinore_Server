package com.sb.elsinore.models;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Timer implements Comparable<Timer> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    private int position = -1;
    private String mode;
    private int targetMins = -1;
    private boolean inverted;
    @Transient
    private long currentValue = 0;
    private Date startTime = null;

    public Timer() {
    }

    public Timer(String newName) {
        this.name = newName;
    }

    public Date getStartTime() {
        return this.startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getTargetMins() {
        return this.targetMins;
    }

    public void setTargetMins(String target) {
        if (target == null || target.equals("")) {
            this.targetMins = -1;
            return;
        }
        this.targetMins = Integer.parseInt(target.replace(",", "."));
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
