package com.sb.elsinore.models;


import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.sb.elsinore.devices.PID.PIDMode.OFF;

/**
 * This is the DAO model for the PIDModel table
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"temperature_id"}))
public class PIDModel implements PIDInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @ElementCollection(targetClass = Temperature.class)
    private Temperature temperature;

    private boolean invertAux = false;
    private BigDecimal dutyCycle = new BigDecimal(0);

    private BigDecimal setPoint = new BigDecimal(0);
    private BigDecimal manualDuty = new BigDecimal(0);
    private BigDecimal manualTime = new BigDecimal(0);

    /* Hysteria Settings */
    private BigDecimal maxTemp = new BigDecimal(0);
    private BigDecimal minTemp = new BigDecimal(0);
    private BigDecimal minTime = new BigDecimal(0);

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "heat_settings_id")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private PIDSettings heatSetting;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cool_settings_id")
    @ElementCollection(targetClass = PIDSettings.class)
    private PIDSettings coolSetting;

    /**
     * The GPIO String values.
     */
    private String auxGPIO = null;

    /**
     * Various strings.
     */
    private com.sb.elsinore.devices.PID.PIDMode pidMode = OFF;

    public PIDModel() {
    }

    /**
     * Create a new PIDModel with minimal information.
     *
     * @param aName The Name of this PIDModel
     * @param gpio  The GPIO Pin to use.
     */
    public PIDModel(String aName, String device, String gpio) {
        this(aName, device);

        getCoolSetting().setGPIO(gpio);
        this.pidMode = OFF;
    }


    /**
     * Create a new PIDModel with minimal information.
     *
     * @param aName The Name of this PIDModel
     */
    public PIDModel(String aName, String device) {
        this.temperature = new Temperature(aName, device);
    }

    @Override
    public TemperatureInterface getTemperature() {
        return this.temperature;
    }

    /**
     * Get the unique name for this PIDModel, it will be the underlying temperature probe
     *
     * @return The name set for this PIDModel
     */
    @Override
    public String getName() {
        return this.temperature.getName();
    }

    /**
     * Get the current pidMode.
     *
     * @return The pidMode.
     */
    @Override
    public com.sb.elsinore.devices.PID.PIDMode getPidMode() {
        return this.pidMode;
    }

    /**
     * Set the pidMode
     *
     * @param pidMode Must be "off", "auto", "manual", "hysteria"
     */
    @Override
    public void setPidMode(com.sb.elsinore.devices.PID.PIDMode pidMode) {
        this.pidMode = pidMode;
    }

    /**
     * @return Get the GPIO Pin
     */
    @Override
    public String getHeatGPIO() {
        return getHeatSetting().getGPIO();
    }

    @Override
    public String getCoolGPIO() {
        return getCoolSetting().getGPIO();
    }

    /**
     * @return Get the Aux GPIO Pin
     */
    @Override
    public String getAuxGPIO() {
        return this.auxGPIO;
    }

    /**
     * @return Get the current duty cycle percentage
     */
    @Override
    public BigDecimal getDutyCycle() {
        return this.dutyCycle;
    }

    /********
     * Set the duty time in %.
     * @param duty Duty Cycle percentage
     */
    @Override
    public void setDutyCycle(BigDecimal duty) {
        if (duty.doubleValue() > 100) {
            duty = new BigDecimal(100);
        } else if (duty.doubleValue() < -100) {
            duty = new BigDecimal(-100);
        }
        if (this.dutyCycle.compareTo(duty) != 0) {
            return;
        }

        this.dutyCycle = duty;
    }

    /**
     * @return Get the PIDModel Target temperature
     */
    @Override
    public BigDecimal getSetPoint() {
        return this.setPoint;
    }

    /****
     * Set the target temperature for the auto pidMode.
     * @param setPoint The new temperature in F.
     */
    @Override
    public void setSetPoint(BigDecimal setPoint) {
        if (setPoint.doubleValue() < 0) {
            setPoint = BigDecimal.ZERO;
        }
        this.setPoint = setPoint.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * @return True if the Auxiliary output is HIGH for off, and LOW for on
     */
    @Override
    public Boolean isAuxInverted() {
        return this.invertAux;
    }

    /**
     * Set the cooling values.
     *
     * @param gpio  The GPIO to be used
     * @param duty  The new duty time in seconds
     * @param delay The start/stop delay in minutes
     * @param cycle The Cycle time for
     * @param p     the proportional value
     * @param i     the integral value
     * @param d     the Derivative value
     */
    @Override
    public void setCool(final String gpio, final BigDecimal duty,
                        final BigDecimal delay, final BigDecimal cycle, final BigDecimal p,
                        final BigDecimal i, final BigDecimal d) {
        PIDSettings coolSetting = getCoolSetting();
        setDutyCycle(duty);
        coolSetting.setGPIO(gpio);
        coolSetting.setDelay(delay);
        coolSetting.setCycleTime(cycle);
        coolSetting.setProportional(p);
        coolSetting.setIntegral(i);
        coolSetting.setDerivative(d);
    }

    @Override
    public BigDecimal getMinTemp() {
        return this.minTemp;
    }

    @Override
    public void setMinTemp(BigDecimal minTemp) {
        if (minTemp == null || minTemp.compareTo(this.minTemp) == 0) {
            return;
        }
        this.minTemp = minTemp;
    }

    @Override
    public BigDecimal getMaxTemp() {
        return this.maxTemp;
    }

    @Override
    public void setMaxTemp(BigDecimal maxTemp) {
        if (maxTemp == null || maxTemp.compareTo(this.maxTemp) == 0) {
            return;
        }
        this.maxTemp = maxTemp;
    }

    @Override
    public BigDecimal getMinTime() {
        return this.minTime;
    }

    @Override
    public void setMinTime(BigDecimal minTime) {
        if (minTime == null || minTime.compareTo(this.minTime) == 0) {
            return;
        }
        this.minTime = minTime;
    }

    /**
     * Invert the outputs.
     *
     * @param invert True to invert the outputs.
     */
    @Override
    public void setInvertAux(final boolean invert) {
        if (invert == this.invertAux) {
            return;
        }
        this.invertAux = invert;
    }

    @Override
    public BigDecimal getManualDuty() {
        return this.manualDuty;
    }

    @Override
    public void setManualDuty(BigDecimal duty) {
        if (duty == null || duty.compareTo(this.manualDuty) == 0) {
            return;
        }
        this.manualDuty = duty;

    }

    @Override
    public BigDecimal getManualTime() {
        return this.manualTime;
    }

    @Override
    public void setManualTime(BigDecimal time) {
        if (time == null || time.compareTo(this.manualTime) == 0) {
            return;
        }
        this.manualTime = time;
    }

    @Override
    public PIDSettings getHeatSetting() {
        if (this.heatSetting == null) {
            this.heatSetting = new PIDSettings();
        }
        return this.heatSetting;
    }

    @Override
    public PIDSettings getCoolSetting() {
        if (this.coolSetting == null) {
            this.coolSetting = new PIDSettings();
        }
        return this.coolSetting;
    }
}
