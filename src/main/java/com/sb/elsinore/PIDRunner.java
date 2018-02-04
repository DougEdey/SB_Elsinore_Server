package com.sb.elsinore;

import com.sb.util.MathUtil;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * This provides a runnable implementation for the PID class
 * Created by Douglas on 2016-05-15.
 */
class PIDRunner implements Runnable {

    PIDRunner(Temp p) {
        this.tempDevice = p;
        this.previousTime = new BigDecimal(System.currentTimeMillis());
    }

    public PID getPID() {
        return (PID) getTempDevice();
    }

    public Temp getTempDevice() {
        return this.tempDevice;
    }

    public void stop() {
        BrewServer.LOG.warning("Shutting down " + getTempDevice().getName());
        this.tempDevice.shutdown();
        Thread.currentThread().interrupt();
    }

    /**
     * The current temperature in F and C.
     */
    private BigDecimal tempF;

    private Temp tempDevice = null;
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
    /**
     * Temp values for PID calculation.
     */
    private BigDecimal previousError = new BigDecimal(0.0);
    private BigDecimal totalError = new BigDecimal(0.0);


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
    @Override
    public void run() {
        Temp temp = getTempDevice();
        temp.started();
        BrewServer.LOG.info("Running " + temp.getName() + " PID.");
        // setup the first time
        this.previousTime = new BigDecimal(System.currentTimeMillis());


        // Main loop
        while (temp.isRunning()) {
            try {
                synchronized (temp.getTemp()) {
                    // do the bulk of the work here
                    this.tempF = temp.getTempF();
                    this.currentTime = new BigDecimal(temp.getTime());
                    if (temp instanceof PID) {
                        setupPID();
                        runPIDCalculations();
                    }

                }

                //pause execution for a second
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                BrewServer.LOG.warning("PID " + temp.getName() + " Interrupted.");
                ex.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

        shutdown();
    }

    /**
     *
     */

    private void setupPID() {
        if (!(getTempDevice() instanceof PID)) {
            deleteAuxPin();
            deleteOutput();
            return;
        }

        PID pid = (PID) getTempDevice();
        // create the Output if needed
        if (!isNullOrEmpty(pid.getHeatGPIO()) && this.outputControl == null) {
            this.outputControl =
                    new OutputControl(pid.getName(), pid.getHeatSetting());
        }
        if (!isNullOrEmpty(pid.getCoolGPIO())) {
            if (this.outputControl == null) {
                this.outputControl = new OutputControl();
            }
            this.outputControl.setCool(pid.getCoolSetting());
        }
        if (this.outputControl == null) {
            return;
        } else if (this.outputThread == null) {
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
        PID pid = (PID) getTempDevice();
        // if the GPIO is blank we do not need to do any of this;
        if (this.hasValidHeater()
                || this.hasValidCooler()) {
            if (getTempList().size() >= 5) {
                getTempList().remove(0);
            }
            getTempList().add(pid.getTemp());
            BigDecimal tempAvg = calcAverage();
            // we have the current temperature
            BrewServer.LOG.info("Running PID Cycle: " + pid.getPidMode());
            switch (pid.getPidMode()) {
                case AUTO:
                    pid.setDutyCycle(calculate(tempAvg));
                    BrewServer.LOG.info("Calculated: " + pid.getDutyCycle());
                    break;
                case MANUAL:
                    this.outputControl.setDuty(pid.getManualDuty());
                    break;
                case OFF:
                    pid.setDutyCycle(BigDecimal.ZERO);
                    this.outputControl.setDuty(BigDecimal.ZERO);
                    break;
                case HYSTERIA:
                    setHysteria();
                    this.outputThread.interrupt();
                    break;
            }
            BrewServer.LOG.info(pid.getPidMode() + ": " + pid.getName() + " status: "
                    + this.tempF + " duty cycle: "
                    + this.outputControl.getDuty());
        }
    }

    /*****
     * Calculate the current PID Duty.
     * @param avgTemp The current average temperature
     * @return A Double of the duty cycle %
     */
    public BigDecimal calculate(BigDecimal avgTemp) {
        PID pid = (PID) getTempDevice();

        BigDecimal dt = calculateTimeDiff();
        if (dt.compareTo(BigDecimal.ZERO) == 0) {
            return this.outputControl.getDuty();
        }

        // Calculate the error
        PIDSettings heatSettings = pid.getCoolSetting();
        PIDSettings coolSettings = pid.getHeatSetting();

        BigDecimal error = pid.getSetPoint().subtract(avgTemp);
        BigDecimal output;
        if (!isNullOrEmpty(heatSettings.getGPIO())) {
            output = calculate(dt, error, heatSettings);
        } else if (!isNullOrEmpty(coolSettings.getGPIO())) {
            output = calculate(dt, error, coolSettings);
        } else {
            output = BigDecimal.ZERO;
        }

        // If we're below 0 but there's no cooling, turn off.
        if (output.compareTo(BigDecimal.ZERO) < 0
                && (pid.getCoolSetting().getGPIO() == null)) {
            output = BigDecimal.ZERO;
            // If we're above 0 but there's no heating, turn off.
        } else if (output.compareTo(BigDecimal.ZERO) > 0
                && (pid.getHeatSetting().getGPIO() == null)) {
            output = BigDecimal.ZERO;
        }

        if (output.compareTo(new BigDecimal(100)) > 0) {
            output = new BigDecimal(100);
        } else if (output.compareTo(new BigDecimal(-100)) < 0) {
            output = new BigDecimal(-100);
        }

        this.previousTime = this.currentTime;
        this.previousError = output;
        return output;
    }

    public BigDecimal calculate(BigDecimal timeDiff, BigDecimal error, PIDSettings pidSettings) {
        this.totalError = this.previousError.add(error.multiply(timeDiff));

        BigDecimal currentError = error.subtract(this.previousError).divide(timeDiff, MathContext.DECIMAL32);

        // proportional
        BigDecimal proportionalOut = pidSettings.getProportional().multiply(error);
        BigDecimal integralOut = pidSettings.getIntegral().multiply(this.totalError);
        BigDecimal derivativeOut = pidSettings.getDerivative().multiply(currentError);

        return proportionalOut.add(integralOut).add(derivativeOut);
    }

    BigDecimal calculateTimeDiff() {
        this.currentTime = new BigDecimal(System.currentTimeMillis());
        if (this.previousTime.compareTo(BigDecimal.ZERO) == 0) {
            this.previousTime = this.currentTime;
        }
        return MathUtil.divide(
                this.currentTime.subtract(this.previousTime), this.THOUSAND);
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

    private List<BigDecimal> getTempList() {
        return this.tempList;
    }

    /**
     * @return Calculate the average of the current tempDevice list
     */
    private BigDecimal calcAverage() {
        int size = getTempList().size();

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
         * 1) If we're below the minimum tempDevice
         *      AND we have a heating output
         *      AND we have been on for the minimum time
         *      AND we have been off for the minimum delay
         *      THEN turn on the heating output
         * 2) If we're above the maximum tempDevice
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
        PID pid = (PID) getTempDevice();
        BigDecimal minTempF = pid.getMinTemp();
        BigDecimal maxTempF = pid.getMaxTemp();

        if (pid.getScale().equalsIgnoreCase("C")) {
            minTempF = Temp.cToF(minTempF);
            maxTempF = Temp.cToF(maxTempF);
        }
        BrewServer.LOG.info("Checking current tempDevice against " + minTempF + " and " + maxTempF);
        String state = "Min: " + minTempF + " (" + pid.getTemp() + ") " + maxTempF;
        if (pid.getTempF().compareTo(minTempF) < 0) {

            if (this.hasValidHeater()
                    && pid.getDutyCycle().compareTo(new BigDecimal(100)) != 0
                    && this.minTimePassed(state)) {
                BrewServer.LOG.info("Current tempDevice is less than the minimum tempDevice, turning on 100");
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                pid.setDutyCycle(new BigDecimal(100));
            } else if (this.hasValidCooler()
                    && pid.getDutyCycle().compareTo(new BigDecimal(100)) < 0
                    && this.minTimePassed(state)) {
                BrewServer.LOG.info("Slept for long enough, turning off");
                // Make sure the thread wakes up for the new settings
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                pid.setDutyCycle(new BigDecimal(0));
            }
        } else if (pid.getTempF().compareTo(maxTempF) >= 0) {
            // TimeDiff is now in minutes
            // Is the cooling output on?
            if (this.hasValidCooler()
                    && pid.getDutyCycle().compareTo(new BigDecimal(-100)) != 0
                    && this.minTimePassed(state)) {
                BrewServer.LOG.info("Current tempDevice is greater than the max tempDevice, turning on -100");
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                pid.setDutyCycle(new BigDecimal(-100));

            } else if (this.hasValidHeater()
                    && pid.getDutyCycle().compareTo(new BigDecimal(-100)) > 0
                    && this.minTimePassed(state)) {
                BrewServer.LOG.info("Current tempDevice is more than the max tempDevice");
                BrewServer.LOG.info("Slept for long enough, turning off");
                // Make sure the thread wakes up for the new settings
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                pid.setDutyCycle(BigDecimal.ZERO);
            }
        } else if (pid.getTempF().compareTo(minTempF) >= 0 && pid.getTempF().compareTo(maxTempF) <= 0
                && pid.getDutyCycle().compareTo(new BigDecimal(0)) != 0
                && this.minTimePassed(state)) {
            this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
            pid.setDutyCycle(BigDecimal.ZERO);
        } else {
            BrewServer.LOG.info("Min: " + minTempF + " (" + pid.getTempF() + ") " + maxTempF);
        }
    }

    private boolean minTimePassed(String direction) {
        PID pid = (PID) getTempDevice();
        if (this.timeDiff.compareTo(pid.getMinTemp()) <= 0) {
            BigDecimal remaining = pid.getMinTemp().subtract(this.timeDiff);
            if (remaining.compareTo(new BigDecimal(10.0 / 60.0)) >= 0) {
                pid.currentError =
                        "Waiting for minimum time before changing outputs,"
                                + " less than "
                                + remaining.setScale(0, BigDecimal.ROUND_UP)
                                + " mins remaining";
                //+ " going " + direction;
            }
            return false;
        } else {
            if (pid.currentError == null || pid.currentError.startsWith("Waiting for minimum")) {
                pid.currentError = "";
            }
            return true;
        }
    }

}
