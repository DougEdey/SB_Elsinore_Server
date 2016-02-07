package com.sb.elsinore;
import com.sb.elsinore.devices.OutputDevice;
import com.sb.util.MathUtil;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The PID class calculates the Duty cycles and updates the output control.
 * @author Doug Edey
 *
 */
public final class PID implements Runnable {

    public static final String DUTY_CYCLE = "duty_cycle";
    public static final String DUTY_TIME = "duty_time";
    public static final String SET_POINT = "set_point";
    public static final String GPIO = "gpio";
    public static final String MODE = "mode";
    public static final String CYCLE_TIME = "cycle_time";
    public static final String PROPORTIONAL = "proportional";
    public static final String INTEGRAL = "integral";
    public static final String DERIVATIVE = "derivative";
    public static final String INVERT = "invert";
    public static final String DELAY = "delay";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String TIME = "time";
    public static final String CUTOFF = "cutoff";
    public static final String CUTOFF_ENABLED = "cutoff_enabled";
    public static final String CALIBRATION = "calibration";
    public static final String AUX = "aux";
    public static final String HIDDEN = "hidden";
    public static final String ID = "id";
    public static final String HEAT = "heat";
    public static final String COOL = "cool";

    /**
     * Thousand BigDecimal multiplier.
     */
    private BigDecimal THOUSAND = new BigDecimal(1000);

    /**
     * The Output control thread.
     */
    private Thread outputThread = null;
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

    private boolean running = true;
    private BigDecimal heatDelay;

    public void setHeatDelay(BigDecimal heatDelay) {
        this.heatSetting.delay = heatDelay;
    }

    /**
     * Inner class to hold the current settings.
     * @author Doug Edey
     *
     */
    public class Settings {
        /**
         * values to hold the settings.
         */
        public BigDecimal
            cycle_time = new BigDecimal(0),
            proportional = new BigDecimal(0),
            integral = new BigDecimal(0),
            derivative = new BigDecimal(0),
            delay = new BigDecimal(0);
        boolean inverted = false;

        /**
         * Default constructor.
         */
        public Settings() {
            cycle_time = proportional =
                    integral = derivative = new BigDecimal(0.0);
        }
    }

    public OutputControl outputControl = null;
    
    /**
     * Create a new PID with minimal information.
     * @param aTemp The Temperature probe object to use
     * @param aName The Name of this PID
     * @param gpio The GPIO Pin to use.
     */
    public PID(final Temp aTemp, final String aName, final String gpio) {
        this.fName = aName;
        this.fTemp = aTemp;

        this.heatGPIO = detectGPIO(gpio);
        this.mode = "off";
        this.heatSetting = new Settings();
    }

