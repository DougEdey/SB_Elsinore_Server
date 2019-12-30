package com.sb.elsinore.wrappers;


import com.sb.common.Constants;
import com.sb.common.TemperatureResult;
import com.sb.elsinore.hardware.OneWireController;
import com.sb.elsinore.interfaces.TemperatureInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.math.BigDecimal;

import static com.sb.elsinore.wrappers.TemperatureValue.cToF;


public class TempRunner implements Runnable {

    @Autowired
    public OneWireController oneWireController;

    public static final String TYPE = "tempProbe";
    /**
     * Strings for the Nodes.
     */
    public static final String POSITION = "position";

    String currentError = null;
    TemperatureValue currentTemp = new TemperatureValue();
    TemperatureInterface temperature = null;
    boolean badTemp = false;
    boolean keepAlive = true;
    boolean isStarted = false;
    String fileProbe = null;
    boolean initialized = false;
    boolean loggingOn = true;
    private Logger logger = LoggerFactory.getLogger(TempRunner.class);
    private BigDecimal defaultTemperature = null;

    public TempRunner(TemperatureInterface temperature, OneWireController oneWireController) {
        this.oneWireController = oneWireController;
        this.temperature = temperature;
    }

    public void updateTemperatureInterface(TemperatureInterface temperatureInterface) {
        this.temperature = temperatureInterface;
    }

    @Autowired
    public void setOneWireController(OneWireController oneWireController) {
        System.out.println("FOO");
        this.oneWireController = oneWireController;
    }

    private void initialize() {
        if (Constants.SYSTEM.equalsIgnoreCase(this.temperature.getName())) {
            this.temperature.setDevice(Constants.SYSTEM);
            File tempFile = new File(Constants.rpiSystemTemp);
            if (tempFile.exists()) {
                this.fileProbe = Constants.rpiSystemTemp;
            } else {
                tempFile = new File(Constants.bbbSystemTemp);
                if (tempFile.exists()) {
                    this.fileProbe = Constants.bbbSystemTemp;
                } else {
                    this.logger.warn("Couldn't find a valid system temperature probe");
                    return;
                }
            }

        }

        this.logger.info("{} initialized.", this.temperature.getName());
        this.initialized = true;
    }

    public TemperatureInterface getTempInterface() {
        return this.temperature;
    }

    /**
     * @return The current temperature as read. -999 if it's bad.
     */
    public BigDecimal updateTemp() {
        TemperatureResult result;

        if (!this.initialized) {
            initialize();
        }

        if (this.badTemp && this.currentError != null && this.currentError.equals("")) {
            this.logger.warn("Trying to recover {}", this.temperature.getName());
        }
        if (this.temperature.getDevice() != null) {
            result = this.oneWireController.updateTempFromDevice(this.temperature.getDevice());
        } else {
            result = this.oneWireController.updateTempFromFile(new File(this.fileProbe));
            if (result.isError() && Constants.rpiSystemTemp.equals(this.fileProbe)) {
                result = this.oneWireController.updateTempFromFile(new File(Constants.bbbSystemTemp));
                if (result.isOK()) {
                    this.fileProbe = Constants.bbbSystemTemp;
                }
            }
        }

        if (result.isError()) {
            if (this.defaultTemperature != null) {
                this.currentTemp.setValue(this.defaultTemperature, TemperatureValue.Scale.C);
                return this.defaultTemperature;
            }
            this.badTemp = true;
            return result.temperature;
        }

        if (this.badTemp) {
            this.badTemp = false;
            this.logger.warn("Recovered temperature reading for {}", this.temperature.getName());
            if (this.currentError.startsWith("Could")) {
                this.currentError = "";
            }
        }

        // OWFS/One wire always uses Celsius
        this.currentTemp.setValue(result.temperature, TemperatureValue.Scale.C);

        this.currentError = null;

        if (
                this.temperature.getCutoffTemp() != null &&
                        this.currentTemp.getValue().compareTo(this.temperature.getCutoffTemp()) >= 0) {
            this.logger.error("{}: ****** CUT OFF TEMPERATURE ({}) EXCEEDED *****", this.currentTemp, this.temperature.getCutoffTemp());
            System.exit(-1);
        }
        return result.temperature;
    }

    /**
     * @return Get the current temperature
     */
    public BigDecimal getTemperature() {
        updateTemp();
        // set up the reader
        if (getScale().equals("F")) {
            return getTempF();
        }
        return getTempC();
    }

    public String getScale() {
        return this.temperature.getScale();
    }

    /**
     * @return The current temperature in fahrenheit.
     */
    public BigDecimal getTempF() {

        BigDecimal temp = this.currentTemp.getValue(TemperatureValue.Scale.F);
        if (temp == null) {
            return null;
        }
        return temp.add(this.temperature.getCalibration());
    }

    /**
     * @return The current temperature in celsius.
     */
    BigDecimal getTempC() {
        BigDecimal temp = this.currentTemp.getValue(TemperatureValue.Scale.C);
        if (temp == null) {
            return null;
        }
        return temp.add(this.temperature.getCalibration());
    }


    public BigDecimal convertF(BigDecimal temperature) {
        if (this.temperature.getScale().equals("C")) {
            return cToF(temperature);
        }

        return temperature;
    }


    public void shutdown() {
        // Graceful shutdown.
        this.keepAlive = false;
        this.logger.warn("{} is shutting down.", this.temperature.getName());
        Thread.currentThread().interrupt();
    }

    public boolean isStarted() {
        return this.isStarted;
    }

    public boolean isRunning() {
        return this.keepAlive;
    }

    public void startRunning() {
        this.isStarted = true;
    }

    public String getName() {
        return this.temperature.getName();
    }

    /***
     * Main loop for using a PIDModel Thread.
     */
    @Override
    public void run() {
        startRunning();
        this.logger.info("Running " + this.temperature.getName() + ".");
        // Main loop
        while (isRunning()) {
            try {
                getTempF();
                // do the bulk of the work here
                loopRun();

                //pause execution for a second
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                this.logger.warn("PIDModel {} interrupted.", this.temperature.getName());
                ex.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

        shutdown();
    }

    protected void loopRun() {
        // nothing extra for the default temp wrapper
    }


    public void stop() {
        this.logger.warn("Shutting down {}", this.temperature.getName());
        shutdown();
        Thread.currentThread().interrupt();
    }

    public Long getId() {
        return this.temperature.getId();
    }

    public void setDefaultTemperature(BigDecimal defaultTemperature) {
        this.defaultTemperature = defaultTemperature;
    }
}
