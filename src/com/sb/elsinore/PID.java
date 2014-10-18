package com.sb.elsinore;
import com.sb.util.MathUtil;

import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
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

    /**
     * Thousand BigDecimal multiplier.
     */
    private BigDecimal THOUSAND = new BigDecimal(1000);

    /**
     * The Output control thread.
     */
    private Thread outputThread = null;
    private boolean invertOutput = false;
    private BigDecimal duty_cycle = new BigDecimal(0);
    private BigDecimal calculatedDuty = new BigDecimal(0);
    private BigDecimal set_point = new BigDecimal(0);
    
    /* Hysteria Settings */
    private BigDecimal max = new BigDecimal(0);
    private BigDecimal min = new BigDecimal(0);
    private BigDecimal minTime = new BigDecimal(0);

    private boolean running = true;
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

        /**
         * Default constructor.
         */
        public Settings() {
            cycle_time = proportional =
                    integral = derivative = new BigDecimal(0.0);
        }
        
        public OutputControl outputControl = null;
    }

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
     * Determine if the GPIO is valid and return it if it is.
     * @param gpio The GPIO String to check.
     * @return The GPIO pin if it's valid, or blank if it's not.
     */
    private String detectGPIO(final String gpio) {
        // Determine what kind of GPIO Mapping we have
        Pattern pinPattern = Pattern.compile("(GPIO)([0-9])_([0-9]*)");
        Pattern pinPatternAlt = Pattern.compile("(GPIO)?([0-9]*)");

        Matcher pinMatcher = pinPattern.matcher(gpio);

        BrewServer.LOG.info("Matches: " + pinMatcher.groupCount());

        if (pinMatcher.groupCount() > 0) {
            // Beagleboard style input
            BrewServer.LOG.info("Matched GPIO pinout for Beagleboard: "
                    + gpio + ". OS: " + System.getProperty("os.level"));
            return gpio;
        } else {
            pinMatcher = pinPatternAlt.matcher(gpio);
            if (pinMatcher.groupCount() > 0) {
                BrewServer.LOG.info("Direct GPIO Pinout detected. OS: "
                        + System.getProperty("os.level"));
                // The last group gives us the GPIO number
                return pinMatcher.group(pinMatcher.groupCount());
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
        this.heatSetting.cycle_time = cycle;
        this.set_point = setpoint;
        BrewServer.LOG.info(heatSetting.proportional + ": "
            + heatSetting.integral + ": " + heatSetting.derivative);
        this.heatSetting.proportional = p;
        this.heatSetting.integral = i;
        this.heatSetting.derivative = d;
        BrewServer.LOG.info(this.heatSetting.proportional + ": "
            + heatSetting.integral + ": " + this.heatSetting.derivative);
        LaunchControl.savePID(this);
        return;
    }

    /****
     * Get the Status of the current PID, heating, off, etc...
     * @return The current status of this PID.
     */
    public synchronized String getStatus() {
        if (this.heatSetting.outputControl != null) {
            return this.heatSetting.outputControl.getStatus();
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
        if (!this.heatGPIO.equals("")) {
            this.heatSetting.outputControl =
                new OutputControl(fName, heatGPIO, heatSetting.cycle_time);
            this.outputThread = new Thread(this.heatSetting.outputControl);
            this.outputThread.start();
        } else {
            return;
        }

        // Detect an Auxilliary output
        if (this.auxGPIO != null && !this.auxGPIO.equals("")) {
            try {
                this.auxPin = new OutPin(this.auxGPIO);
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
                    if (this.heatGPIO != "") {
                        if (this.tempList.size() >= 5) {
                            tempList.remove(0);
                        }

                        tempList.add(fTemp.getTemp());

                        BigDecimal tempAvg = calcAverage();

                        // we have the current temperature
                        if (mode.equals("auto")) {
                            this.calculatedDuty =
                                calculate(tempAvg, true);
                            BrewServer.LOG.info("Calculated: " + calculatedDuty);
                            this.heatSetting.outputControl.setDuty(calculatedDuty);
                            this.heatSetting.outputControl.getHeater().setCycleTime(heatSetting.cycle_time);
                        } else if (mode.equals("manual")) {
                            this.heatSetting.outputControl.setDuty(duty_cycle);
                        } else if (mode.equals("off")) {
                            this.heatSetting.outputControl.setDuty(BigDecimal.ZERO);
                            this.heatSetting.outputControl.getHeater().setCycleTime(heatSetting.cycle_time);
                        } else if (mode.equals("hysteria")) {
                            // Set the duty cycle to be 100, we can wake it up when we want to
                            BrewServer.LOG.info("Checking current temp against " + this.min + " and " + this.max);
                            if (this.getTempF().compareTo(this.min) < 0 
                                    && this.heatSetting.outputControl.getDuty().compareTo(new BigDecimal(100)) < 0) {
                                BrewServer.LOG.info("Current temp is less than the minimum temp, turning on 100");
                                this.hysteriaStartTime = this.currentTime;
                                this.duty_cycle = new BigDecimal(100);
                                this.heatSetting.outputControl.setDuty(this.duty_cycle);
                                this.heatSetting.outputControl.getHeater().setCycleTime(this.minTime.multiply(new BigDecimal(60)));
                                
                                // Make sure the thread wakes up for the new settings
                                this.outputThread.interrupt();
                                
                            } else if (this.getTempF().compareTo(this.max) >= 0
                                    && this.heatSetting.outputControl.getDuty().compareTo(BigDecimal.ZERO) != 0) {
                                BrewServer.LOG.info("Current temp is more than the max temp");
                                // We're over the maximum temp, but should we wake up the thread?
                                BigDecimal timeDiff = this.currentTime.subtract(this.hysteriaStartTime);
                                timeDiff = timeDiff.divide(THOUSAND).divide(new BigDecimal(60));
                                // TimeDiff is now in minutes
                                
                                if (timeDiff.compareTo(this.minTime) >= 0) {
                                    BrewServer.LOG.info("Slep for long enough, turning off");
                                    // Make sure the thread wakes up for the new settings    

                                    this.duty_cycle = BigDecimal.ZERO;
                                    this.heatSetting.outputControl.setDuty(this.duty_cycle);
                                    this.outputThread.interrupt();
                                }
                            }
                        
                            
                        }
                   
                        BrewServer.LOG.info(mode + ": " + fName + " status: "
                            + fTempF + " duty cycle: "
                            + this.heatSetting.outputControl.getDuty());
                    }
                    //notify all waiters of the change of state
                }

                //pause execution for a second
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                System.err.println(ex);
                Thread.currentThread().interrupt();
            }
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
        this.set_point = temp;
    }

    /*******
     * set an auxilliary manual GPIO (for dual element systems).
     * @param gpio The GPIO to use as an aux
     */
    public void setAux(final String gpio) {
        this.auxGPIO = detectGPIO(gpio);

        if (this.auxGPIO == null || auxGPIO.equals("")) {
            BrewServer.LOG.log(Level.INFO,
                "Could not detect GPIO as valid: " + gpio);
        }
    }

    /**
     * Toggle the aux pin from it's current state.
     */
    public void toggleAux() {
        // Flip the aux pin value
        if (auxPin != null) {
            // If the value if "1" we set it to false
            // If the value is not "1" we set it to true
            BrewServer.LOG.info("Aux Pin is being set to: "
                    + !auxPin.getValue().equals("1"));

            if (this.invertOutput) {
                auxPin.setValue(!auxPin.getValue().equals("0"));
            } else {
                auxPin.setValue(!auxPin.getValue().equals("1"));
            }
        } else {
            BrewServer.LOG.info("Aux Pin is not set for " + this.fName);
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
        coolSetting.proportional = p;
    }

    /******
     * Set the integral value.
     * @param i The new Integral.
     */
    public void setCoolI(final BigDecimal i) {
        coolSetting.integral = i;
    }

    /******
     * Set the differential value.
     * @param d The new differential
     */
    public void setCoolD(final BigDecimal d) {
        coolSetting.derivative = d;
    }
    
    /******
     * Set the proportional value.
     * @param p the new proportional value
     */
    public void setHeatP(final BigDecimal p) {
        heatSetting.proportional = p;
    }

    /******
     * Set the integral value.
     * @param i The new Integral.
     */
    public void setHeatI(final BigDecimal i) {
        heatSetting.integral = i;
    }

    /******
     * Set the differential value.
     * @param d The new differential
     */
    public void setHeatD(final BigDecimal d) {
        heatSetting.derivative = d;
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
        return fTempC;
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

        if ( size == 0 )
        {
            return new BigDecimal(-999.0);
        }
        
        BigDecimal total = new BigDecimal(0.0);
        for (BigDecimal t : tempList) {
            total = total.add(t);
        }
        
        return MathUtil.divide(total,size);
    }

    /**
     * the current status.
     */
    private boolean fStatus = false;
    /**
     * The current temperature Object.
     */
    private Temp fTemp;
    /**
     * The current temperature in F and C.
     */
    private BigDecimal fTempF, fTempC;
    /**
     * The GPIO String values.
     */
    private String heatGPIO, auxGPIO = null;
    private String coolGPIO = null;
    /**
     * The previous five temperature readings.
     */
    private List<BigDecimal> tempList = new ArrayList<BigDecimal>();

    /**
     * Various strings.
     */
    private String mode, fName;
    /**
     * The current timestamp.
     */
    private BigDecimal currentTime = new BigDecimal(System.currentTimeMillis());
    private BigDecimal hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
    /**
     * Settings for the heating and cooling.
     */
    private Settings heatSetting = new Settings();
    private Settings coolSetting = new Settings();
    /**
     * The aux output pin.
     */
    private OutPin auxPin = null;

    /**
     *  Temp values for PID calculation.
     */
    private BigDecimal error = new BigDecimal(0.0);
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
     *  Temp values for PID calculation.
     */
    private BigDecimal output = new BigDecimal(0.0);

    /**
     * Max and min limiters.
     */
    private BigDecimal gmaHLIM = new BigDecimal(100.0),
    gmaLLIM = new BigDecimal(-100.0);

    /**
     * @return Get the current temp probe (for saving)
     */
    public Temp getTemp() {
        return fTemp;
    }
    
    /*****
     * Calculate the current PID Duty.
     * @param avgTemp The current average temperature
     * @param enable  Enable the output
     * @return  A Double of the duty cycle %
     */
    private BigDecimal calculate(final BigDecimal avgTemp,
            final boolean enable) {
        this.currentTime = new BigDecimal(System.currentTimeMillis());
        if (previousTime.compareTo(BigDecimal.ZERO) == 0) {
            previousTime = currentTime;
        }
        BigDecimal dt = MathUtil.divide(
                currentTime.subtract(previousTime),THOUSAND);
        if (dt.compareTo(BigDecimal.ZERO) == 0) {
            return heatSetting.outputControl.getDuty();
        }

        // Calculate the error
        this.error = this.set_point.subtract(avgTemp);

        if ((this.totalError.add(this.error).multiply(
                this.integralFactor).compareTo(new BigDecimal(100)) < 0)
                && (this.totalError.add(this.error).multiply(
                        this.integralFactor).compareTo(new BigDecimal(0)) > 0))
        {
            this.totalError = this.totalError.add(this.error);
        }

        this.heatSetting.proportional.multiply(this.error).add(
                heatSetting.integral.multiply(this.totalError)).add(
                        heatSetting.derivative.multiply(
                                this.error.subtract(this.previousError)));

        BrewServer.LOG.info("DT: " + dt + " Error: " + errorFactor
            + " integral: " + integralFactor
            + " derivative: " + derivativeFactor);

        this.output = heatSetting.proportional.multiply(this.error)
                .add(heatSetting.integral.multiply(integralFactor))
                .add(heatSetting.derivative.multiply(derivativeFactor));

        previousError = error;

        if (output.compareTo(new BigDecimal(100)) > 0) {
            this.output = new BigDecimal(100);
        }

        if (output.compareTo(new BigDecimal(-100)) < 0) {
            this.output = new BigDecimal(-100);
        }

        this.previousTime = currentTime;
        return this.output;
    }

    /**
     * Used as a shutdown hook to close off everything.
     */
    public void shutdown() {
        if (heatSetting.outputControl != null && outputThread != null) {
            this.heatSetting.outputControl.shuttingDown = true;
            this.outputThread.interrupt();
            this.heatSetting.outputControl.shutdown();
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
    public void setCool(final String gpio, final BigDecimal duty,
            final BigDecimal delay, final BigDecimal cycle, final BigDecimal p,
            final BigDecimal i, final BigDecimal d) {
        // set the values
        int j = 0;
        // Wait for the Output to turn on.
        while (coolSetting.outputControl == null) {
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
        this.heatSetting.outputControl.setCool(gpio, duty, delay);
    }

    /**
     * @return The current status as a map
     */
    public Map<String, Object> getMapStatus() {
        Map<String, Object> statusMap = new HashMap<String, Object>();
        statusMap.put("mode", getMode());
        // hack to get the real duty out
        if (getMode().contains("auto")) {
            statusMap.put("actualduty", calculatedDuty);
        }

        // The Heat settings
        Map<String, Object> heatMap = new HashMap<String, Object>();
        heatMap.put("cycle", getHeatCycle());
        heatMap.put("p", getHeatP());
        heatMap.put("i", getHeatI());
        heatMap.put("d", getHeatD());
        heatMap.put("gpio", getHeatGPIO());
        statusMap.put("heat", heatMap);

        // The cool settings
        Map<String, Object> coolMap = new HashMap<String, Object>();
        coolMap.put("cycle", getCoolCycle());
        coolMap.put("p", getCoolP());
        coolMap.put("i", getCoolI());
        coolMap.put("d", getCoolD());
        coolMap.put("gpio", getCoolGPIO());
        coolMap.put("delay", getCoolDelay());
        statusMap.put("cool", coolMap);

        statusMap.put("duty", getDuty());
        statusMap.put("setpoint", getSetPoint());
        statusMap.put("min", this.min);
        statusMap.put("max", this.max);
        statusMap.put("time", this.minTime);

        statusMap.put("status", getStatus());

        if (auxPin != null) {
            // This value should be cached
            // but I don't trust someone to hit it with a different application
            statusMap.put("auxStatus", auxPin.getValue());
        }

        return statusMap;
    }

    /**
     * Set the GPIO to a new pin, shutdown the old one first.
     * @param gpio The new GPIO to use
     */
    public void setHeatGPIO(final String gpio) {
        // Close down the existing OutputControl
        if (this.heatSetting.outputControl != null) {
            this.heatSetting.outputControl.shutdown();
        }

        this.heatGPIO = gpio;
        this.heatSetting.outputControl = new OutputControl(
            this.fName, this.heatGPIO, this.heatSetting.cycle_time);
    }

    public void setCoolGPIO(final String gpio) {
        // Close down the existing OutputControl
        if (this.coolSetting.outputControl != null) {
            this.coolSetting.outputControl.shutdown();
        }

        this.coolGPIO = gpio;
        this.coolSetting.outputControl = new OutputControl(
            this.fName, this.coolGPIO, this.coolSetting.cycle_time);
    }
    
    public BigDecimal getMin() {
        return this.min;
    }

    public BigDecimal getMax() {
        return this.min;
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
        System.out.println("Shutting down " + this.getName());
        running = false;
        Thread.currentThread().interrupt();
    }

    public void setCoolDelay(BigDecimal coolDelay) {
        this.coolSetting.delay = coolDelay;
    }
    
    public void setCoolCycle(BigDecimal coolCycle) {
        this.coolSetting.cycle_time= coolCycle;
    }
}
