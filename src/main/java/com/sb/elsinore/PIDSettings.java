package com.sb.elsinore;

import java.math.BigDecimal;
import java.util.Observable;

/**
 * Hosts the setting specific to a PID profile
 * Created by Douglas on 2016-05-15.
 */
public class PIDSettings extends Observable {

    public String getGPIO()
    {
        return gpio;
    }

    public void setGPIO(String newGPIO)
    {
        gpio = newGPIO;
    }
    public BigDecimal getCycleTime() {
        return cycle_time;
    }

    public void setCycleTime(BigDecimal cycle_time) {
        this.cycle_time = cycle_time;
        setChanged();
        notifyObservers("cycle_time");
    }

    public BigDecimal getProportional() {
        return proportional;
    }

    public void setProportional(BigDecimal proportional) {
        this.proportional = proportional;
        setChanged();
        notifyObservers("proportional");
    }

    public BigDecimal getIntegral() {
        return integral;
    }

    public void setIntegral(BigDecimal integral) {
        this.integral = integral;
        setChanged();
        notifyObservers("integral");
    }

    public BigDecimal getDerivative() {
        return derivative;
    }

    public void setDerivative(BigDecimal derivative) {
        this.derivative = derivative;
        setChanged();
        notifyObservers("derivative");
    }

    public BigDecimal getDelay() {
        return delay;
    }

    public void setDelay(BigDecimal delay) {
        this.delay = delay;
        setChanged();
        notifyObservers("delay");
    }

    public PID_MODE getMode() {
        return mode;
    }

    public void setMode(PID_MODE mode) {
        this.mode = mode;
        setChanged();
        notifyObservers("mode");
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
        setChanged();
        notifyObservers("profile");
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
        setChanged();
        notifyObservers("inverted");
    }

    /**
     * Hosts the mode of these settings (Heat or COOL for now)
     */
    public enum PID_MODE {
        HEAT,
        COOL,
    }

    /**
     * values to hold the settings.
     */
    private BigDecimal
            cycle_time = new BigDecimal(0),
            proportional = new BigDecimal(0),
            integral = new BigDecimal(0),
            derivative = new BigDecimal(0),
            delay = new BigDecimal(0);
    private PID_MODE mode = PID_MODE.HEAT;
    private String profile = "", gpio = null;
    private boolean inverted = false;

    /**
     * Default constructor.
     */
    PIDSettings() {
        cycle_time = proportional =
                integral = derivative = new BigDecimal(0.0);
    }

}
