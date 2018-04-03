package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;
import com.sb.elsinore.Device;
import com.sb.elsinore.PIDSettings;
import jGPIO.OutPin;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sb.elsinore.devices.PID.PIDMode.OFF;

/**
 * The PID class calculates the Duty cycles and updates the output control.
 *
 * @author Doug Edey
 */
@Entity
@DiscriminatorValue("P")
public class PID extends TempProbe {

    public static final String TYPE = "PID";
    /**
     * Aux -> The Aux PIN has changed
     * Heat -> The heat settings have changed
     * Cool -> The cool settings have changed
     * Mode -> The PID mode (on/off/auto/hysteria) and temp has changed
     */
    @Transient
    private boolean auxDirty, heatDirty, coolDirty, modeDirty;

    public enum PIDMode {
        HYSTERIA("Hysteria"),
        AUTO("Auto"),
        MANUAL("Manual"),
        OFF("Off");

        private String value;

        PIDMode(String value) {
            this.value = value;
        }

        public String toString() {
            return this.value;
        }


    }

    private boolean invertAux = false;
    private BigDecimal dutyCycle = new BigDecimal(0);
    private BigDecimal calculatedDuty = new BigDecimal(0);
    private BigDecimal setPoint = new BigDecimal(0);
    private BigDecimal manualDuty = new BigDecimal(0);
    private BigDecimal manualTime = new BigDecimal(0);

    /* Hysteria Settings */
    private BigDecimal maxTemp = new BigDecimal(0);
    private BigDecimal minTemp = new BigDecimal(0);
    private BigDecimal minTime = new BigDecimal(0);

    @Transient
    private OutPin auxPin = null;

