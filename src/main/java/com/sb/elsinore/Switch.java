package com.sb.elsinore;

import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

/**
 * A helper class for switch control. not very complex. Designed to control a
 * single GPIO pin with a straight forward on/off functionality
 * 
 * @author Doug Edey
 * 
 */
public class Switch implements Comparable<Switch> {

    public static final String GPIO = "gpio";
    public static final String POSITION = "position";
    /**
     * Current switch name.
     */
    private String name;
    /**
     * the outpin for the switch.
     */
    private OutPin output = null;
    private boolean invertOutput = false;
    private int position = -1;

    /**
     * The Constructor.
     * 
     * @param newName
     *            The name of this switch
     * @param pinName
     *            The GPIO pin name
     * @throws InvalidGPIOException
     *             If there's a problem opening the GPIO.
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

        try {
            this.output = new OutPin(pinName);
            this.turnOff();
        } catch (InvalidGPIOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    public void setName(String name){
        this.name = name;
    }


    public void setGPIO(String gpio) throws InvalidGPIOException {
        if (!this.output.getGPIOName().equalsIgnoreCase(gpio))
        {
            this.output.close();
            this.output = new OutPin(gpio);
        }
    }

    /**
     * @return The current state of the switch, true for on. False for off.
     */
    public final boolean getStatus() {
        try {
            if (this.invertOutput) {
                return output.getValue().equals("0");
            } else {
                return output.getValue().equals("1");
            }
        } catch (Exception e) {
            BrewServer.LOG.warning("Couldn't toggle switch: " + e);
            return false;
        }
    }

    /**
     * Turn on the switch.
     */
    public final void turnOn() {
        if (this.invertOutput) {
            output.setValue(false);
        } else {
            output.setValue(true);
        }
    }

    /**
     * Turn off the switch.
     */
    public final void turnOff() {
        if (this.invertOutput) {
            output.setValue(true);
        } else {
            output.setValue(false);
        }
    }

    /**
     * @return The name of this switch
     */
    public final String getName() {
        return name;
    }

    /**
     * @return The current GPIO Pin.
     */
    public final String getGPIO() {
        return output.getGPIOName();
    }

    /**
     * @return Return the current name with _ instead of spaces.
     */
    public final String getNodeName() {

        return "_" + name.replace(" ", "_");
    }

    /**
     * Set the new position of this switch.
     * @param newPos The new position to use.
     */
    public void setPosition(int newPos) {
        this.position = newPos;
    }

    public int getPosition() {
        return this.position;
    }

    @Override
    public int compareTo(Switch o) {
        return Integer.compare(this.position, o.getPosition());
    }
    
    public void shutdown() {
        if (output != null) {
            output.close();
        }
    }
    
    public void setInverted(boolean invert) {
        this.invertOutput = invert;
        turnOff();
    }
    
    public boolean getInverted() {
        return this.invertOutput;
    }
}
