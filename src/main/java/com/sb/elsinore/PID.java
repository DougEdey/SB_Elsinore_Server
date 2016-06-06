package com.sb.elsinore;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The PID class calculates the Duty cycles and updates the output control.
 * @author Doug Edey
 *
 */
public class PID extends Temp {

    private boolean invertAux = false;
    private BigDecimal duty_cycle = new BigDecimal(0);
    private BigDecimal calculatedDuty = new BigDecimal(0);
    private BigDecimal set_point = new BigDecimal(0);
    private BigDecimal manual_duty = new BigDecimal(0);
    private BigDecimal manual_time = new BigDecimal(0);
    
    /* Hysteria Settings */
    private BigDecimal max = new BigDecimal(0);
    private BigDecimal min = new BigDecimal(0);
    private BigDecimal minTime = new BigDecimal(0);

    private OutPin auxPin = null;

    /**
     * Settings for the heating and cooling.
     */
    private PIDSettings heatSetting = new PIDSettings();
    private PIDSettings coolSetting = new PIDSettings();

    void setHeatDelay(BigDecimal heatDelay) {
        getHeatSetting().setDelay(heatDelay);
    }
    
    /**
     * Create a new PID with minimal information.
     * @param aName The Name of this PID
     * @param gpio The GPIO Pin to use.
     */
    public PID(String aName, String device, String gpio) {
        super(aName, device);
        setHeatGPIO(gpio);
        this.mode = "off";
    }

    /**
     * Create a new PID with minimal information.
     * @param aName The Name of this PID
     */
    public PID(String aName, String device) {
        super(aName, device);
    }

    /**
     * Set the mode
     * @param mode Must be "off", "auto", "manual", "hysteria"
     */
    public void setMode(String mode) {
        if (mode.equalsIgnoreCase("off")) {
            this.mode = "off";
            return;
        }
        if (mode.equalsIgnoreCase("manual")) {
            this.mode = "manual";
            return;
        }
        if (mode.equalsIgnoreCase("auto")) {
            this.mode = "auto";
            return;
        }
        if (mode.equalsIgnoreCase("hysteria")) {
            this.mode = "hysteria";
        }
    }

    /**
     * Determine if the GPIO is valid and return it if it is.
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
     * @param d Differential value
     */
    void updateValues(final String m, final BigDecimal duty,
                      final BigDecimal cycle, final BigDecimal setpoint, final BigDecimal p,
                      final BigDecimal i, final BigDecimal d) {
        this.mode = m;
        if (this.mode.equals("manual")) {
            this.duty_cycle = duty;
        }
        this.heatSetting.setCycleTime(cycle);
        this.set_point = setpoint;

        this.heatSetting.setProportional(p);
        this.heatSetting.setIntegral(i);
        this.heatSetting.setDerivative(d);

        BrewServer.LOG.info("Mode " + this.mode + " " + this.heatSetting.getProportional() + ": "
            + heatSetting.getIntegral() + ": " + this.heatSetting.getDerivative());
        LaunchControl.getInstance().saveEverything();
    }

    /**
     * Set the PID to hysteria mode.
     * @param newMax   The maximum value to disable heating at
     * @param newMin   The minimum value to start heating at
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
        this.mode = "hysteria";
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
        if (newGPIO == null)
        {
            if (auxPin != null) {
                auxPin.close();
                auxPin = null;
            }
        }
        else if(!newGPIO.equalsIgnoreCase(auxGPIO)) {
            auxGPIO = newGPIO;
            try {
                auxPin = new OutPin(auxGPIO);
            } catch (InvalidGPIOException i)
            {
                if (auxPin != null) {
                    auxPin.close();
                    auxPin = null;
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
        if (auxPin != null) {
            // If the value if "1" we set it to false
            // If the value is not "1" we set it to true
            BrewServer.LOG.info("Aux Pin is being set to: "
                    + !auxPin.getValue().equals("1"));
            setAux(!getAuxStatus());
        } else {
            BrewServer.LOG.info("Aux Pin is not set for " + this.fName);
        }
    }

    void setAux(boolean on)
    {
        if (this.invertAux) {
            auxPin.setValue(!on);
        } else {
            auxPin.setValue(on);
        }
    }
    /**
     * @return True if there's an aux pin
     */
    public boolean hasAux() {
        return (auxPin != null);
    }

