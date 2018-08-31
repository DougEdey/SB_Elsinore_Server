package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;
import com.sb.elsinore.models.SwitchInterface;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

/**
 * A helper class for switch control. not very complex. Designed to control a
 * single GPIO pin with a straight forward on/off functionality
 *
 * @author Doug Edey
 */
public class Switch implements Comparable<Switch>, SwitchInterface {

    /**
     * the outpin for the switch.
     */
    private OutPin output;

    private SwitchInterface switchInterface;

    public Switch(SwitchInterface switchInterface) throws InvalidGPIOException {
        this.switchInterface = switchInterface;
        this.output = new OutPin(switchInterface.getGPIOName());
        turnOff();
    }

    /**
     * @return The current state of the switch, true for on. False for off.
     */
    public boolean getStatus() {
        try {
            if (isOutputInverted()) {
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
        if (isOutputInverted()) {
            this.output.setValue(false);
        } else {
            this.output.setValue(true);
        }
    }

    /**
     * Turn off the switch.
     */
    public void turnOff() {
        if (isOutputInverted()) {
            this.output.setValue(true);
        } else {
            this.output.setValue(false);
        }
    }


    /**
     * @return The current GPIO Pin.
     */
    public String getGPIO() {
        return this.output.getGPIOName();
    }


    @Override
    public int compareTo(Switch o) {
        return Integer.compare(this.getPosition(), o.getPosition());
    }

    public void shutdown() {
        if (this.output != null) {
            turnOff();
            this.output.close();
        }
    }


    @Override
    public Long getId() {
        return this.switchInterface.getId();
    }

    @Override
    public String getName() {
        return this.switchInterface.getName();
    }

    @Override
    public void setName(String name) {
        this.switchInterface.setName(name);
    }

    @Override
    public String getGPIOName() {
        return this.switchInterface.getGPIOName();
    }

    @Override
    public void setGPIOName(String gpioName) {
        if (!this.output.getGPIOName().equalsIgnoreCase(gpioName)) {
            this.output.close();
            try {
                this.output = new OutPin(gpioName);
                this.switchInterface.setGPIOName(gpioName);
            } catch (InvalidGPIOException e) {
                BrewServer.LOG.warning("Failed to start Switch GPIO: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isOutputInverted() {
        return this.switchInterface.isOutputInverted();
    }

    @Override
    public void setOutputInverted(boolean inverted) {
        this.switchInterface.setOutputInverted(inverted);
    }

    @Override
    public int getPosition() {
        return this.switchInterface.getPosition();
    }

    @Override
    public void setPosition(int position) {
        this.switchInterface.setPosition(position);
    }
}
