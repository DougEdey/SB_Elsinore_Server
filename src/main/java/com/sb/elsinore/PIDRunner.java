package com.sb.elsinore;

import com.sb.util.MathUtil;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sb.elsinore.UrlEndpoints.COOL;
import static com.sb.elsinore.UrlEndpoints.HEAT;

/**
 * This provides a runnable implementation for the PID class
 * Created by Douglas on 2016-05-15.
 */
class PIDRunner implements Runnable {

    PIDRunner(Temp p) {
        this.temp = p;
    }

    public PID getPID() {
        return (PID) this.temp;
    }

    public Temp getTemp() {
        return this.temp;
    }

    public void stop() {
        BrewServer.LOG.warning("Shutting down " + this.temp.getName());
        this.running = false;
        Thread.currentThread().interrupt();
    }

    private boolean running = true;
    /**
     * The current temperature in F and C.
     */
    private BigDecimal tempF;

    private Temp temp = null;
    /**
     * The previous five temperature readings.
     */
    private List<BigDecimal> tempList = new ArrayList<>();
    /**
     * Thousand BigDecimal multiplier.
     */
    private BigDecimal THOUSAND = new BigDecimal(1000);
    private BigDecimal previousTime;
    /**
     * The Output control thread.
     */
    private Thread outputThread = null;
    private OutputControl outputControl = null;
    /**
     * The aux output pin.
     */
    private OutPin auxPin = null;
    /**
     * The current timestamp.
     */
    private BigDecimal currentTime, hysteriaStartTime
            = new BigDecimal(System.currentTimeMillis());
    private BigDecimal timeDiff = BigDecimal.ZERO;

    private BigDecimal totalError = new BigDecimal(0.0);
    private BigDecimal errorFactor = new BigDecimal(0.0);
    /**
     * Temp values for PID calculation.
     */
    private BigDecimal previousError = new BigDecimal(0.0);
    /**
     * Temp values for PID calculation.
     */
    private BigDecimal integralFactor = new BigDecimal(0.0);
    /**
     * Temp values for PID calculation.
     */
    private BigDecimal derivativeFactor = new BigDecimal(0.0);

    /**
     * Helper to check is a cooler is enabled correctly.
     *
     * @return True is a valid cooler is enabled
     */
    private boolean hasValidCooler() {
        return (this.outputControl != null
                && this.outputControl.getCooler() != null
                && this.outputControl.getCooler().getGpio() != null
                && !this.outputControl.getCooler().getGpio().equals(""));
    }

    /**
     * Helper to check is a valid heater is enabled.
     *
     * @return True if a valid heater is detected.
     */
    private boolean hasValidHeater() {
        return (this.outputControl != null
                && this.outputControl.getHeater() != null
                && this.outputControl.getHeater().getGpio() != null
                && !this.outputControl.getHeater().getGpio().equals(""));
    }