    /******
     * Set the proportional value.
     * @param p the new proportional value
     */
    void setCoolP(final BigDecimal p) {
        coolSetting.setProportional(p);
    }

    /******
     * Set the integral value.
     * @param i The new Integral.
     */
    void setCoolI(final BigDecimal i) {
        coolSetting.setIntegral(i);
    }

    /******
     * Set the differential value.
     * @param d The new differential
     */
    void setCoolD(final BigDecimal d) {
        coolSetting.setDerivative(d);
    }

    /******
     * Set the proportional value.
     * @param p the new proportional value
     */
    void setHeatP(final BigDecimal p) {
        heatSetting.setProportional(p);
    }

    /******
     * Set the integral value.
     * @param i The new Integral.
     */
    void setHeatI(final BigDecimal i) {
        heatSetting.setIntegral(i);
    }

    /******
     * Set the differential value.
     * @param d The new differential
     */
    void setHeatD(final BigDecimal d) {
        heatSetting.setDerivative(d);
    }

    /*******
     * Get the current mode.
     * @return The mode.
     */
    public String getMode() {
        return mode;
    }

    /**
     * @return Get the GPIO Pin
     */
    String getHeatGPIO() {
        return heatSetting.getGPIO();
    }

    String getCoolGPIO() {
        return coolSetting.getGPIO();
    }
    
    /**
     * @return  Get the Aux GPIO Pin
     */
    String getAuxGPIO() {
        return auxGPIO;
    }

    /**
     * @return Get the current duty cycle percentage
     */
    public BigDecimal getDuty() {
        return duty_cycle;
    }

    /**
     * @return Get the PID Target temperature
     */
    BigDecimal getSetPoint() {
        return this.set_point;
    }

    /**
     * @return  Get the current Duty Cycle Time
     */
    BigDecimal getHeatCycle() {
        return heatSetting.getCycleTime();
    }

    /**
     * @return Get the current proportional value
     */
    BigDecimal getHeatP() {
        return heatSetting.getProportional();
    }

    /**
     * @return  Get the current Integral value
     */
    BigDecimal getHeatI() {
        return heatSetting.getIntegral();
    }

    /**
     * @return Get the current Differential value
     */
    BigDecimal getHeatD() {
        return heatSetting.getDerivative();
    }

    BigDecimal getHeatDelay() {
        return heatSetting.getDelay();
    }

    /**
     * @return true if the heating output is inverted.
     */
    boolean isHeatInverted() {
        return heatSetting.isInverted();
    }

    void setHeatInverted(boolean inverted) {
        if (heatSetting != null) {
            heatSetting.setInverted(inverted);
        }
    }

    void setCoolInverted(boolean inverted) {
        if (coolSetting != null) {
            coolSetting.setInverted(inverted);
        }
    }

    /**
     * @return true if the cooling output is inverted.
     */
    boolean isCoolInverted() {
        return coolSetting.isInverted();
    }
    /**
     * @return  Get the current Duty Cycle Time
     */
    BigDecimal getCoolCycle() {
        return coolSetting.getCycleTime();
    }


    /**
     * @return Get the current proportional value
     */
    BigDecimal getCoolP() {
        return coolSetting.getProportional();
    }

    /**
     * @return  Get the current Integral value
     */
    BigDecimal getCoolI() {
        return coolSetting.getIntegral();
    }

    /**
     * @return Get the current Differential value
     */
    BigDecimal getCoolD() {
        return coolSetting.getDerivative();
    }

    BigDecimal getCoolDelay() {
        return coolSetting.getDelay();
    }


  //PRIVATE ///
    /**
     * Store the previous timestamp for the update.
     */
    private BigDecimal previousTime = new BigDecimal(0);

    /**
     * The GPIO String values.
     */
    private String auxGPIO = null;

    /**
     * Various strings.
     */
    private String mode = "off", fName = null;

    /**
     * @return Get the current temperature
     */
    public BigDecimal getTemp() {
        return super.getTemp();
    }


