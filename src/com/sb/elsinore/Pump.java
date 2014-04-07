package com.sb.elsinore;

import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

/**
 * A helper class for pump control. not very complex.
 * Designed to control a single GPIO pin with a
 * straight forward on/off functionality
 * @author Doug Edey
 *
 */
public class Pump {

    /**
     * Current pump name.
     */
    private String name;
    /**
     * the outpin for the pump.
     */
    private OutPin output = null;

    /**
     * The Constructor.
     * @param newName The name of this pump
     * @param pinName The GPIO pin name
     * @throws InvalidGPIOException If there's a problem opening the GPIO.
     */
    public Pump(final String newName, final String pinName)
            throws InvalidGPIOException {
        this.name = newName.replace("_", " ");
        try {
            this.output = new OutPin(pinName);
        } catch (InvalidGPIOException e) {
            throw e;
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return The current state of the pump, true for on. False for off.
     */
    public final boolean getStatus() {
        try {
            return output.getValue().equals("1");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Turn on the pump.
     */
    public final void turnOn() {
        output.setValue(true);
    }

    /**
     * Turn off the pump.
     */
    public final void turnOff() {
        output.setValue(false);
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
}
