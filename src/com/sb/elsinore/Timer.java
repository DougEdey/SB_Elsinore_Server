package com.sb.elsinore;

import java.util.Comparator;

public class Timer implements Comparable<Timer> {

    private String name = "";
    private int position = -1;

    public Timer(String newName) {
        this.name = newName;
    }

    /**
     * Set the new position of this pump.
     * @param newPos The new position to use.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Set the new position of this pump.
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
        // TODO Auto-generated method stub
        return Integer.compare(this.position, other.getPosition());
    }

}
