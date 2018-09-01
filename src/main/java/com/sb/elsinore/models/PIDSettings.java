package com.sb.elsinore.models;

import javax.persistence.*;
import java.math.BigDecimal;


/**
 * Hosts the setting specific to a PIDModel profile
 * Created by Douglas on 2016-05-15.
 */
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"gpio"})
}
)
public class PIDSettings implements com.sb.elsinore.interfaces.PIDSettingsInterface {

    public static final String HEAT = "heat";
    public static final String COOL = "cool";
    public static final String DELAY = "delay";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
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
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public String getGPIO() {
        return this.gpio;
    }

    @Override
    public void setGPIO(String newGPIO) {
        this.gpio = newGPIO;
    }

    @Override
    public BigDecimal getCycleTime() {
        return this.cycle_time;
    }

    @Override
    public void setCycleTime(BigDecimal cycle_time) {
        this.cycle_time = cycle_time;
    }

    @Override
    public BigDecimal getProportional() {
        return this.proportional;
    }

    @Override
    public void setProportional(BigDecimal proportional) {
        this.proportional = proportional;
    }

    @Override
    public BigDecimal getIntegral() {
        return this.integral;
    }

    @Override
    public void setIntegral(BigDecimal integral) {
        this.integral = integral;
    }

    @Override
    public BigDecimal getDerivative() {
        return this.derivative;
    }

    @Override
    public void setDerivative(BigDecimal derivative) {
        this.derivative = derivative;
    }

    @Override
    public BigDecimal getDelay() {
        return this.delay;
    }

    @Override
    public void setDelay(BigDecimal delay) {
        this.delay = delay;
    }

    @Override
    public PID_MODE getMode() {
        return this.mode;
    }

    @Override
    public void setMode(PID_MODE mode) {
        this.mode = mode;
    }

    @Override
    public String getProfile() {
        return this.profile;
    }

    @Override
    public void setProfile(String profile) {
        this.profile = profile;
    }

    @Override
    public boolean isInverted() {
        return this.inverted;
    }

    @Override
    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    /**
     * Hosts the mode of these settings (Heat or COOL for now)
     */
    public enum PID_MODE {
        HEAT,
        COOL,
    }

}
