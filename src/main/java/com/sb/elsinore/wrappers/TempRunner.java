package com.sb.elsinore.wrappers;


import com.sb.elsinore.BrewServer;
import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.devices.TempProbe;
import com.sb.util.MathUtil;
import org.apache.commons.lang3.StringUtils;
import org.owfs.jowfsclient.OwfsException;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sb.elsinore.wrappers.TemperatureValue.cToF;


public class TempRunner implements Runnable {

    public static final String TYPE = "tempProbe";
    /**
     * Strings for the Nodes.
     */
    public static final String PROBE_ELEMENT = "probe";
    public static final String POSITION = "position";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TempRunner.class);
    private static BigDecimal ERROR_TEMP = new BigDecimal(-999);
    /**
     * Base path for BBB System TempProbe.
     */
    private final String bbbSystemTemp =
            "/sys/class/hwmon/hwmon0/device/temp1_input";
    /**
     * Base path for RPi System TempProbe.
     */
    private final String rpiSystemTemp =
            "/sys/class/thermal/thermal_zone1/tempProbe";
    String currentError = null;
    TemperatureValue currentTemp = new TemperatureValue();
    TempProbe tempProbe = null;
    boolean badTemp = false;
    boolean keepAlive = true;
    boolean isStarted = false;
    String fileProbe = null;
    boolean initialized = false;
    boolean loggingOn = true;

    /**
     * Match the temperature regexp.
     */
    private Pattern tempRegexp = null;
    private BigDecimal previousTime = null;
    private BigDecimal tempF = null;
    private BigDecimal currentTime = null;

    public TempRunner(TempProbe tempProbe) {
        this.tempProbe = tempProbe;
    }

    private void initialize() {
        if ("system".equalsIgnoreCase(this.tempProbe.getName())) {
            this.tempProbe.setDevice("System");
            File tempFile = new File(this.rpiSystemTemp);
            if (tempFile.exists()) {
                this.fileProbe = this.rpiSystemTemp;
            } else {
                tempFile = new File(this.bbbSystemTemp);
                if (tempFile.exists()) {
                    this.fileProbe = this.bbbSystemTemp;
                } else {
                    log.warn("Couldn't find a valid system temperature probe");
                    return;
                }
            }

        } else if (!"blank".equalsIgnoreCase(this.tempProbe.getName())) {
            this.fileProbe = "/sys/bus/w1/devices/" + this.tempProbe.getDevice() + "/w1_slave";
            File probePath =
                    new File(this.fileProbe);

            // Lets assume that OWFS has "." separated names
            if (!probePath.exists() && this.tempProbe.getDevice().contains(".")) {
                String[] newAddress = this.tempProbe.getDevice().split("\\.|-");

                if (newAddress.length == 2) {
                    String devFamily = newAddress[0];
                    String devAddress = "";
                    // Byte swap!
                    devAddress += newAddress[1].subSequence(10, 12);
                    devAddress += newAddress[1].subSequence(8, 10);
                    devAddress += newAddress[1].subSequence(6, 8);
                    devAddress += newAddress[1].subSequence(4, 6);
                    devAddress += newAddress[1].subSequence(2, 4);
                    devAddress += newAddress[1].subSequence(0, 2);

                    String fixedAddress = devFamily + "-" + devAddress.toLowerCase();

                    log.info("Converted address: " + fixedAddress);

                    this.fileProbe = "/sys/bus/w1/devices/" + fixedAddress + "/w1_slave";
                    probePath = new File(this.fileProbe);
                    if (probePath.exists()) {
                        this.tempProbe.setDevice(fixedAddress);
                    }
                }
            }
        }

        log.info("{} initialized.", this.tempProbe.getProbe());
        this.initialized = true;
    }

    /**
     * @return The current temperature as read. -999 if it's bad.
     */
    public BigDecimal updateTemp() {
        BigDecimal result;

        if (!this.initialized) {
            initialize();
        }

        if (this.badTemp && this.currentError != null && this.currentError.equals("")) {
            log.warn("Trying to recover {}", this.tempProbe.getName());
        }
        if (this.tempProbe.getProbe() == null) {
            result = updateTempFromOWFS();
        } else {
            result = updateTempFromFile();
        }

        if (result.equals(ERROR_TEMP)) {
            this.badTemp = true;
            return result;
        }

        if (this.badTemp) {
            this.badTemp = false;
            log.warn("Recovered temperature reading for {}", this.tempProbe.getName());
            if (this.currentError.startsWith("Could")) {
                this.currentError = "";
            }
        }

        // OWFS/One wire always uses Celsius
        this.currentTemp.setValue(result, TemperatureValue.Scale.C);

        this.currentError = null;

        if (this.tempProbe.getCutoffEnabled()
                && this.currentTemp.getValue().compareTo(this.tempProbe.getCutoffTemp()) >= 0) {
            log.error("{}: ****** CUT OFF TEMPERATURE ({}) EXCEEDED *****", this.currentTemp, this.tempProbe.getCutoffTemp());
            System.exit(-1);
        }
        return result;
    }

    /**
     * @return Get the current temperature from the OWFS server
     */
    public BigDecimal updateTempFromOWFS() {
        // Use the OWFS connection
        if (isNullOrEmpty(this.tempProbe.getProbe()) || this.tempProbe.getProbe().equals("Blank")) {
            return new BigDecimal(0.0);
        }

        BigDecimal temp = ERROR_TEMP;
        String rawTemp = "";

        try {
            rawTemp = LaunchControl.getInstance().readOWFSPath(this.tempProbe.getProbe() + "/temperature");
            if (rawTemp.equals("")) {
                log.error("Couldn't find the probe {} for {}", this.tempProbe.getProbe() , this.tempProbe.getName());
                LaunchControl.getInstance().setupOWFS();
            } else {
                temp = new BigDecimal(rawTemp);
            }
        } catch (IOException e) {
            this.currentError = "Couldn't read " + this.tempProbe.getProbe();
            log.error(this.currentError, e);
        } catch (OwfsException e) {
            this.currentError = "Couldn't read " + this.tempProbe.getProbe();
            log.error(this.currentError, e);
            LaunchControl.getInstance().setupOWFS();
        } catch (NumberFormatException e) {
            this.currentError = "Couldn't parse" + rawTemp;
            log.error(this.currentError, e);
        }

        this.loggingOn = (!temp.equals(ERROR_TEMP));

        return temp;
    }

    public BigDecimal updateTempFromFile() {
        if (StringUtils.isEmpty(this.fileProbe)) {
            log.warn("No File to probe");
            return BigDecimal.ZERO;
        }

        BufferedReader br = null;
        String temp = null;

        BigDecimal newTemperature = null;

        try {
            br = new BufferedReader(new FileReader(this.fileProbe));
            String line = br.readLine();

            if (line == null || line.contains("NO")) {
                // bad CRC, do nothing
                this.currentError = "Bad CRC from " + this.fileProbe;
            } else if (line.contains("YES")) {
                // good CRC
                line = br.readLine();
                // last value should be t=
                int t = line.indexOf("t=");
                temp = line.substring(t + 2);
                BigDecimal tTemp = new BigDecimal(temp);
                newTemperature = MathUtil.divide(tTemp, 1000);
                this.currentError = null;
            } else {
                // System Temperature
                BigDecimal tTemp = new BigDecimal(line);
                newTemperature = MathUtil.divide(tTemp, 1000);
            }

        } catch (IOException ie) {
            if (this.loggingOn) {
                this.currentError = "Couldn't find the device under: " + this.fileProbe;
                log.warn(this.currentError);
                if (this.fileProbe.equals(this.rpiSystemTemp)) {
                    this.fileProbe = this.bbbSystemTemp;
                }
            }
            return ERROR_TEMP;
        } catch (NumberFormatException nfe) {
            this.currentError = "Couldn't parse " + temp + " as a double";
            nfe.printStackTrace();
        } catch (Exception e) {
            this.currentError = "Couldn't update temperature from file";
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ie) {
                    log.warn(ie.getLocalizedMessage());
                }
            }
        }
        if (newTemperature == null) {
            newTemperature = getTempC();
        }
        return newTemperature;
    }


    /**
     * @return Get the current temperature
     */
    public BigDecimal getTemperature() {
        updateTemp();
        // set up the reader
        if (this.tempProbe.getScale().equals("F")) {
            return getTempF();
        }
        return getTempC();
    }

    /**
     * @return The current temperature in fahrenheit.
     */
    public BigDecimal getTempF() {
        BigDecimal temp = this.currentTemp.getValue(TemperatureValue.Scale.F);
        if (temp == null) {
            return null;
        }
        return temp.add(this.tempProbe.getCalibration());
    }

    /**
     * @return The current temperature in celsius.
     */
    BigDecimal getTempC() {
        BigDecimal temp = this.currentTemp.getValue(TemperatureValue.Scale.C);
        if (temp == null) {
            return null;
        }
        return temp.add(this.tempProbe.getCalibration());
    }


    public BigDecimal convertF(BigDecimal temperature) {
        if (this.tempProbe.getScale().equals("C")) {
            return cToF(temperature);
        }

        return temperature;
    }


    public void shutdown() {
        // Graceful shutdown.
        this.keepAlive = false;
        log.warn("{} is shutting down.", this.tempProbe.getName());
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


    public TempProbe getTempProbe() {
        return this.tempProbe;
    }

    public String getName() {
        return this.tempProbe.getName();
    }

    /***
     * Main loop for using a PID Thread.
     */
    @Override
    public void run() {
        startRunning();
        BrewServer.LOG.info("Running " + tempProbe.getName() + ".");
        // setup the first time
        this.previousTime = new BigDecimal(System.currentTimeMillis());


        // Main loop
        while (isRunning()) {
            try {
                synchronized (this.tempF = getTempF()) {
                    // do the bulk of the work here
                    this.currentTime = new BigDecimal(System.currentTimeMillis());
                    loopRun();

                }

                //pause execution for a second
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                BrewServer.LOG.warning("PID " + tempProbe.getName() + " Interrupted.");
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
        BrewServer.LOG.warning("Shutting down " + getTempProbe().getName());
        shutdown();
        Thread.currentThread().interrupt();
    }
}
