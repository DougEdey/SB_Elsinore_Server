package com.sb.elsinore;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sb.elsinore.UrlEndpoints.*;

/**
 * The PID class calculates the Duty cycles and updates the output control.
 *
 * @author Doug Edey
 */
public class PID extends Temp {

    public static final String TYPE = "PID";

    public static final String HYSTERIA = "hysteria";
    public static final String AUTO = "auto";
    public static final String MANUAL = "manual";
    public static final String OFF = "off";
    @Expose
    @SerializedName(INVERTED)
    private boolean invertAux = false;
    @Expose
    @SerializedName(DUTYCYCLE)
    private BigDecimal duty_cycle = new BigDecimal(0);
    @Expose
    @SerializedName(ACTUAL_DUTY)
    private BigDecimal calculatedDuty = new BigDecimal(0);
    @Expose
    @SerializedName(SETPOINT)
    private BigDecimal set_point = new BigDecimal(0);
    @Expose
    @SerializedName(MANUAL_DUTY)
    private BigDecimal manual_duty = new BigDecimal(0);
    @Expose
    @SerializedName(MANUAL_TIME)
    private BigDecimal manual_time = new BigDecimal(0);

    /* Hysteria Settings */
    @Expose
    @SerializedName(MAX)
    private BigDecimal max = new BigDecimal(0);
    @Expose
    @SerializedName(MIN)
    private BigDecimal min = new BigDecimal(0);
    @Expose
    @SerializedName(TIME)
    private BigDecimal minTime = new BigDecimal(0);

    @Expose
    @SerializedName("aux")
    private OutPin auxPin = null;

    /**
     * Settings for the heating and cooling.
     */

    @Expose
    @SerializedName(HEAT)
    private PIDSettings heatSetting;

    @Expose
    @SerializedName(COOL)
    private PIDSettings coolSetting;

    /**
     * Create a new PID with minimal information.
     *
     * @param aName The Name of this PID
     * @param gpio  The GPIO Pin to use.
     */
    public PID(String aName, String device, String gpio) {
        super(aName, device);
        this.type = TYPE;
        getSettings(HEAT).setGPIO(gpio);
        this.mode = OFF;
    }

    /**
     * Create a new PID with minimal information.
     *
     * @param aName The Name of this PID
     */
    public PID(String aName, String device) {
        super(aName, device);
        this.type = TYPE;
    }