    /***
     * Main loop for using a PID Thread.
     */
    public void run() {
        this.temp.started();
        BrewServer.LOG.info("Running " + this.temp.getName() + " PID.");
        // setup the first time
        this.previousTime = new BigDecimal(System.currentTimeMillis());


        // Main loop
        while (this.temp.isRunning()) {
            try {
                synchronized (this.temp.getTemp()) {
                    // do the bulk of the work here
                    this.tempF = this.temp.getTempF();
                    this.currentTime = new BigDecimal(this.temp.getTime());
                    if (this.temp instanceof PID) {
                        setupPID();
                        runPIDCalculations();
                    }

                }
                //pause execution for a second
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                BrewServer.LOG.warning("PID " + this.temp.getName() + " Interrupted.");
                ex.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     *
     */

    private void setupPID() {
        if (this.temp instanceof PID) {
            PID pid = (PID) this.temp;
            // create the Output if needed
            if (!isNullOrEmpty(pid.getHeatGPIO())) {
                this.outputControl =
                        new OutputControl(this.temp.getName(), pid.getSettings(HEAT));
            }
            if (!isNullOrEmpty(pid.getCoolGPIO())) {
                if (this.outputControl == null) {
                    this.outputControl = new OutputControl();
                }
                this.outputControl.setCool(pid.getSettings(COOL));
            }
            if (this.outputControl == null) {
                return;
            } else {
                this.outputThread = new Thread(this.outputControl);
                this.outputThread.start();
            }

            // Detect an Auxilliary output
            if (!isNullOrEmpty(pid.getAuxGPIO())) {
                try {
                    this.auxPin = new OutPin(pid.getAuxGPIO());
                    pid.setAux(false);
                } catch (InvalidGPIOException e) {
                    BrewServer.LOG.log(Level.SEVERE,
                            "Couldn't parse " + pid.getAuxGPIO() + " as a valid GPIO");
                    System.exit(-1);
                } catch (RuntimeException e) {
                    BrewServer.LOG.log(Level.SEVERE,
                            "Couldn't setup " + pid.getAuxGPIO() + " as a valid GPIO");
                    System.exit(-1);
                }
            }
        } else {
            deleteAuxPin();
            deleteOutput();
        }

    }

    private void deleteAuxPin() {
        if (this.auxPin != null) {
            this.auxPin.close();
            this.auxPin = null;
        }
    }

    private void deleteOutput() {
        if (this.outputControl != null) {
            this.outputControl.shutdown();
            this.outputControl = null;
            this.outputThread = null;
        }
    }

    /**
     * Runs the current PID iteration.
     */
    private void runPIDCalculations() {
        PID pid = (PID) this.temp;
        // if the GPIO is blank we do not need to do any of this;
        if (this.hasValidHeater()
                || this.hasValidCooler()) {
            if (this.tempList.size() >= 5) {
                this.tempList.remove(0);
            }
            this.tempList.add(this.temp.getTemp());
            BigDecimal tempAvg = calcAverage();
            // we have the current temperature
            BrewServer.LOG.info(pid.getMode());
            switch (pid.getMode()) {
                case "auto":
                    pid.setDuty(calculate(tempAvg));
                    BrewServer.LOG.info(
                            "Calculated: " + pid.getDuty());
                    break;
                case "manual":
                    this.outputControl.setDuty(pid.getManualCycle());
                    break;
                case "off":
                    pid.setDuty(BigDecimal.ZERO);
                    this.outputControl.setDuty(BigDecimal.ZERO);
                    break;
                case "hysteria":
                    setHysteria();
                    this.outputThread.interrupt();
                    break;
            }
            BrewServer.LOG.info(pid.getMode() + ": " + this.temp.getName() + " status: "
                    + this.tempF + " duty cycle: "
                    + this.outputControl.getDuty());
        }
    }

    /*****
     * Calculate the current PID Duty.
     * @param avgTemp The current average temperature
     * @return A Double of the duty cycle %
     */
    private BigDecimal calculate(BigDecimal avgTemp) {
        PID pid = (PID) this.temp;
        this.currentTime = new BigDecimal(System.currentTimeMillis());
        if (this.previousTime.compareTo(BigDecimal.ZERO) == 0) {
            this.previousTime = this.currentTime;
        }
        BigDecimal dt = MathUtil.divide(
                this.currentTime.subtract(this.previousTime), this.THOUSAND);
        if (dt.compareTo(BigDecimal.ZERO) == 0) {
            return this.outputControl.getDuty();
        }

        // Calculate the error
        BigDecimal error = pid.getSetPoint().subtract(avgTemp);

        if ((this.totalError.add(error).multiply(
                this.integralFactor).compareTo(new BigDecimal(100)) < 0)
                && (this.totalError.add(error).multiply(
                this.integralFactor).compareTo(new BigDecimal(0)) > 0)) {
            this.totalError = this.totalError.add(error);
        }

        PIDSettings heatSettings = pid.getSettings(HEAT);
        this.derivativeFactor = heatSettings.getProportional().multiply(error)
                .add(heatSettings.getIntegral().multiply(this.totalError))
                .add(heatSettings.getDerivative().multiply(error.subtract(this.previousError)));

        BrewServer.LOG.info("DT: " + dt + " Error: " + this.errorFactor
                + " integral: " + this.integralFactor
                + " derivative: " + this.derivativeFactor);

        BigDecimal output = heatSettings.getProportional().multiply(error)
                .add(heatSettings.getIntegral().multiply(this.integralFactor))
                .add(heatSettings.getDerivative().multiply(this.derivativeFactor));

        this.previousError = error;

        if (output.compareTo(BigDecimal.ZERO) < 0
                && (pid.getCoolGPIO() == null)) {
            output = BigDecimal.ZERO;
        } else if (output.compareTo(BigDecimal.ZERO) > 0
                && (pid.getHeatGPIO() == null)) {
            output = BigDecimal.ZERO;
        }

        if (output.compareTo(new BigDecimal(100)) > 0) {
            output = new BigDecimal(100);
        } else if (output.compareTo(new BigDecimal(-100)) < 0) {
            output = new BigDecimal(-100);
        }

        this.previousTime = this.currentTime;
        return output;
    }

    /**
     * Used as a shutdown hook to close off everything.
     */
    public void shutdown() {
        if (this.outputControl != null && this.outputThread != null) {
            this.outputControl.shuttingDown = true;
            this.outputThread.interrupt();
            this.outputControl.shutdown();
        }

        if (this.auxPin != null) {
            this.auxPin.close();
        }
    }

    /**
     * @return Calculate the average of the current temp list
     */
    private BigDecimal calcAverage() {
        int size = this.tempList.size();

        if (size == 0) {
            return new BigDecimal(-999.0);
        }

        BigDecimal total = new BigDecimal(0.0);
        for (BigDecimal t : this.tempList) {
            total = total.add(t);
        }

        return MathUtil.divide(total, size);
    }

    private void setHysteria() {
        /*
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

        try {
            this.timeDiff = this.currentTime.subtract(this.hysteriaStartTime);
            this.timeDiff = MathUtil.divide(MathUtil.divide(this.timeDiff, this.THOUSAND), 60);
        } catch (ArithmeticException e) {
            BrewServer.LOG.warning(e.getMessage());
        }
        PID pid = (PID) this.temp;
        BigDecimal minTempF = pid.getMin();
        BigDecimal maxTempF = pid.getMax();

        if (this.temp.getScale().equalsIgnoreCase("C")) {
            minTempF = Temp.cToF(minTempF);
            maxTempF = Temp.cToF(maxTempF);
        }
        BrewServer.LOG.info("Checking current temp against " + minTempF + " and " + maxTempF);
        String state = "Min: " + minTempF + " (" + this.temp.getTemp() + ") " + maxTempF;
        if (this.temp.getTempF().compareTo(minTempF) < 0) {

            if (this.hasValidHeater()
                    && pid.getDuty().compareTo(new BigDecimal(100)) != 0
                    && this.minTimePassed(state)) {
                BrewServer.LOG.info("Current temp is less than the minimum temp, turning on 100");
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                pid.setDuty(new BigDecimal(100));
            } else if (this.hasValidCooler()
                    && pid.getDuty().compareTo(new BigDecimal(100)) < 0
                    && this.minTimePassed(state)) {
                BrewServer.LOG.info("Slept for long enough, turning off");
                // Make sure the thread wakes up for the new settings
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                pid.setDuty(new BigDecimal(0));
            }
        } else if (this.temp.getTempF().compareTo(maxTempF) >= 0) {
            // TimeDiff is now in minutes
            // Is the cooling output on?
            if (this.hasValidCooler()
                    && pid.getDuty().compareTo(new BigDecimal(-100)) != 0
                    && this.minTimePassed(state)) {
                BrewServer.LOG.info("Current temp is greater than the max temp, turning on -100");
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                pid.setDuty(new BigDecimal(-100));

            } else if (this.hasValidHeater()
                    && pid.getDuty().compareTo(new BigDecimal(-100)) > 0
                    && this.minTimePassed(state)) {
                BrewServer.LOG.info("Current temp is more than the max temp");
                BrewServer.LOG.info("Slept for long enough, turning off");
                // Make sure the thread wakes up for the new settings
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                pid.setDuty(BigDecimal.ZERO);
            }
        } else if (this.temp.getTempF().compareTo(minTempF) >= 0 && this.temp.getTempF().compareTo(maxTempF) <= 0
                && pid.getDuty().compareTo(new BigDecimal(0)) != 0
                && this.minTimePassed(state)) {
            this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
            pid.setDuty(BigDecimal.ZERO);
        } else {
            BrewServer.LOG.info("Min: " + minTempF + " (" + this.temp.getTempF() + ") " + maxTempF);
        }
    }

    private boolean minTimePassed(String direction) {
        PID pid = (PID) this.temp;
        if (this.timeDiff.compareTo(pid.getMin()) <= 0) {
            BigDecimal remaining = pid.getMin().subtract(this.timeDiff);
            if (remaining.compareTo(new BigDecimal(10.0 / 60.0)) >= 0) {
                this.temp.currentError =
                        "Waiting for minimum time before changing outputs,"
                                + " less than "
                                + remaining.setScale(0, BigDecimal.ROUND_UP)
                                + " mins remaining";
                //+ " going " + direction;
            }
            return false;
        } else {
            if (this.temp.currentError == null || this.temp.currentError.startsWith("Waiting for minimum")) {
                this.temp.currentError = "";
            }
            return true;
        }
    }

}
