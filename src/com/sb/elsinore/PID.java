package com.sb.elsinore;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

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
     * The output control that's used.
     */
    private OutputControl outputControl = null;
    /**
     * The Output control thread.
     */
    private Thread outputThread = null;

    /**
     * Inner class to hold the current settings.
     * @author Doug Edey
     *
     */
    public class Settings {
        /**
         * values to hold the settings.
         */
        public double duty_cycle, cycle_time, proportional,
            integral, derivative, set_point, calculatedDuty;

        /**
         * Default constructor.
         */
        public Settings() {
            duty_cycle = cycle_time = proportional =
                    integral = derivative = set_point = 0.0;
        }
    }

    /******
     * Create a new PID.
     * @param aTemp Temperature Object to use
     * @param aName Name of the PID
     * @param aDuty Duty Cycle % being set
     * @param aTime Cycle Time in seconds
     * @param p Proportional value
     * @param i Integral Value
     * @param d Differential value
     * @param gpio GPIO to be used to control the output
     */
    public PID(final Temp aTemp, final String aName, final double aDuty,
            final double aTime, final double p, final double i,
            final double d, final String gpio) {
        this.mode = "off";
        this.heatSetting = new Settings();
        this.heatSetting.set_point = 175;
        this.heatSetting.duty_cycle = aDuty;
        this.heatSetting.cycle_time = aTime;
        this.heatSetting.proportional = p;
        this.heatSetting.integral = i;
        this.heatSetting.derivative = d;
        this.heatSetting.calculatedDuty = 0.0;

        this.fName = aName;
        this.fTemp = aTemp;

        this.fGPIO = detectGPIO(gpio);

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
    public void updateValues(final String m, final double duty,
            final double cycle, final double setpoint, final double p,
            final double i, final double d) {
        this.mode = m;
        if (this.mode.equals("manual")) {
            this.heatSetting.duty_cycle = duty;
        }
        this.heatSetting.cycle_time = cycle;
        this.heatSetting.set_point = setpoint;
        BrewServer.LOG.info(heatSetting.proportional + ": "
            + heatSetting.integral + ": " + heatSetting.derivative);
        this.heatSetting.proportional = p;
        this.heatSetting.integral = i;
        this.heatSetting.derivative = d;
        BrewServer.LOG.info(this.heatSetting.proportional + ": "
            + heatSetting.integral + ": " + this.heatSetting.derivative);
        LaunchControl.savePID(this.fName, this.heatSetting,
            this.fGPIO, this.auxGPIO);
        return;
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
        return "No output on! Duty Cyle: " + this.heatSetting.duty_cycle
                + " - Temp: " + getTempC();
    }


    /***
     * Main loop for using a PID Thread.
     */
    public void run() {
        BrewServer.LOG.info("Running " + this.fName + " PID.");
        // setup the first time
        this.previousTime = System.currentTimeMillis();
        // create the Output if needed
        if (!this.fGPIO.equals("")) {
            this.outputControl =
                new OutputControl(fName, fGPIO, heatSetting.cycle_time);
            this.outputThread = new Thread(this.outputControl);
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
        while (true) {
            try {
                synchronized (this.fTemp) {
                    // do the bulk of the work here
                    this.fTempC = this.fTemp.getTempC();
                    this.currentTime = this.fTemp.getTime();
                    this.fTempF = this.fTemp.getTempF();
                    // if the GPIO is blank we do not need to do any of this;
                    if (this.fGPIO != "") {
                        if (this.tempList.size() >= 5) {
                            tempList.remove(0);
                        }

                        tempList.add(fTemp.getTemp());

                        double tempAvg = calcAverage();


                        // we have the current temperature
                        if (mode.equals("auto")) {
                            this.heatSetting.calculatedDuty =
                                calcPIDreg4(tempAvg, true);
                            BrewServer.LOG.info("Calculated: "
                                + heatSetting.calculatedDuty);
                            this.outputControl.setDuty(
                                heatSetting.calculatedDuty);
                        } else if (mode.equals("manual")) {
                            this.outputControl.setDuty(heatSetting.duty_cycle);
                        } else if (mode.equals("off")) {
                            this.outputControl.setDuty(0);
                        }
                        // determine if the heat needs to be on or off
                        this.outputControl.setHTime(heatSetting.cycle_time);

                        BrewServer.LOG.info(mode + ": " + fName + " status: "
                            + fTempF + " duty cycle: "
                            + this.outputControl.getDuty());
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
    public void setDuty(double duty) {
        if (duty > 100) {
            duty = 100;
        } else if (duty < -100) {
            duty = -100;
        }

        heatSetting.duty_cycle = duty;
    }

    /****
     * Set the target temperature for the auto mode.
     * @param temp The new temperature in F.
     */
    public void setTemp(double temp) {
        if (temp < 0) {
            temp = 0;
        }
        heatSetting.set_point = temp;
    }

    /*******
     * set an auxilliary manual GPIO (for dual element systems).
     * @param gpio The GPIO to use as an aux
     */
    public void setAux(final String gpio) {
        this.auxGPIO = detectGPIO(gpio);

        if (this.auxGPIO == null || auxGPIO.equals("")) {
            BrewServer.LOG.log(Level.WARNING,
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
            auxPin.setValue(!auxPin.getValue().equals("1"));
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
    public void setP(final double p) {
        heatSetting.proportional = p;
    }

    /******
     * Set the integral value.
     * @param i The new Integral.
     */
    public void setI(final double i) {
        heatSetting.integral = i;
    }

    /******
     * Set the differential value.
     * @param d The new differential
     */
    public void setD(final double d) {
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
    public double getTempC() {
        return fTempC;
    }

    /**
     * @return Get the temperature in fahrenheit
     */
    public double getTempF() {
        return fTempC;
    }

    /**
     * @return Get the GPIO Pin
     */
    public String getGPIO() {
        return fGPIO;
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
    public double getDuty() {
        return heatSetting.duty_cycle;
    }

    /**
     * @return  Get the current Duty Cycle Time
     */
    public double getCycle() {
        return heatSetting.cycle_time;
    }

    /**
     * @return Get the PID Target temperature
     */
    public double getSetPoint() {
        return heatSetting.set_point;
    }

    /**
     * @return Get the current proportional value
     */
    public double getP() {
        return heatSetting.proportional;
    }

    /**
     * @return  Get the current Integral value
     */
    public double getI() {
        return heatSetting.integral;
    }

    /**
     * @return Get the current Differential value
     */
    public double getD() {
        return heatSetting.derivative;
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
    private long previousTime = 0;

    /**
     * @return Calculate the average of the current temp list
     */
    private double calcAverage() {
        int size = tempList.size();

        double total = 0.0;
        for (double t : tempList) {
            total += t;
        }
        return total / size;
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
    private double fTempF, fTempC;
    /**
     * The GPIO String values.
     */
    private String fGPIO, auxGPIO = null;
    /**
     * The previous five temperature readings.
     */
    private List<Double> tempList = new ArrayList<Double>();

    /**
     * Various strings.
     */
    private String mode, fName;
    /**
     * The current timestamp.
     */
    private long currentTime = System.currentTimeMillis();
    /**
     * Settings for the heating and cooling.
     */
    private Settings heatSetting, coldSetting;
    /**
     * The aux output pin.
     */
    private OutPin auxPin = null;

    /**
     *  Temp values for PID calculation.
     */
    private double errorFactor = 0.0;
    /**
     *  Temp values for PID calculation.
     */
    private double previousError = 0.0;
    /**
     *  Temp values for PID calculation.
     */
    private double integralFactor = 0.0;
    /**
     *  Temp values for PID calculation.
     */
    private double derivativeFactor = 0.0;
    /**
     *  Temp values for PID calculation.
     */
    private double output = 0.0;

    /**
     * Max and min limiters.
     */
    private double gmaHLIM = 100.0,
    gmaLLIM = -100.0;

    /*****
     * Calculate the current PID Duty.
     * @param avgTemp The current average temperature
     * @param enable  Enable the output
     * @return  A Double of the duty cycle %
     */
    private double calcPIDreg4(final double avgTemp, final boolean enable) {
        this.currentTime = System.currentTimeMillis();
        if (previousTime == 0.0) {
            previousTime = currentTime;
        }
        double dt = (currentTime - previousTime) / 1000;
        if (dt == 0.0 || Double.isNaN(dt)) {
            return outputControl.getDuty();
        }

        this.errorFactor = heatSetting.set_point - avgTemp;
        this.integralFactor = (integralFactor - errorFactor) * dt;
        this.derivativeFactor = (errorFactor - previousError) / dt;

        BrewServer.LOG.info("DT: " + dt + " Error: " + errorFactor
            + " integral: " + integralFactor
            + " derivative: " + derivativeFactor);

        this.output = heatSetting.proportional * errorFactor
                + heatSetting.integral * integralFactor
                + heatSetting.derivative * derivativeFactor;

        previousError = errorFactor;

        // limit y[k] to GMA_HLIM and GMA_LLIM
        if (output > gmaHLIM) {
            this.output = gmaHLIM;
        }

        if (output < gmaLLIM) {
            this.output = gmaLLIM;
        }
        this.previousTime = currentTime;
        this.previousError = errorFactor;

        return this.output;
    }

    /**
     * Used as a shutdown hook to close off everything.
     */
    public void shutdown() {
        if (outputControl != null && outputThread != null) {
            this.outputThread.interrupt();
            this.outputControl.shutdown();
        }

        if (auxPin != null) {
            this.auxPin.close();
        }

        if (this.getName() != null && !getName().equals("")) {
            LaunchControl.savePID(
                this.getName(), heatSetting, fGPIO, auxGPIO);
        }
    }

    /**
     * Set the cooling values.
     * @param gpio The GPIO to be used
     * @param duty The new duty time in seconds
     * @param delay The start/stop delay in minutes
     * @param p the proportional value
     * @param i the integral value
     * @param d the differential value
     */
    public void setCool(final String gpio, final int duty, final double delay,
            final double p, final double i, final double d) {
        // set the values
        int j = 0;
        // Wait for the Output to turn on.
        while (outputControl == null) {
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
        Map<String, Object> statusMap = new HashMap<String, Object>();
        statusMap.put("mode", getMode());
        statusMap.put("gpio", getGPIO());
        // hack to get the real duty out
        if (getMode().contains("auto")) {
            statusMap.put("actualduty", heatSetting.calculatedDuty);
        }
        statusMap.put("duty", getDuty());
        statusMap.put("cycle", getCycle());
        statusMap.put("setpoint", getSetPoint());
        statusMap.put("p", getP());
        statusMap.put("i", getI());
        statusMap.put("d", getD());
        statusMap.put("status", getStatus());

        if (auxPin != null) {
            // This value should be cached
            // but I don't trust someone to hit it with a different application
            statusMap.put("auxStatus", auxPin.getValue());
        }

        return statusMap;
    }
}
