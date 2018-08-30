package com.sb.elsinore.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Observable;


/**
 * Hosts the setting specific to a PIDModel profile
 * Created by Douglas on 2016-05-15.
 */
@Entity
public class PIDSettings extends Observable implements Serializable {

    public static final String HEAT = "heat";
    public static final String COOL = "cool";
    public static final String DELAY = "delay";
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * values to hold the settings.
     */
    private BigDecimal cycle_time = new BigDecimal(0);
    private BigDecimal proportional = new BigDecimal(0);
    private BigDecimal integral = new BigDecimal(0);
    private BigDecimal derivative = new BigDecimal(0);
    private BigDecimal delay = new BigDecimal(0);
    private PID_MODE mode = PID_MODE.HEAT;
    private String profile = "";
    private String gpio = null;
    private boolean inverted = false;

    /**
     * Default constructor.
     */
    public PIDSettings() {
        this.cycle_time = this.proportional =
                this.integral = this.derivative = new BigDecimal(0.0);
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGPIO() {
        return this.gpio;
    }

    public void setGPIO(String newGPIO) {
        this.gpio = newGPIO;
    }

    public BigDecimal getCycleTime() {
        return this.cycle_time;
    }

    public void setCycleTime(BigDecimal cycle_time) {
        this.cycle_time = cycle_time;
        setChanged();
        notifyObservers("cycle_time");
    }

    public BigDecimal getProportional() {
        return this.proportional;
    }

    public void setProportional(BigDecimal proportional) {
        this.proportional = proportional;
        setChanged();
        notifyObservers("proportional");
    }

    public BigDecimal getIntegral() {
        return this.integral;
    }

    public void setIntegral(BigDecimal integral) {
        this.integral = integral;
        setChanged();
        notifyObservers("integral");
    }

    public BigDecimal getDerivative() {
        return this.derivative;
    }

    public void setDerivative(BigDecimal derivative) {
        this.derivative = derivative;
        setChanged();
        notifyObservers("derivative");
    }

    public BigDecimal getDelay() {
        return this.delay;
    }

    public void setDelay(BigDecimal delay) {
        this.delay = delay;
        setChanged();
        notifyObservers(DELAY);
    }

    public PID_MODE getMode() {
        return this.mode;
    }

    public void setMode(PID_MODE mode) {
        this.mode = mode;
        setChanged();
        notifyObservers("mode");
    }

    public String getProfile() {
        return this.profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
        setChanged();
        notifyObservers("profile");
    }

    public boolean isInverted() {
        return this.inverted;
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

}