    /**
     * Create a new PID with minimal information.
     * @param aTemp The Temperature probe object to use
     * @param aName The Name of this PID
     */
    public PID(final Temp aTemp, final String aName) {
        this.fName = aName;
        this.fTemp = aTemp;
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
            return;
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
                return "";
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
    public void updateValues(final String m, final BigDecimal duty,
            final BigDecimal cycle, final BigDecimal setpoint, final BigDecimal p,
            final BigDecimal i, final BigDecimal d) {
        this.mode = m;
        if (this.mode.equals("manual")) {
            this.duty_cycle = duty;
        }
        if (cycle != null) {
            this.heatSetting.cycle_time = cycle;
        }
        if (setpoint != null) {
            this.set_point = setpoint;
        }
        BrewServer.LOG.info(heatSetting.proportional + ": "
            + heatSetting.integral + ": " + heatSetting.derivative);
        if (p != null) {
            this.heatSetting.proportional = p;
        }
        if (i != null) {
            this.heatSetting.integral = i;
        }
        if (d != null) {
            this.heatSetting.derivative = d;
        }
        BrewServer.LOG.info("Mode " + this.mode + " " + this.heatSetting.proportional + ": "
            + heatSetting.integral + ": " + this.heatSetting.derivative);
        LaunchControl.savePID(this);
    }

    /****
     * Get the Status of the current PID, heating, off, etc...
     * @return The current status of this PID.
     */
    public synchronized String getStatus() {
        if (this.outputControl != null) {
            return this.outputControl.getStatus();
        }
        // Output control is broken
        return "No output on! Duty Cyle: " + this.duty_cycle
                + " - Temp: " + getTempC();
    }

    /**
     * Set the PID to hysteria mode.
     * @param newMax   The maximum value to disable heating at
     * @param newMin   The minimum value to start heating at
     * @param newMinTime The minimum amount of time to keep the burner on for
     */
    public void setHysteria(final BigDecimal newMin,
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

    public void useHysteria() {
        this.mode = "hysteria";
    }
    /***
     * Main loop for using a PID Thread.
     */
    public void run() {
        BrewServer.LOG.info("Running " + this.fName + " PID.");
        // setup the first time
        this.previousTime = new BigDecimal(System.currentTimeMillis());
        // create the Output if needed
        if (this.heatGPIO != null && !this.heatGPIO.equals("")) {
            this.outputControl =
                    new OutputControl(fName, heatGPIO, heatSetting.cycle_time);
        }
        if (this.coolGPIO != null ) {
            if (this.outputControl == null) {
                this.outputControl = new OutputControl();
            }
            this.outputControl.setCool(coolGPIO, coolSetting.cycle_time, coolSetting.delay);
        }
        if (this.outputControl == null) {
            return;
        } else {
            this.outputThread = new Thread(outputControl);
            outputThread.start();
        }

        // Detect an Auxilliary output
        if (this.auxGPIO != null && !this.auxGPIO.equals("")) {
            try {
                this.auxPin = new OutPin(this.auxGPIO);
                setAux(false);
            } catch (InvalidGPIOException e) {
                BrewServer.LOG.log(Level.SEVERE,
                    "Couldn't parse " + this.auxGPIO + " as a valid GPIO");
                System.exit(-1);
            } catch (RuntimeException e) {
                BrewServer.LOG.log(Level.SEVERE,
                    "Couldn't setup " + auxGPIO + " as a valid GPIO");
                System.exit(-1);
            }
        }

        // Main loop
        while (running) {
            try {
                synchronized (this.fTemp) {
                    // do the bulk of the work here
                    this.fTempC = this.fTemp.getTempC();
                    this.fTempF = this.fTemp.getTempF();
                    this.currentTime = new BigDecimal(this.fTemp.getTime());

                    // if the GPIO is blank we do not need to do any of this;
                    if (this.hasValidHeater()
                            || this.hasValidCooler()) {
                        if (this.tempList.size() >= 5) {
                            tempList.remove(0);
                        }
                        tempList.add(fTemp.getTemp());
                        BigDecimal tempAvg = calcAverage();
                        // we have the current temperature
                        BrewServer.LOG.info(mode);
                        switch (mode) {
                            case "auto":
                                this.calculatedDuty =
                                        calculate(tempAvg);
                                BrewServer.LOG.info(
                                        "Calculated: " + calculatedDuty);
                                if (this.outputControl.setDuty(calculatedDuty)) {
                                    this.outputControl.getHeater().setCycleTime(
                                            heatSetting.cycle_time);
                                    this.outputThread.interrupt();
                                }
                                break;
                            case "manual":
                                if (this.outputControl.setDuty(this.manual_duty)) {
                                    this.outputControl.getHeater().setCycleTime(
                                            this.manual_time);
                                    this.outputThread.interrupt();
                                }
                                break;
                            case "off":
                                this.outputControl.setDuty(BigDecimal.ZERO);
                                this.outputControl.getHeater().setCycleTime(
                                        heatSetting.cycle_time);
                                this.outputThread.interrupt();
                                break;
                            case "hysteria":
                                setHysteria();
                                this.outputThread.interrupt();
                                break;
                        }
                        BrewServer.LOG.info(mode + ": " + fName + " status: "
                            + fTempF + " duty cycle: "
                            + this.outputControl.getDuty());
                    }
                    //notify all waiters of the change of state
                }

                //pause execution for a second
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                BrewServer.LOG.warning("PID " + getName() + " Interrupted.");
                ex.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean minTimePassed() {
        if (this.timeDiff.compareTo(this.minTime) <= 0) {
            BigDecimal remaining = this.minTime.subtract(this.timeDiff);
            if (remaining.compareTo(new BigDecimal(10.0/60.0)) >= 0) {
                this.getTemp().currentError =
                    "Waiting for minimum time before changing outputs,"
                    + " less than "
                    + remaining.setScale(0, BigDecimal.ROUND_UP)
                    + " mins remaining";
            }
            return false;
        } else {
            if (getTemp().currentError == null || getTemp().currentError.startsWith("Waiting for minimum")) {
                getTemp().currentError = "";
            }
            return true;
        }
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
    public void setAux(String gpio, boolean invert) {
        if (gpio == null || gpio.length() == 0) return;

        // Only do this is the pin has changed
        if (!detectGPIO(gpio).equalsIgnoreCase(auxGPIO)) {
            this.auxGPIO = detectGPIO(gpio);
            try {
                auxPin = new OutPin(auxGPIO);

            } catch (InvalidGPIOException i)
            {
                if (auxPin != null) {
                    auxPin.close();
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
    public void  toggleAux() {
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

    public void setAux(boolean on)
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
    public void setCoolP(final BigDecimal p) {
        if (p != null) {
            coolSetting.proportional = p;
        }
    }

    /******
     * Set the integral value.
     * @param i The new Integral.
     */
    public void setCoolI(final BigDecimal i) {
        if (i != null)
        {
            coolSetting.integral = i;
        }
    }

    /******
     * Set the differential value.
     * @param d The new differential
     */
    public void setCoolD(final BigDecimal d) {
        if (d != null)
        {
            coolSetting.derivative = d;
        }
    }

    /******
     * Set the proportional value.
     * @param p the new proportional value
     */
    public void setHeatP(final BigDecimal p) {
        if (p != null)
        {
            heatSetting.proportional = p;
        }
    }

    /******
     * Set the integral value.
     * @param i The new Integral.
     */
    public void setHeatI(final BigDecimal i) {
        if (i != null)
        {
            heatSetting.integral = i;
        }
    }

    /******
     * Set the differential value.
     * @param d The new differential
     */
    public void setHeatD(final BigDecimal d) {
        if (d != null)
        {
            heatSetting.derivative = d;
        }
    }

    /*******
     * Get the current mode.
     * @return The mode.
     */
    public String getMode() {
        return mode;
    }

    /**
     * @return Get the temperature in celsius.
     */
    public BigDecimal getTempC() {
        return fTempC;
    }

    /**
     * @return Get the temperature in fahrenheit
     */
    public BigDecimal getTempF() {
        return fTempF;
    }

    /**
     * @return Get the GPIO Pin
     */
    public String getHeatGPIO() {
        return heatGPIO;
    }

    public String getCoolGPIO() {
        return coolGPIO;
    }
    
    /**
     * @return  Get the Aux GPIO Pin
     */
    public String getAuxGPIO() {
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
    public BigDecimal getSetPoint() {
        return this.set_point;
    }

    /**
     * @return  Get the current Duty Cycle Time
     */
    public BigDecimal getHeatCycle() {
        return heatSetting.cycle_time;
    }

    /**
     * @return Get the current proportional value
     */
    public BigDecimal getHeatP() {
        return heatSetting.proportional;
    }

    /**
     * @return  Get the current Integral value
     */
    public BigDecimal getHeatI() {
        return heatSetting.integral;
    }

    /**
     * @return Get the current Differential value
     */
    public BigDecimal getHeatD() {
        return heatSetting.derivative;
    }

    public BigDecimal getHeatDelay() {
        return heatSetting.delay;
    }

    /**
     * @return true if the heating output is inverted.
     */
    public boolean getHeatInverted() {
        return heatSetting.inverted;
    }

    public void setHeatInverted(boolean inverted) {
        if (this.hasValidHeater()) {
            heatSetting.inverted = inverted;
            this.outputControl.getHeater().setInverted(inverted);
            this.outputControl.getHeater().turnOff();
        }
    }

    public void setCoolInverted(boolean inverted) {
        if (this.hasValidCooler()) {
            coolSetting.inverted = inverted;
            this.outputControl.getCooler().setInverted(inverted);
            this.outputControl.getCooler().turnOff();
        }
    }

    /**
     * @return true if the cooling output is inverted.
     */
    public boolean getCoolInverted() {
        return coolSetting.inverted;
    }
    /**
     * @return  Get the current Duty Cycle Time
     */
    public BigDecimal getCoolCycle() {
        return coolSetting.cycle_time;
    }


    /**
     * @return Get the current proportional value
     */
    public BigDecimal getCoolP() {
        return coolSetting.proportional;
    }

    /**
     * @return  Get the current Integral value
     */
    public BigDecimal getCoolI() {
        return coolSetting.integral;
    }

    /**
     * @return Get the current Differential value
     */
    public BigDecimal getCoolD() {
        return coolSetting.derivative;
    }

    public BigDecimal getCoolDelay() {
        return coolSetting.delay;
    }

    /**
     * @return Get the current Temp object
     */
    public Temp getTempProbe() {
        return fTemp;
    }

    /**
     * @return Get the name of this device
     */
    public String getName() {
        return fTemp.getName();
    }

  //PRIVATE ///
    /**
     * Store the previous timestamp for the update.
     */
    private BigDecimal previousTime = new BigDecimal(0);

    /**
     * @return Calculate the average of the current temp list
     */
    private BigDecimal calcAverage() {
        int size = tempList.size();

        if (size == 0)
        {
            return new BigDecimal(-999.0);
        }

        BigDecimal total = new BigDecimal(0.0);
        for (BigDecimal t : tempList) {
            total = total.add(t);
        }

        return MathUtil.divide(total, size);
    }

    /**
     * The current temperature Object.
     */
    private final Temp fTemp;
    /**
     * The current temperature in F and C.
     */
    private BigDecimal fTempF, fTempC;
    /**
     * The GPIO String values.
     */
    private String heatGPIO, auxGPIO, coolGPIO = null;
    /**
     * The previous five temperature readings.
     */
    private List<BigDecimal> tempList = new ArrayList<>();

    /**
     * Various strings.
     */
    private String mode = "off", fName = null;
    /**
     * The current timestamp.
     */
    private BigDecimal currentTime, hysteriaStartTime
        = new BigDecimal(System.currentTimeMillis());
    private BigDecimal timeDiff = BigDecimal.ZERO;
    /**
     * Settings for the heating and cooling.
     */
    private Settings heatSetting = new Settings();
    private Settings coolSetting = new Settings();
    /**
     * The aux output pin.
     */
    private OutPin auxPin = null;

    private BigDecimal totalError = new BigDecimal(0.0);
    private BigDecimal errorFactor = new BigDecimal(0.0);
    /**
     *  Temp values for PID calculation.
     */
    private BigDecimal previousError = new BigDecimal(0.0);
    /**
     *  Temp values for PID calculation.
     */
    private BigDecimal integralFactor = new BigDecimal(0.0);
    /**
     *  Temp values for PID calculation.
     */
    private BigDecimal derivativeFactor = new BigDecimal(0.0);

    /**
     * @return Get the current temp probe (for saving)
     */
    public Temp getTemp() {
        return fTemp;
    }

    /*****
     * Calculate the current PID Duty.
     * @param avgTemp The current average temperature
     * @return  A Double of the duty cycle %
     */
    private BigDecimal calculate(BigDecimal avgTemp) {
        this.currentTime = new BigDecimal(System.currentTimeMillis());
        if (previousTime.compareTo(BigDecimal.ZERO) == 0) {
            previousTime = currentTime;
        }
        BigDecimal dt = MathUtil.divide(
                currentTime.subtract(previousTime),THOUSAND);
        if (dt.compareTo(BigDecimal.ZERO) == 0) {
            return outputControl.getDuty();
        }

        // Calculate the error
        /*
       Temp values for PID calculation.
     */
        BigDecimal error = this.set_point.subtract(avgTemp);

        if ((this.totalError.add(error).multiply(
                this.integralFactor).compareTo(new BigDecimal(100)) < 0)
                && (this.totalError.add(error).multiply(
                        this.integralFactor).compareTo(new BigDecimal(0)) > 0))
        {
            this.totalError = this.totalError.add(error);
        }

        this.heatSetting.proportional.multiply(error).add(
                heatSetting.integral.multiply(this.totalError)).add(
                        heatSetting.derivative.multiply(
                                error.subtract(this.previousError)));

        BrewServer.LOG.info("DT: " + dt + " Error: " + errorFactor
            + " integral: " + integralFactor
            + " derivative: " + derivativeFactor);

        /*
       Temp values for PID calculation.
     */
        BigDecimal output = heatSetting.proportional.multiply(error)
                .add(heatSetting.integral.multiply(integralFactor))
                .add(heatSetting.derivative.multiply(derivativeFactor));

        previousError = error;

        if (output.compareTo(BigDecimal.ZERO) < 0
                && (this.coolGPIO == null || this.coolGPIO.equals(""))) {
            output = BigDecimal.ZERO;
        } else if (output.compareTo(BigDecimal.ZERO) > 0
                && (this.heatGPIO == null || this.heatGPIO.equals(""))) {
            output = BigDecimal.ZERO;
        }

        if (output.compareTo(new BigDecimal(100)) > 0) {
            output = new BigDecimal(100);
        } else if (output.compareTo(new BigDecimal(-100)) < 0) {
            output = new BigDecimal(-100);
        }

        this.previousTime = currentTime;
        return output;
    }

    /**
     * Used as a shutdown hook to close off everything.
     */
    public void shutdown() {
        if (outputControl != null && outputThread != null) {
            this.outputControl.shuttingDown = true;
            this.outputThread.interrupt();
            this.outputControl.shutdown();
        }

        if (auxPin != null) {
            this.auxPin.close();
        }

        if (this.getName() != null && !getName().equals("")) {
            LaunchControl.savePID(this);
        }
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
        // set the values
        int j = 0;
        // Wait for the Output to turn on.
        while (outputControl.getCooler() == null) {
            j++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }

            // Max retries
            if (j > 10) {
                return;
            }
        }
        this.outputControl.setCool(gpio, duty, delay);
    }

    /**
     * @return The current status as a map
     */
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
        heatMap.put("inverted", getHeatInverted());
        statusMap.put("heat", heatMap);

        // The cool settings
        Map<String, Object> coolMap = new HashMap<>();
        coolMap.put("cycle", getCoolCycle());
        coolMap.put("p", getCoolP());
        coolMap.put("i", getCoolI());
        coolMap.put("d", getCoolD());
        coolMap.put("gpio", getCoolGPIO());
        coolMap.put("delay", getCoolDelay());
        coolMap.put("inverted", getCoolInverted());
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
    }

    public void delHeatGPIO() {
        if (this.hasValidHeater()) {
            this.outputControl.getHeater().disable();
        }
        this.heatGPIO = "";
    }

    public void delCoolGPIO() {
        if (this.hasValidCooler()) {
            this.outputControl.getCooler().disable();
        }
        this.coolGPIO = "";
    }
    /**
     * Set the GPIO to a new pin, shutdown the old one first.
     * @param gpio The new GPIO to use
     */
    public void setHeatGPIO(final String gpio) {
        // Close down the existing OutputControl
        this.heatGPIO = this.detectGPIO(gpio);
        if (this.outputControl == null) {
            this.outputControl = new OutputControl(
                    this.getName(), gpio, this.getHeatCycle());
        }
        if (this.outputControl.getHeater() != null) {
            this.outputControl.getHeater().disable();
        }

        if (this.heatGPIO != null) {
            this.outputControl.setHeater(new OutputDevice(
                    this.getName(), heatGPIO, this.heatSetting.cycle_time));
        } else {
            this.outputControl.setHeater(null);
        }
    }

    public void setCoolGPIO(final String gpio) {
        // Close down the existing OutputControl

        this.coolGPIO = this.detectGPIO(gpio);
        if (this.outputControl == null) {
            this.outputControl = new OutputControl(this.getName(), this.heatGPIO, this.getHeatCycle());
        }
        if (this.outputControl.getCooler() != null) {
            this.outputControl.getCooler().disable();
        }
        
        if (gpio != null) {
            this.outputControl.setCool(gpio, this.coolSetting.cycle_time, this.coolSetting.delay);
        } else {
            this.outputControl.setCooler(null);
        }
    }
    
    public BigDecimal getMin() {
        return this.min;
    }

    public BigDecimal getMax() {
        return this.max;
    }

    public BigDecimal getTime() {
        return this.minTime;
    }

    public Settings getHeatSetting() {
        return this.heatSetting;
    }
    
    public Settings getCoolSetting() {
        return this.coolSetting;
    }
    
    public void stop() {
        BrewServer.LOG.warning("Shutting down " + this.getName());
        running = false;
        Thread.currentThread().interrupt();
    }

    public void setCoolDelay(BigDecimal coolDelay) {
        if (coolDelay != null) {
            this.coolSetting.delay = coolDelay;
        }
    }
    
    public void setCoolCycle(BigDecimal coolCycle) {
        if (coolCycle != null)
        {
            this.coolSetting.cycle_time = coolCycle;
        }
    }
    
    public void setHeatCycle(BigDecimal heatCycle) {
        if (heatCycle != null)
        {
            this.heatSetting.cycle_time = heatCycle;
        }
    }
    
    public void setManualDuty(BigDecimal duty) {
        if (duty != null) {
            this.manual_duty = duty;
        }
    }
    
    public void setManualTime(BigDecimal time) {
        if (time != null)
        {
            this.manual_time = time;
        }
    }
    
    private void setHysteria() {
        /**
         * New logic
         * 1) If we're below the minimum temp
         *      AND we have a heating output
         *      AND we have been on for the minimum time
         *      AND we have been off for the minimum delay
         *      THEN turn on the heating output
         * 2) If we're above the maximum temp
         *      AND we have a cooling output
         *      AND we have been on for the minimum time
         *      AND we have been off for the minimum delay
         *      THEN turn on the cooling output
         */
        // Set the duty cycle to be 100, we can wake it up when we want to
        BrewServer.LOG.info("Checking current temp against " + this.min + " and " + this.max);
        try {
            this.timeDiff = this.currentTime.subtract(this.hysteriaStartTime);
            this.timeDiff = MathUtil.divide(MathUtil.divide(timeDiff, THOUSAND), 60);
        } catch (ArithmeticException e) {
            BrewServer.LOG.warning(e.getMessage());
        }
        
        BigDecimal minTempF = this.min;
        BigDecimal maxTempF = this.max;
        if (this.getTemp().getScale().equalsIgnoreCase("C")) {
            minTempF = Temp.cToF(this.min);
            maxTempF = Temp.cToF(this.max);
        }
        
        if (this.minTimePassed() && this.getTempF().compareTo(minTempF) < 0) {

            if (this.hasValidHeater()
                    && this.duty_cycle.compareTo(new BigDecimal(100)) != 0) {
                BrewServer.LOG.info("Current temp is less than the minimum temp, turning on 100");
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                this.duty_cycle = new BigDecimal(100);
                this.outputControl.setDuty(this.duty_cycle);
                this.outputControl.getHeater().setCycleTime(this.minTime.multiply(new BigDecimal(60)));
            } else if (this.hasValidCooler()
                    && this.duty_cycle.compareTo(new BigDecimal(100)) < 0) {
                BrewServer.LOG.info("Slept for long enough, turning off");
                // Make sure the thread wakes up for the new settings
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                this.duty_cycle = new BigDecimal(0);
                this.outputControl.setDuty(this.duty_cycle);
                this.outputThread.interrupt();
            }

            // Make sure the thread wakes up for the new settings
            this.outputThread.interrupt();
        } else if (this.minTimePassed() && this.getTempF().compareTo(maxTempF) >= 0) {
            // TimeDiff is now in minutes
            // Is the cooling output on?
            if (this.hasValidCooler()
                    && this.duty_cycle.compareTo(new BigDecimal(-100)) != 0) {
                BrewServer.LOG.info("Current temp is greater than the max temp, turning on -100");
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                this.duty_cycle = new BigDecimal(-100);
                this.outputControl.setDuty(this.duty_cycle);
                this.outputControl.getCooler().setCycleTime(this.minTime.multiply(new BigDecimal(60)));
                this.outputThread.interrupt();

            } else if(this.hasValidHeater()
                    && this.duty_cycle.compareTo(new BigDecimal(-100)) > 0) {
                BrewServer.LOG.info("Current temp is more than the max temp");
                BrewServer.LOG.info("Slept for long enough, turning off");
                // Make sure the thread wakes up for the new settings
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                this.duty_cycle = BigDecimal.ZERO;
                this.outputControl.setDuty(this.duty_cycle);
                this.outputThread.interrupt();
            }
        } else if (this.minTimePassed() && this.getTempF().compareTo(minTempF) >= 0 && this.getTempF().compareTo(maxTempF) <= 0) {
            this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
            this.duty_cycle = BigDecimal.ZERO;
            this.outputControl.setDuty(this.duty_cycle);
            this.outputThread.interrupt();
        } else {
            BrewServer.LOG.info("Min: " + minTempF + " (" + getTempF() + ") " + maxTempF);
        }
    }

    /**
     * Helper to check is a cooler is enabled correctly.
     * @return True is a valid cooler is enabled
     */
    public boolean hasValidCooler() {
        return (this.outputControl != null
                && this.outputControl.getCooler() != null
                && this.outputControl.getCooler().getGpio() != null
                && !this.outputControl.getCooler().getGpio().equals(""));
    }

    /**
     * Helper to check is a valid heater is enabled.
     * @return True if a valid heater is detected.
     */
    public boolean hasValidHeater() {
        return (this.outputControl != null
                && this.outputControl.getHeater() != null
                && this.outputControl.getHeater().getGpio() != null
                && !this.outputControl.getHeater().getGpio().equals(""));
    }

    /**
     * Invert the outputs.
     * @param invert True to invert the outputs.
     */
    public void setAuxInverted(final boolean invert) {
        this.invertAux = invert;
    }

    /**
     * @return True if this device has inverted outputs.
     */
    public boolean isAuxInverted() {
        return this.invertAux;
    }

    public boolean getAuxStatus()
    {
        if (this.invertAux) {
            return auxPin.getValue().equals("0");
        }
        return auxPin.getValue().equals("1");
    }

    public BigDecimal getManualCycle() {
        return this.manual_duty;
    }
    
    public BigDecimal getManualTime() {
        return this.manual_time;
    }
}