    /**
     * Set the mode
     *
     * @param mode Must be "off", "auto", "manual", "hysteria"
     */
    public void setMode(String mode) {
        if (mode.equalsIgnoreCase(OFF)) {
            this.mode = OFF;
            return;
        }
        if (mode.equalsIgnoreCase(MANUAL)) {
            this.mode = MANUAL;
            return;
        }
        if (mode.equalsIgnoreCase(AUTO)) {
            this.mode = AUTO;
            return;
        }
        if (mode.equalsIgnoreCase(HYSTERIA)) {
            this.mode = HYSTERIA;
        }
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

    /******
     * Update the current values of the PID.
     * @param m String indicating mode (manual, auto, off)
     * @param duty Duty Cycle % being set
     * @param cycle Cycle Time in seconds
     * @param setpoint Target temperature for auto mode
     * @param p Proportional value
     * @param i Integral Value
     * @param d Derivative value
     */
    void updateValues(final String m, final BigDecimal duty,
                      final BigDecimal cycle, final BigDecimal setpoint, final BigDecimal p,
                      final BigDecimal i, final BigDecimal d) {
        this.mode = m;
        if (this.mode.equals(MANUAL)) {
            this.duty_cycle = duty;
        }
        this.heatSetting.setCycleTime(cycle);
        this.set_point = setpoint;

        this.heatSetting.setProportional(p);
        this.heatSetting.setIntegral(i);
        this.heatSetting.setDerivative(d);

        BrewServer.LOG.info("Mode " + this.mode + " " + this.heatSetting.getProportional() + ": "
                + this.heatSetting.getIntegral() + ": " + this.heatSetting.getDerivative());
        LaunchControl.getInstance().saveEverything();
    }

    /**
     * Set the PID to hysteria mode.
     *
     * @param newMax     The maximum value to disable heating at
     * @param newMin     The minimum value to start heating at
     * @param newMinTime The minimum amount of time to keep the burner on for
     */
    void setHysteria(final BigDecimal newMin,
                     final BigDecimal newMax, final BigDecimal newMinTime) {

        if (newMax.compareTo(BigDecimal.ZERO) <= 0
                && newMax.compareTo(newMin) <= 0) {
            throw new NumberFormatException(
                    "Min value is less than the max value");
        }

        if (newMinTime.compareTo(BigDecimal.ZERO) < 0) {
            throw new NumberFormatException("Min Time is negative");
        }

        this.max = newMax;
        this.min = newMin;
        this.minTime = newMinTime;
    }

    void useHysteria() {
        this.mode = HYSTERIA;
    }


    /********
     * Set the duty time in %.
     * @param duty Duty Cycle percentage
     */
    public void setDuty(BigDecimal duty) {
        if (duty.doubleValue() > 100) {
            duty = new BigDecimal(100);
        } else if (duty.doubleValue() < -100) {
            duty = new BigDecimal(-100);
        }

        this.duty_cycle = duty;
    }

    /****
     * Set the target temperature for the auto mode.
     * @param temp The new temperature in F.
     */
    public void setTemp(BigDecimal temp) {
        if (temp.doubleValue() < 0) {
            temp = BigDecimal.ZERO;
        }
        this.set_point = temp.setScale(2, BigDecimal.ROUND_CEILING);
    }

    /*******
     * set an auxilliary manual GPIO (for dual element systems).
     * @param gpio The GPIO to use as an aux
     */
    void setAux(String gpio, boolean invert) {
        if (gpio == null || gpio.length() == 0) {
            this.auxGPIO = "";

            return;
        }

        // Only do this is the pin has changed
        String newGPIO = detectGPIO(gpio);
        if (newGPIO == null) {
            if (this.auxPin != null) {
                this.auxPin.close();
                this.auxPin = null;
            }
        } else if (!newGPIO.equalsIgnoreCase(this.auxGPIO)) {
            this.auxGPIO = newGPIO;
            try {
                this.auxPin = new OutPin(this.auxGPIO);
            } catch (InvalidGPIOException i) {
                if (this.auxPin != null) {
                    this.auxPin.close();
                    this.auxPin = null;
                }
                BrewServer.LOG.warning(String.format("Failed to setup GPIO for the aux Pin provided %s", i.getMessage()));
            }
        }


        this.setAuxInverted(invert);
        setAux(false);
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
     * Get the current mode.
     * @return The mode.
     */
    public String getMode() {
        return this.mode;
    }

    /**
     * @return Get the GPIO Pin
     */
    String getHeatGPIO() {
        return getSettings(HEAT).getGPIO();
    }

    String getCoolGPIO() {
        return getSettings(COOL).getGPIO();
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
    public BigDecimal getDuty() {
        return this.duty_cycle;
    }

    /**
     * @return Get the PID Target temperature
     */
    BigDecimal getSetPoint() {
        return this.set_point;
    }

    //PRIVATE ///

    /**
     * The GPIO String values.
     */
    private String auxGPIO = null;

    /**
     * Various strings.
     */
    @Expose
    @SerializedName(MODE)
    private String mode = OFF;
    private String fName = null;

    /**
     * @return Get the current temperature
     */
    public BigDecimal getTemp() {
        return super.getTemp();
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
    @SuppressWarnings("unused")
    public void setCool(final String gpio, final BigDecimal duty,
                        final BigDecimal delay, final BigDecimal cycle, final BigDecimal p,
                        final BigDecimal i, final BigDecimal d) {
        getSettings(COOL).setGPIO(gpio);

        setDuty(duty);
        getSettings(COOL).setDelay(delay);
        getSettings(COOL).setCycleTime(cycle);
        getSettings(COOL).setProportional(p);
        getSettings(COOL).setIntegral(i);
        getSettings(COOL).setDerivative(d);
    }

    /*****
     * Helper function to return a map of the current status.
     * @return The current status of the temperature probe.
     */
    String getJsonStatus() {
        return LaunchControl.getInstance().toJsonString(this);
    }


    public BigDecimal getMin() {
        return this.min;
    }

    public BigDecimal getMax() {
        return this.max;
    }

    public BigDecimal getMinTime() {
        return this.minTime;
    }


    void setManualDuty(BigDecimal duty) {
        if (duty != null) {
            this.manual_duty = duty;
        }
    }

    void setManualTime(BigDecimal time) {
        if (time != null) {
            this.manual_time = time;
        }
    }

    /**
     * Invert the outputs.
     *
     * @param invert True to invert the outputs.
     */
    void setAuxInverted(final boolean invert) {
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

    BigDecimal getManualCycle() {
        return this.manual_duty;
    }

    BigDecimal getManualTime() {
        return this.manual_time;
    }

    public PIDSettings getSettings(String type) {
        switch (type) {
            case HEAT:
                if (this.heatSetting == null) {
                    this.heatSetting = new PIDSettings();
                }
                return this.heatSetting;
            case COOL:
                if (this.coolSetting == null) {
                    this.coolSetting = new PIDSettings();
                }
                return this.coolSetting;
        }
        return null;
    }
}