    /**
     * Set the cooling values.
     * @param gpio The GPIO to be used
     * @param duty The new duty time in seconds
     * @param delay The start/stop delay in minutes
     * @param cycle The Cycle time for 
     * @param p the proportional value
     * @param i the integral value
     * @param d the differential value
     */
    @SuppressWarnings("unused")
    public void setCool(final String gpio, final BigDecimal duty,
            final BigDecimal delay, final BigDecimal cycle, final BigDecimal p,
            final BigDecimal i, final BigDecimal d) {
        setCoolGPIO(gpio);

        setDuty(duty);
        setCoolDelay(delay);
        setCoolCycle(cycle);
        setCoolP(p);
        setCoolI(i);
        setCoolD(d);
    }

    /**
     * @return The current status as a map
     *
    public Map<String, Object> getMapStatus() {
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("mode", getMode());
        // hack to get the real duty out
        if (getMode().contains("auto")) {
            statusMap.put("actualduty", calculatedDuty);
        }

        // The Heat settings
        Map<String, Object> heatMap = new HashMap<>();
        heatMap.put("cycle", getHeatCycle());
        heatMap.put("p", getHeatP());
        heatMap.put("i", getHeatI());
        heatMap.put("d", getHeatD());
        heatMap.put("delay", getHeatDelay());
        heatMap.put("gpio", getHeatGPIO());
        heatMap.put("inverted", isHeatInverted());
        statusMap.put("heat", heatMap);

        // The cool settings
        Map<String, Object> coolMap = new HashMap<>();
        coolMap.put("cycle", getCoolCycle());
        coolMap.put("p", getCoolP());
        coolMap.put("i", getCoolI());
        coolMap.put("d", getCoolD());
        coolMap.put("gpio", getCoolGPIO());
        coolMap.put("delay", getCoolDelay());
        coolMap.put("inverted", isCoolInverted());
        statusMap.put("cool", coolMap);

        statusMap.put("duty", getDuty());
        statusMap.put("setpoint", getSetPoint());
        statusMap.put("manualduty", this.manual_duty);
        statusMap.put("manualtime", this.manual_time);
        statusMap.put("min", this.min);
        statusMap.put("max", this.max);
        statusMap.put("time", this.minTime);

        statusMap.put("status", getStatus());

        if (auxPin != null) {
            // This value should be cached
            // but I don't trust someone to hit it with a different application
            Map<String, Object> auxStatus = new HashMap<>();
            auxStatus.put("gpio", auxGPIO);
            auxStatus.put("inverted", isAuxInverted());
            auxStatus.put("status", getAuxStatus());
            statusMap.put("aux", auxStatus);
        }

        return statusMap;
    }*/

    void delHeatGPIO() {
        heatSetting.setGPIO(null);
    }

    void delCoolGPIO() {
        coolSetting.setGPIO(null);
    }
    /**
     * Set the GPIO to a new pin, shutdown the old one first.
     * @param gpio The new GPIO to use
     */
    void setHeatGPIO(final String gpio) {
        heatSetting.setGPIO(detectGPIO(gpio));
    }

    void setCoolGPIO(final String gpio) {
        coolSetting.setGPIO(detectGPIO(gpio));
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

    PIDSettings getHeatSetting() {
        return this.heatSetting;
    }
    
    PIDSettings getCoolSetting() {
        return this.coolSetting;
    }

    void setCoolDelay(BigDecimal coolDelay) {
        this.coolSetting.setDelay(coolDelay);
    }
    
    void setCoolCycle(BigDecimal coolCycle) {
        this.coolSetting.setCycleTime(coolCycle);
    }
    
    void setHeatCycle(BigDecimal heatCycle) {
        this.heatSetting.setCycleTime(heatCycle);
    }
    
    void setManualDuty(BigDecimal duty) {
        if (duty != null) {
            this.manual_duty = duty;
        }
    }
    
    void setManualTime(BigDecimal time) {
        if (time != null)
        {
            this.manual_time = time;
        }
    }

    /**
     * Invert the outputs.
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

    private boolean getAuxStatus()
    {
        if (this.invertAux) {
            return auxPin.getValue().equals("0");
        }
        return auxPin.getValue().equals("1");
    }

    BigDecimal getManualCycle() {
        return this.manual_duty;
    }
    
    BigDecimal getManualTime() {
        return this.manual_time;
    }
}
