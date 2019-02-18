package com.sb.elsinore;

import com.sb.elsinore.devices.PID;
import com.sb.elsinore.hardware.OneWireController;
import com.sb.elsinore.interfaces.PIDSettingsInterface;
import com.sb.elsinore.wrappers.TempRunner;
import com.sb.util.MathUtil;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sb.elsinore.wrappers.TemperatureValue.cToF;

/**
 * This provides a runnable implementation for the PIDModel class
 * Created by Douglas on 2016-05-15.
 */
class PIDRunner extends TempRunner {
    private PID pid;
    private Logger logger = LoggerFactory.getLogger(PIDRunner.class);
    /**
     * The previous five temperature readings.
     */
    private List<BigDecimal> tempList = new ArrayList<>();
    /**
     * Thousand BigDecimal multiplier.
     */
    private BigDecimal THOUSAND = new BigDecimal(1000);
    private BigDecimal previousTime = BigDecimal.ZERO;
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
     * TempProbe values for PIDModel calculation.
     */
    private BigDecimal previousError = new BigDecimal(0.0);
    private BigDecimal totalError = new BigDecimal(0.0);

    public PIDRunner(PID pid, OneWireController oneWireController) {
        super(pid.getTemperature(), oneWireController);
        this.pid = pid;
    }

    public PID getPID() {
        return this.pid;
    }

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

    /**
     * This does custom logic for the PIDModel
     */
    @Override
    public void loopRun() {
        setupPID();
        runPIDCalculations();
    }


