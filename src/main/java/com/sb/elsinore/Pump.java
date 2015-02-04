package com.sb.elsinore;

import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

/**
 * A helper class for pump control. not very complex. Designed to control a
 * single GPIO pin with a straight forward on/off functionality
 * 
 * @author Doug Edey
 * 
 */
public class Pump implements Comparable<Pump> {

    /**
     * Current pump name.
     */
    private String name;
    /**
     * the outpin for the pump.
     */
    private OutPin output = null;
    private boolean invertOutput = false;
    private int position = -1;

    /**
     * The Constructor.
     * 
     * @param newName
     *            The name of this pump
     * @param pinName
     *            The GPIO pin name
     * @throws InvalidGPIOException
     *             If there's a problem opening the GPIO.
     */
    public Pump(final String newName, final String pinName)
            throws InvalidGPIOException {
        this.name = newName.replace("_", " ");

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

    /**
     * @return The current state of the pump, true for on. False for off.
     */
    public final boolean getStatus() {
        try {
            if (this.invertOutput) {
                return output.getValue().equals("0");
            } else {
                return output.getValue().equals("1");
            }
        } catch (Exception e) {
            BrewServer.LOG.warning("Couldn't toggle pump: " + e);
            return false;
        }
    }

    /**
     * Turn on the pump.
     */
    public final void turnOn() {
        if (this.invertOutput) {
            output.setValue(false);
        } else {
            output.setValue(true);
        }
    }

    /**
     * Turn off the pump.
     */
    public final void turnOff() {
        if (this.invertOutput) {
            output.setValue(true);
        } else {
            output.setValue(false);
        }
    }

    /**
     * @return The name of this pump
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
        return name.replace(" ", "_");
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
    public int compareTo(Pump o) {
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
