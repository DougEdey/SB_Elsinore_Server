package com.sb.elsinore;

public class Timer implements Comparable<Timer> {

    private String name = "";
    private int position = -1;
    private String mode;
    private int target_mins = -1;

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
        this.target_mins = Integer.parseInt(target.replace(",", "."));
    }

    public int getTarget() {
        return this.target_mins;
    }
}