    /**
     * Setup and run the PIDModel
     */
    private void setupPID() {
        // create the Output if needed
        if (!isNullOrEmpty(this.pid.getHeatGPIO()) && this.outputControl == null) {
            this.outputControl =
                    new OutputControl(getTempInterface().getName(), this.pid.getHeatSetting());
        }
        if (!isNullOrEmpty(this.pid.getCoolGPIO())) {
            if (this.outputControl == null) {
                this.outputControl = new OutputControl();
            }
            this.outputControl.setCool(this.pid.getCoolSetting());
        }
        if (this.outputControl == null) {
            return;
        } else if (this.outputThread == null) {
            this.outputThread = new Thread(this.outputControl);
            this.outputThread.start();
        }

        // Detect an Auxilliary output
        if (!isNullOrEmpty(this.pid.getAuxGPIO())) {
            try {
                this.auxPin = new OutPin(this.pid.getAuxGPIO());
                this.pid.setAux(false);
            } catch (InvalidGPIOException e) {
                this.logger.error("Couldn't parse {} as a valid GPIO", this.pid.getAuxGPIO());
                System.exit(-1);
            } catch (RuntimeException e) {
                this.logger.error("Couldn't setup {} as a valid GPIO", this.pid.getAuxGPIO());
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
     * Runs the current PIDModel iteration.
     */
    private void runPIDCalculations() {
        // if the GPIO is blank we do not need to do any of this;
        if (this.hasValidHeater()
                || this.hasValidCooler()) {
            if (getTempList().size() >= 5) {
                getTempList().remove(0);
            }
            getTempList().add(getTemperature());
            BigDecimal tempAvg = calcAverage();
            // we have the current temperature
            this.logger.info("Running PIDModel Cycle: {}", this.pid.getPidMode());
            switch (this.pid.getPidMode()) {
                case AUTO:
                    this.pid.setDutyCycle(calculate(tempAvg));
                    this.logger.info("Calculated: {}", this.pid.getDutyCycle());
                    break;
                case MANUAL:
                    this.outputControl.setDuty(this.pid.getManualDuty());
                    break;
                case OFF:
                    this.pid.setDutyCycle(BigDecimal.ZERO);
                    this.outputControl.setDuty(BigDecimal.ZERO);
                    break;
                case HYSTERIA:
                    setHysteria();
                    this.outputThread.interrupt();
                    break;
            }
            this.logger.info("{}, mode: {},  status: {}, duty cycle: {}",
                    getName(), this.pid.getPidMode(), getTempF(), this.outputControl.getDuty());
        }
    }

    /*****
     * Calculate the current PIDModel Duty.
     * @param avgTemp The current average temperature
     * @return A Double of the duty cycle %
     */
    public BigDecimal calculate(BigDecimal avgTemp) {
        BigDecimal dt = calculateTimeDiff();
        if (dt.compareTo(BigDecimal.ZERO) == 0) {
            return this.outputControl.getDuty();
        }

        // Calculate the error
        PIDSettingsInterface heatSettings = this.pid.getCoolSetting();
        PIDSettingsInterface coolSettings = this.pid.getHeatSetting();

        BigDecimal error = this.pid.getSetPoint().subtract(avgTemp);
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
                && (this.pid.getCoolSetting().getGPIO() == null)) {
            output = BigDecimal.ZERO;
            // If we're above 0 but there's no heating, turn off.
        } else if (output.compareTo(BigDecimal.ZERO) > 0
                && (this.pid.getHeatSetting().getGPIO() == null)) {
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

    public BigDecimal calculate(BigDecimal timeDiff, BigDecimal error, PIDSettingsInterface pidSettings) {
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
    @Override
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
     * @return Calculate the average of the current tempProbeDevice list
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
         * 1) If we're below the minimum tempProbeDevice
         *      AND we have a heating output
         *      AND we have been on for the minimum time
         *      AND we have been off for the minimum delay
         *      THEN turn on the heating output
         * 2) If we're above the maximum tempProbeDevice
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
            this.logger.warn("Couldn't calculate time difference", e);
        }

        BigDecimal minTempF = this.pid.getMinTemp();
        BigDecimal maxTempF = this.pid.getMaxTemp();

        if (getScale().equalsIgnoreCase("C")) {
            minTempF = cToF(minTempF);
            maxTempF = cToF(maxTempF);
        }

        this.logger.info("Checking current tempProbeDevice against {} and {}", minTempF, maxTempF);
        String state = "Min: " + minTempF + " (" + getTemperature() + ") " + maxTempF;
        if (getTempF().compareTo(minTempF) < 0) {

            if (this.hasValidHeater()
                    && this.pid.getDutyCycle().compareTo(new BigDecimal(100)) != 0
                    && this.minTimePassed(state)) {
                this.logger.info("Current tempProbeDevice is less than the minimum tempProbeDevice, turning on 100");
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                this.pid.setDutyCycle(new BigDecimal(100));
            } else if (this.hasValidCooler()
                    && this.pid.getDutyCycle().compareTo(new BigDecimal(100)) < 0
                    && this.minTimePassed(state)) {
                this.logger.info("Slept for long enough, turning off");
                // Make sure the thread wakes up for the new settings
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                this.pid.setDutyCycle(new BigDecimal(0));
            }
        } else if (getTempF().compareTo(maxTempF) >= 0) {
            // TimeDiff is now in minutes
            // Is the cooling output on?
            if (this.hasValidCooler()
                    && this.pid.getDutyCycle().compareTo(new BigDecimal(-100)) != 0
                    && this.minTimePassed(state)) {
                this.logger.info("Current tempProbeDevice is greater than the max tempProbeDevice, turning on -100");
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                this.pid.setDutyCycle(new BigDecimal(-100));

            } else if (this.hasValidHeater()
                    && this.pid.getDutyCycle().compareTo(new BigDecimal(-100)) > 0
                    && this.minTimePassed(state)) {
                this.logger.info("Current tempProbeDevice is more than the max tempProbeDevice");
                this.logger.info("Slept for long enough, turning off");
                // Make sure the thread wakes up for the new settings
                this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
                this.pid.setDutyCycle(BigDecimal.ZERO);
            }
        } else if (getTempF().compareTo(minTempF) >= 0 && getTempF().compareTo(maxTempF) <= 0
                && this.pid.getDutyCycle().compareTo(new BigDecimal(0)) != 0
                && this.minTimePassed(state)) {
            this.hysteriaStartTime = new BigDecimal(System.currentTimeMillis());
            this.pid.setDutyCycle(BigDecimal.ZERO);
        } else {
            this.logger.info("Min: {}, current: {}, max: {}", minTempF, getTempF(), maxTempF);
        }
    }

    private boolean minTimePassed(String direction) {

        if (this.timeDiff.compareTo(this.pid.getMinTemp()) <= 0) {
            BigDecimal remaining = this.pid.getMinTemp().subtract(this.timeDiff);
            if (remaining.compareTo(new BigDecimal(10.0 / 60.0)) >= 0) {
//                setCurrentError (
//                        "Waiting for minimum time before changing outputs,"
//                                + " less than "
//                                + remaining.setScale(0, RoundingMode.HALF_UP)
//                                + " mins remaining");
                //+ " going " + direction;
            }
            return false;
        } else {
//            if (getTempProbe().currentError == null || getTempProbe().currentError.startsWith("Waiting for minimum")) {
//                getTempProbe().currentError = "";
//            }
            return true;
        }
    }

}
