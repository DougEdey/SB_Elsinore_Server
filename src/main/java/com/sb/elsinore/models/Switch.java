package com.sb.elsinore.models;

import com.sb.elsinore.BrewServer;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

import javax.persistence.*;

/**
 * A helper class for switch control. not very complex. Designed to control a
 * single GPIO pin with a straight forward on/off functionality
 *
 * @author Doug Edey
 */
@Entity
public class Switch implements Comparable<Switch> {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String pinName = null;
    /**
     * Current switch name.
     */
    private String name = null;
    /**
     * the outpin for the switch.
     */
    @Transient
    private OutPin output;
    private boolean invertOutput = false;
    private int position = -1;

    /**
     * For hibernation
     */
    public Switch() {
    }

    /**
     * The Constructor.
     *
     * @param newName The name of this switch
     * @param pinName The GPIO pin name
     * @throws InvalidGPIOException If there's a problem opening the GPIO.
     */
    public Switch(final String newName, final String pinName)
            throws InvalidGPIOException {
        this.name = newName.replace("_", " ").trim();

        String temp = null;

        try {
            temp = System.getProperty("invert_outputs");
        } catch (Exception e) {
            // In case get property fails
        }

        if (temp != null) {
            BrewServer.LOG.warning("Inverted outputs: " + temp);
            this.invertOutput = true;
        }

        this.pinName = pinName;
        this.output = new OutPin(pinName);
        this.turnOff();
    }

    /**
     * @return The current state of the switch, true for on. False for off.
     */
    public boolean getStatus() {
        try {
            if (this.invertOutput) {
                return this.output.getValue().equals("0");
            } else {
                return this.output.getValue().equals("1");
            }
        } catch (Exception e) {
            BrewServer.LOG.warning("Couldn't toggle switch: " + e);
            return false;
        }
    }

    /**
     * Turn on the switch.
     */
    public void turnOn() {
        if (this.invertOutput) {
            this.output.setValue(false);
        } else {
            this.output.setValue(true);
        }
    }

    /**
     * Turn off the switch.
     */
    public void turnOff() {
        if (this.invertOutput) {
            this.output.setValue(true);
        } else {
            this.output.setValue(false);
        }
    }

    /**
     * @return The name of this switch
     */
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return The current GPIO Pin.
     */
    public String getGPIO() {
        return this.output.getGPIOName();
    }

    public void setGPIO(String gpio) throws InvalidGPIOException {
        if (!this.output.getGPIOName().equalsIgnoreCase(gpio)) {
            this.output.close();
            this.output = new OutPin(gpio);
        }
    }

    /**
     * @return Return the current name with _ instead of spaces.
     */
    public String getNodeName() {

        return "_" + this.name.replace(" ", "_");
    }

    public int getPosition() {
        return this.position;
    }

    /**
     * Set the new position of this switch.
     *
     * @param newPos The new position to use.
     */
    public void setPosition(int newPos) {
        this.position = newPos;
    }

    @Override
    public int compareTo(Switch o) {
        return Integer.compare(this.position, o.getPosition());
    }

    public void shutdown() {
        if (this.output != null) {
            this.output.close();
        }
    }

    public boolean getInverted() {
        return this.invertOutput;
    }

    public void setInverted(boolean invert) {
        this.invertOutput = invert;
        turnOff();
    }
}