    /**
     * Settings for the heating and cooling.
     */

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "heatSettings_id")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    //@RestResource(path = "pidSettings", rel = "pidSettings")
    private PIDSettings heatSetting;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coolSettings_id")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    //@RestResource(rel = "pidSettings")
    private PIDSettings coolSetting;

    /**
     * Create a new PID with minimal information.
     *
     * @param aName The Name of this PID
     * @param gpio  The GPIO Pin to use.
     */
    public PID(String aName, String device, String gpio) {
        this(aName, device);

        getCoolSetting().setGPIO(gpio);
        this.pidMode = OFF;
    }

    public PID() {

    }

    /**
     * Create a new PID with minimal information.
     *
     * @param aName The Name of this PID
     */
    public PID(String aName, String device) {
        super(aName, device);
    }

    public PID(Device device) {
        super(device);
        if (!(device instanceof PID)) {
            return;
        }
        // do stuff to copy over from the PID
        PID other = (PID) device;
        this.heatSetting = other.heatSetting;
        this.coolSetting = other.coolSetting;
        this.auxGPIO = other.auxGPIO;
    }

    /**
     * Set the pidMode
     *
     * @param pidMode Must be "off", "auto", "manual", "hysteria"
     */
    public void setPidMode(PIDMode pidMode) {
        this.pidMode = pidMode;
    }

    /**
     * Determine if the GPIO is valid and return it if it is.
     *
     * @param gpio The GPIO String to check.
     * @return The GPIO pin if it's valid, or blank if it's not.
     */
    private String detectGPIO(final String gpio) {
        // Determine what kind of GPIO Mapping we have
        Pattern pinPattern = Pattern.compile("(GPIO)([0-9])_([0-9]+)");
        Pattern pinPatternAlt = Pattern.compile("(GPIO)?_?([0-9]+)");

        Matcher pinMatcher = pinPattern.matcher(gpio);

        BrewServer.LOG.info(gpio + " Matches: " + pinMatcher.groupCount());

        if (pinMatcher.matches()) {
            // Beagleboard style input
            BrewServer.LOG.info("Matched GPIO pinout for Beagleboard: "
                    + gpio + ". OS: " + System.getProperty("os.level"));
            return gpio;
        } else {
            pinMatcher = pinPatternAlt.matcher(gpio);
            if (pinMatcher.matches()) {
                BrewServer.LOG.info("Direct GPIO Pinout detected. OS: "
                        + System.getProperty("os.level"));
                // The last group gives us the GPIO number
                return gpio;
            } else {
                BrewServer.LOG.info("Could not match the GPIO!");
                return null;
            }
        }
    }


    /********
     * Set the duty time in %.
     * @param duty Duty Cycle percentage
     */
    public void setDutyCycle(BigDecimal duty) {
        if (duty.doubleValue() > 100) {
            duty = new BigDecimal(100);
        } else if (duty.doubleValue() < -100) {
            duty = new BigDecimal(-100);
        }
        if (this.dutyCycle.compareTo(duty) != 0) {
            return;
        }

        this.modeDirty = true;
        this.dutyCycle = duty;
    }

    /****
     * Set the target temperature for the auto pidMode.
     * @param temp The new temperature in F.
     */
    public void setTemp(BigDecimal temp) {
        if (temp.doubleValue() < 0) {
            temp = BigDecimal.ZERO;
        }
        this.setPoint = temp.setScale(2, BigDecimal.ROUND_CEILING);
    }

    /**
     * Toggle the aux pin from it's current state.
     */
    void toggleAux() {
        // Flip the aux pin value
        if (this.auxPin != null) {
            // If the value if "1" we set it to false
            // If the value is not "1" we set it to true
            BrewServer.LOG.info("Aux Pin is being set to: "
                    + !this.auxPin.getValue().equals("1"));
            setAux(!getAuxStatus());
        } else {
            BrewServer.LOG.info("Aux Pin is not set for " + this.fName);
        }
    }

    void setAux(boolean on) {
        if (this.invertAux) {
            this.auxPin.setValue(!on);
        } else {
            this.auxPin.setValue(on);
        }
    }

    /**
     * @return True if there's an aux pin
     */
    public boolean hasAux() {
        return (this.auxPin != null);
    }

    /*******
     * Get the current pidMode.
     * @return The pidMode.
     */
    public PIDMode getPidMode() {
        return this.pidMode;
    }

    /**
     * @return Get the GPIO Pin
     */
    String getHeatGPIO() {
        return getHeatSetting().getGPIO();
    }

    String getCoolGPIO() {
        return getCoolSetting().getGPIO();
    }

    /**
     * @return Get the Aux GPIO Pin
     */
    String getAuxGPIO() {
        return this.auxGPIO;
    }

    /**
     * @return Get the current duty cycle percentage
     */
    public BigDecimal getDutyCycle() {
        return this.dutyCycle;
    }

    /**
     * @return Get the PID Target temperature
     */
    public BigDecimal getSetPoint() {
        return this.setPoint;
    }

    public void setSetPoint(BigDecimal setPoint) {
        if (setPoint == null || setPoint.compareTo(this.setPoint) == 0) {
            return;
        }
        this.modeDirty = true;
        this.setPoint = setPoint;
    }

    //PRIVATE ///

    /**
     * The GPIO String values.
     */
    private String auxGPIO = null;

    /**
     * Various strings.
     */
    private PIDMode pidMode = OFF;
    private String fName = null;

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
    @SuppressWarnings("unused")
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

    public void setMinTime(BigDecimal minTime) {
        if (minTime == null || minTime.compareTo(this.minTime) == 0) {
            return;
        }
        this.modeDirty = true;
        this.minTime = minTime;
    }

    public void setMinTemp(BigDecimal minTemp) {
        if (minTemp == null || minTemp.compareTo(this.minTemp) == 0) {
            return;
        }
        this.modeDirty = true;
        this.minTemp = minTemp;
    }

    public void setMaxTemp(BigDecimal maxTemp) {
        if (maxTemp == null || maxTemp.compareTo(this.maxTemp) == 0) {
            return;
        }
        this.modeDirty = true;
        this.maxTemp = maxTemp;
    }

    public BigDecimal getMinTemp() {
        return this.minTemp;
    }

    public BigDecimal getMaxTemp() {
        return this.maxTemp;
    }

    public BigDecimal getMinTime() {
        return this.minTime;
    }


    void setManualDuty(BigDecimal duty) {
        if (duty == null || duty.compareTo(this.manualDuty) == 0) {
            return;
        }
        this.modeDirty = true;
        this.manualDuty = duty;

    }

    void setManualTime(BigDecimal time) {
        if (time == null || time.compareTo(this.manualTime) == 0) {
            return;
        }
        this.modeDirty = true;
        this.manualTime = time;

    }

    /**
     * Invert the outputs.
     *
     * @param invert True to invert the outputs.
     */
    void setInvertAux(final boolean invert) {
        if (invert == this.invertAux) {
            return;
        }
        this.auxDirty = true;

        this.invertAux = invert;
    }

    /**
     * @return True if this device has inverted outputs.
     */
    boolean isAuxInverted() {
        return this.invertAux;
    }

    private boolean getAuxStatus() {
        if (this.invertAux) {
            return this.auxPin.getValue().equals("0");
        }
        return this.auxPin.getValue().equals("1");
    }


    public BigDecimal getManualDuty() {
        return this.manualDuty;
    }

    public BigDecimal getManualTime() {
        return this.manualTime;
    }

    public PIDSettings getHeatSetting() {
        if (this.heatSetting == null) {
            this.heatSetting = new PIDSettings();
        }
        return this.heatSetting;
    }

    public PIDSettings getCoolSetting() {
        if (this.coolSetting == null) {
            this.coolSetting = new PIDSettings();
        }
        return this.coolSetting;
    }
}
