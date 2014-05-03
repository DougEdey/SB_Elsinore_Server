package com.sb.elsinore;
import jGPIO.GPIO.Direction;
import jGPIO.InPin;
import jGPIO.InvalidGPIOException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.owfs.jowfsclient.OwfsException;

/**
 * Temp class is used to monitor temperature on the one wire system.
 * @author Doug Edey
 *
 */
public final class Temp implements Runnable {

    /**
     * Base path for BBB System Temp.
     */
    private final String bbbSystemTemp =
            "/sys/class/hwmon/hwmon0/device/temp1_input";
    /**
     * Base path for RPi System Temp.
     */
    private final String rpiSystemTemp =
            "/sys/class/thermal/thermal_zone0/temp";
    /**
     * Match the temperature regexp.
     */
    private final Pattern tempRegexp = Pattern.compile("(-?)(\\d{1,3})(C|F)");

    /**
     * Save the current object to the configuration using LaunchControl.
     */
    public void save() {
        if (name != null && !name.equals("")) {
            LaunchControl.addTempToConfig(probeName, name);
        }
    }

    /**
     * Standard constructor.
     * @param input The probe name or input name.
     *  Use "system" to create a system temperature probe
     * @param inName The name of this temperature probe.
     */
    public Temp(final String input, final String inName) {

        String aName = inName;
        BrewServer.LOG.info("Adding" + aName);
        if (input.equalsIgnoreCase("system")) {
            File tempFile = new File(rpiSystemTemp);
            if (tempFile.exists()) {
                fProbe = rpiSystemTemp;
            } else {
                tempFile = new File(bbbSystemTemp);
                if (tempFile.exists()) {
                    fProbe = bbbSystemTemp;
                } else {
                    BrewServer.LOG.info(
                        "Couldn't find a valid system temperature probe");
                    return;
                }
            }
        } else if (LaunchControl.getOWFS() != null) {
            try {
                aName = aName.replace("-", ".");
                BrewServer.LOG.info("Using OWFS for " + aName + "/temperature");
                if ("" == LaunchControl.readOWFSPath(aName + "/temperature")) {
                    String[] newAddress = aName.split("\\.|-");

                    // OWFS contains "-", W1 contained "."
                    if (newAddress.length == 2 && aName.indexOf("-") != 2) {
                        String devFamily = newAddress[0];
                        StringBuilder devAddress = new StringBuilder();

                        // Byte Swap time!
                        devAddress.append(newAddress[1].subSequence(10, 12));
                        devAddress.append(newAddress[1].subSequence(8, 10));
                        devAddress.append(newAddress[1].subSequence(6, 8));
                        devAddress.append(newAddress[1].subSequence(4, 6));
                        devAddress.append(newAddress[1].subSequence(2, 4));
                        devAddress.append(newAddress[1].subSequence(0, 2));

                        String fixedAddress =
                            devFamily.toString() + "."
                            + devAddress.toString().toUpperCase();

                        System.out.println("Converted address: "
                            + fixedAddress);

                        aName = fixedAddress;
                        if ("" == LaunchControl.readOWFSPath(
                                aName + "/temperature")) {
                            BrewServer.LOG.severe(
                                "This is not a temperature probe " + aName);
                        }
                    }
                }
            } catch (OwfsException e) {
                BrewServer.LOG.log(Level.SEVERE,
                    "This is not a temperature probe!", e);
            } catch (IOException e) {
                e.printStackTrace();
            }
            fProbe = null;
        } else {

            File probePath =
                new File("/sys/bus/w1/devices/" + aName + "/w1_slave");

            // Lets assume that OWFS has "." separated names
            if (!probePath.exists() && aName.indexOf(".") != 2) {
                String[] newAddress = aName.split("\\.|-");

                if (newAddress.length == 2) {
                    String devFamily = newAddress[0];
                    StringBuilder devAddress = new StringBuilder();
                    // Byte swap!
                    devAddress.append(newAddress[1].subSequence(10, 12));
                    devAddress.append(newAddress[1].subSequence(8, 10));
                    devAddress.append(newAddress[1].subSequence(6, 8));
                    devAddress.append(newAddress[1].subSequence(4, 6));
                    devAddress.append(newAddress[1].subSequence(2, 4));
                    devAddress.append(newAddress[1].subSequence(0, 2));

                    String fixedAddress = devFamily.toString() + "."
                        + devAddress.toString().toLowerCase();

                    System.out.println("Converted address: " + fixedAddress);

                    aName = fixedAddress;
                    probePath = null;
                }
            }
            fProbe = "/sys/bus/w1/devices/" + aName + "/w1_slave";
        }

        probeName = aName;
        name = input;
        BrewServer.LOG.info(probeName + " added.");
    }

    /**
     * The Runnable loop.
     */
    public void run() {

        while (true) {
            if (updateTemp() == -999) {
                if (fProbe == null || fProbe.equals(
                        "/sys/class/thermal/thermal_zone0/temp")) {
                    return;
                }
                // Uh(oh no file found, disable output to prevent logging floods
                loggingOn = false;
            } else {
                loggingOn = true;
            }

            if (volumeMeasurement) {
                updateVolume();
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param n The name to set this Temp to.
     */
    public void setName(final String n) {
        this.name = n;
    }

    /**
     * @return The name of this probe
     */
    public String getName() {
        return name;
    }

    /**
     * @return The address of this probe
     */
    public String getProbe() {
        return probeName;
    }

    /******
     * Method to take a cutoff value, parse it to the correct scale
     *  and then update the cutoffTemp.
     * @param cutoffInput String describing the temperature
     */
    public void setCutoffTemp(final String cutoffInput) {
        Matcher tempMatcher = tempRegexp.matcher(cutoffInput);

        if (tempMatcher.find()) {
            // We have matched against the TEMP_REGEXP
            String negative = tempMatcher.group(1);
            if (negative == null) {
                negative = "+";
            }

            Double temperature = Double.parseDouble(
                negative + tempMatcher.group(2));
            String unit = tempMatcher.group(3);

            if (unit.equals(this.scale)) {
                this.cutoffTemp = temperature;
            } else if (unit.equals("F")) {
                this.cutoffTemp = fToC(temperature);
            } else if (unit.equals("C")) {
                this.cutoffTemp = cToF(temperature);
            }

        } else {
            BrewServer.LOG.severe(cutoffTemp + " doesn't match "
                    + tempRegexp.pattern());
        }
    }

    // PRIVATE ////
    /**
     * Setup strings for the probe.
     */
    private String fProbe, name, probeName;
    /**
     * Turn on and off logging.
     */
    private boolean loggingOn = true;
    /**
     * Hold the current error string.
     */
    private String currentError = null;

    /**
     * The current temp.
     */
    private double currentTemp = 0, currentVolume = 0, cutoffTemp = -999,
            volumeConstant = 0, volumeMultiplier = 0.0;

    /**
     * The current timestamp.
     */
    private long currentTime = 0;
    /**
     * Other strings, obviously named.
     */
    private String scale = "C", volumeAddress = null, volumeOffset = null,
            volumeUnit = null;

    /**
     * Are we measuring volume?
     */
    private boolean volumeMeasurement = false;
    /**
     * Volume analog input number.
     */
    private int volumeAIN = -1;
    /**
     * The baselist of volume measurements.
     */
    private ConcurrentHashMap<Double, Double> volumeBase = null;

    /**
     * The input pin to read.
     */
    private InPin volumePin = null;

    /**
     * @return Get the current temperature
     */
    public double getTemp() {
        // set up the reader
        if (scale.equals("F")) {
            return getTempF();
        }
        return getTempC();
    }

    /**
     * @return The temp unit/Scale
     */
    public String getScale() {
        return scale;
    }

    /**
     * @param s Value to set the temperature unit to.
     */
    public void setScale(String s) {
        if (s.equalsIgnoreCase("F"))
        {
            // Do we need to convert the cutoff temp
            if (cutoffTemp != -999 && !scale.equalsIgnoreCase(s)) {
                this.cutoffTemp = cToF(cutoffTemp);
            }

            this.scale = s;
        }
        if (s.equalsIgnoreCase("C"))
        {
            // Do we need to convert the cutoff temp
            if (cutoffTemp != -999 && !scale.equalsIgnoreCase(s)) {
                this.cutoffTemp = fToC(cutoffTemp);
            }

            this.scale = s;
        }
    }

    /**
     * @return The current temperature in fahrenheit.
     */
    public double getTempF() {
        if (scale.equals("F")) {
            return currentTemp;
        }
        return (currentTemp - 32) / (9.0 * 5.0);
    }

    /**
     * @return The current temperature in celsius.
     */
    public double getTempC() {
        if (scale.equals("C")) {
            return currentTemp;
        }
        return (9.0 / 5.0) * currentTemp + 32;
    }

    /**
     * @param temp temperature to convert in Fahrenheit
     * @return temp in celsius
     */
    public double fToC(final double temp) {
        return (temp - 32) / (9.0 * 5.0);
    }

    /**
     * @param temp temperature to convert in Celsius
     * @return temp in Fahrenheit
     */
    public double cToF(final double temp) {
        return (9.0 / 5.0) * temp + 32;
    }

    /**
     * @return The current timestamp.
     */
    public long getTime() {
        return currentTime;
    }

    /**
     * @return The current temperature as read. -999 if it's bad.
     */
    public double updateTemp() {
        double result = -1L;

        if (fProbe == null) {
            result = updateTempFromOWFS();
        } else {
            result = updateTempFromFile();
        }

        if (result == -999) {
            return result;
        }

        if (scale.equals("F")) {
            result = (9.0/5.0)*result + 32;
        }

        currentTemp = result;
        currentTime = System.currentTimeMillis();
        currentError = null;

        if (cutoffTemp != -999 && currentTemp >= cutoffTemp) {
            BrewServer.LOG.log(Level.SEVERE,
                currentTemp + ": ****** CUT OFF TEMPERATURE ("
                + cutoffTemp + ") EXCEEDED *****");
            System.exit(-1);
        }
        return result;
    }

    /**
     * @return Get the current temperature from the OWFS server
     */
    public double updateTempFromOWFS() {
        // Use the OWFS connection
        double temp = -999;
        String rawTemp = "";
        try {
            rawTemp =  LaunchControl.readOWFSPath(probeName + "/temperature");
            if (rawTemp == null || rawTemp.equals("")) {
                BrewServer.LOG.severe(
                    "Couldn't find the probe " + probeName + " for " + name);
            } else {
                temp = Double.parseDouble(rawTemp);
            }
        } catch (IOException e) {
            currentError = "Couldn't read " + probeName;
            BrewServer.LOG.log(Level.SEVERE, currentError, e);
        } catch (OwfsException e) {
            currentError = "Couldn't read " + probeName;
            BrewServer.LOG.log(Level.SEVERE, currentError, e);
            LaunchControl.setupOWFS();
        } catch (NumberFormatException e) {
            currentError = "Couldn't parse" + rawTemp;
            BrewServer.LOG.log(Level.SEVERE, currentError, e);
        }

        return temp;
    }

    /**
     * @return The current temperature read directly from the file system.
     */
    public double updateTempFromFile() {
        BufferedReader br = null;
        String temp = null;

        try {
            br = new BufferedReader(new FileReader(fProbe));
            String line = br.readLine();
            if (line.contains("NO")) {
                // bad CRC, do nothing
                this.currentError = "Bad CRC from " + fProbe;
            } else if (line.contains("YES")) {
                // good CRC
                line = br.readLine();
                // last value should be t=
                int t = line.indexOf("t=");
                temp = line.substring(t + 2);
                double tTemp = Double.parseDouble(temp);
                this.currentTemp = tTemp / 1000;
                this.currentError = null;
            } else {
                // System Temperature
                this.currentTemp = Double.parseDouble(line) / 1000;
            }

        } catch (IOException ie) {
            if (loggingOn) {
                this.currentError = "Couldn't find the device under: " + fProbe;
                System.out.println(currentError);
                if (fProbe == rpiSystemTemp) {
                    fProbe = bbbSystemTemp;
                }
            }
            return -999;
        } catch (NumberFormatException nfe) {
            this.currentError = "Couldn't parse " + temp + " as a double";
            nfe.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ie) {
                    BrewServer.LOG.info(ie.getLocalizedMessage());
                }
            }
        }
        return currentTemp;
    }

    /**
     * Setup the volume reading.
     * @param address One Wire device address.
     * @param offset Device offset input.
     * @param unit The volume unit to read as.
     */
    public void setupVolumes(final String address, final String offset,
            final String unit) {
        this.volumeMeasurement = true;
        this.volumeUnit = unit;
        this.volumeAddress = address.replace("-", ".");
        this.volumeOffset = offset.toUpperCase();

        try {
            BrewServer.LOG.log(Level.INFO,
                "Volume ADC at: " + volumeAddress + " - " + offset);
            String temp =
                LaunchControl.readOWFSPath(volumeAddress + "/volt." + offset);
            if (temp.equals("")) {
                BrewServer.LOG.severe(
                    "Couldn't read the Volume from " + volumeAddress
                    + "/volt." + offset);
            } else {
                BrewServer.LOG.log(Level.INFO, "Volume reads " + temp);
            }
        } catch (IOException e) {
            BrewServer.LOG.log(Level.SEVERE,
                "IOException when access the ADC over 1wire", e);
        } catch (OwfsException e) {
            BrewServer.LOG.log(Level.SEVERE,
                "OWFSException when access the ADC over 1wire", e);
        }

        if (volumeConstant != 0.0 && volumeMultiplier != 0.0) {
            BrewServer.LOG.info("Volume constants and multiplier are good");
        } else {
            BrewServer.LOG.info("Volume constants and multiplier show "
                    + "there's no change in the pressure sensor");
        }
    }

    /**
     * Setup volumes for AIN pins.
     * @param analogPin The AIN pin number
     * @param unit The volume unit
     * @throws InvalidGPIOException If the Pin cannot be setup
     */
    public void setupVolumes(final int analogPin, final String unit)
            throws InvalidGPIOException {
        // start a volume measurement at the same time
        this.volumeMeasurement = true;
        this.volumeUnit = unit;

        try {
            this.volumePin = new InPin(analogPin, Direction.ANALOGUE);
        } catch (InvalidGPIOException e) {
            System.out.println("Invalid Analog GPIO specified " + analogPin);
            throw(e);
        }

        setupVolume();

        if (volumeConstant == 0.0 || volumeMultiplier == 0.0) {
            this.volumeMeasurement = false;
        }

    }

    /**
     * Calculate volume reading maths.
     */
    public void setupVolume() {
        if (volumeBase == null) {
            return;
        }

        // Calculate the values of b*value + c = volume
        // get the value of c
        this.volumeMultiplier = 0.0;
        this.volumeConstant = 0.0;

        // for the rest of the values
        Iterator<Entry<Double, Double>> it = volumeBase.entrySet().iterator();
        Entry<Double, Double> prevPair = null;

        while (it.hasNext()) {
            Entry<Double, Double> pairs = (Entry<Double, Double>) it.next();
            if (prevPair != null) {
                // diff the pair value and dive by the diff of the key
                double keyDiff = pairs.getKey() - prevPair.getKey();
                double valueDiff = pairs.getValue() - prevPair.getValue();
                double newMultiplier = valueDiff / keyDiff;
                double newConstant = pairs.getValue() - (valueDiff * keyDiff);

                if (volumeMultiplier != 0.0) {
                    if (newMultiplier != volumeMultiplier) {
                        System.out.println(
                            "The newMultiplier isn't the same as the old one,"
                            + " if this is a big difference, be careful!"
                            + " You may need a quadratic!");
                        System.out.println("New: " + newMultiplier
                            + ". Old: " + volumeMultiplier);
                    }
                } else {
                    this.volumeMultiplier = newMultiplier;
                }

                if (volumeConstant != 0.0) {
                    if (newConstant != volumeConstant) {
                        System.out.println("The new constant "
                            + "isn't the same as the old one, if this is a big"
                            + " difference, be careful!"
                            + " You may need a quadratic!");
                        System.out.println("New: " + newConstant
                            + ". Old: " + volumeConstant);
                    }
                } else {
                    this.volumeConstant = newConstant;
                }
            }
        }

        // we should be done now
    }


    /**
     * @return The latest volume reading
     */
    public double updateVolume() {
        try {
            double pinValue = 0;
            if (volumeAIN != -1) {
                pinValue = Integer.parseInt(volumePin.readValue());
            } else if (volumeAddress != null && volumeOffset != null) {
                try {
                    pinValue = Double.parseDouble(
                        LaunchControl.readOWFSPath(
                            volumeAddress + "/volt." + volumeOffset));
                } catch (Exception e) {
                    BrewServer.LOG.log(Level.SEVERE,
                            "Could not update the volume reading from OWFS", e);
                    LaunchControl.setupOWFS();
                    return 0.0;
                }

            } else {
                return 0.0;
            }

            // Are we outside of the known range?
            Double curKey = null, prevKey = null;
            Double curValue = null, prevValue = null;
            SortedSet<Double> keys = null;
            try {
                keys = Collections.synchronizedSortedSet(
                    new TreeSet<Double>(volumeBase.keySet()));
            } catch (NullPointerException npe) {
                // No VolumeBase setup, so we're probably calibrating
                return pinValue;
            } catch (Exception e) {
                BrewServer.LOG.log(Level.SEVERE,
                    "Uncaught exception when creating the volume set", e);
                System.exit(-1);
            }

            double tVolume = -1;

            try  {
                for (Double key: keys) {
                    if (prevKey == null) {
                        prevKey = key;
                        prevValue = volumeBase.get(key);
                        continue;
                    } else if (curKey != null) {
                        prevKey = curKey;
                        prevValue = curValue;
                    }

                    curKey = key;
                    curValue = volumeBase.get(key);

                    if (pinValue >= prevValue && pinValue <= curValue) {
                        // We have a set encompassing the values!
                        // assume it's linear
                        double volRange = curKey - prevKey;
                        double readingRange = curValue - prevValue;
                        double ratio = ((double) pinValue - prevValue)
                                / readingRange;
                        double volDiff = ratio * volRange;
                        tVolume = volDiff + prevKey;
                    }

                }

                if (tVolume == -1 && curKey != null && prevKey != null) {
                    // Try to extrapolate
                    double volRange = curKey - prevKey;
                    double readingRange = curValue - prevValue;
                    double ratio = ((double) pinValue - prevValue)
                            / readingRange;
                    double volDiff = ratio * volRange;
                    tVolume = volDiff + prevKey;
                }

            } catch (NoSuchElementException e) {
                // no more elements
                BrewServer.LOG.info("Finished reading Volume Elements");
            }

            if (tVolume == -1) {
                // try to assume the value
                this.currentVolume = (pinValue - volumeConstant)
                        * volumeMultiplier;
            } else {
                this.currentVolume = tVolume;
            }

            return pinValue;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * @param unit Unit to set the volume units to.
     */
    public void setVolumeUnit(final String unit) {
        this.volumeUnit = unit;
    }

    /**
     * Append a volume measurement to the current list of calibrated values.
     * @param volume Volume measurement to record.
     */
    public void addVolumeMeasurement(final double volume) {
        // record 10 readings and average it
        int maxReads = 10;
        int total = 0;
        for (int i = 0; i < maxReads; i++) {
            try {
                try {
                    total += Integer.parseInt(volumePin.readValue());
                } catch (RuntimeException re) {
                    re.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            } catch (NumberFormatException  e) {
                System.out.println("Bad Analog input value!");
                return;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        // read in ten values
        double avgValue = (double) total / maxReads;

        volumeBase.put(volume, avgValue);

        System.out.println("Read " + avgValue + " for "
                + volume + " " + volumeUnit.toString());
        return;
    }

    /**
     * Add a volume measurement at a specific key.
     * @param key The key to overwrite/set
     * @param value The value to overwrite/set
     */
    public void addVolumeMeasurement(final Double key, final Double value) {
        BrewServer.LOG.info("Adding " + key + " with value " + value);
        if (volumeBase == null) {
            this.volumeBase = new ConcurrentHashMap<Double, Double>();
        }
        this.volumeBase.put(key, value);
    }

    /**
     * @return The current volume measurement,
     *       -1.0 is there is no measurement enabled.
     */
    public double getVolume() {
        if (this.volumeMeasurement) {
            return this.currentVolume;
        }

        return -1.0;
    }

    /**
     * @return The current volume unit
     */
    public String getVolumeUnit() {
        return this.volumeUnit;
    }

    /**
     * @return The analogue input
     */
    public int getVolumeAIN() {
        return this.volumeAIN;
    }

    /**
     * @return Get the volume address
     */
    public String getVolumeAddress() {
        return this.volumeAddress;
    }

    /**
     * @return The current volume offset
     */
    public String getVolumeOffset() {
        return this.volumeOffset;
    }

    /**
     * @return The current volume base map
     */
    public ConcurrentHashMap<Double, Double> getVolumeBase() {
        return this.volumeBase;
    }

    /*****
     * Helper function to return a map of the current status.
     * @return The current status of the temperature probe.
     */
    public Map<String, Object> getMapStatus() {
        Map<String, Object> statusMap = new HashMap<String, Object>();

        statusMap.put("temp", getTemp());
        statusMap.put("elapsed", getTime());
        statusMap.put("scale", getScale());

        double tVolume = getVolume();
        if (volumeMeasurement && tVolume != -1.0) {
            statusMap.put("volume", tVolume);
            statusMap.put("volumeUnits", getVolumeUnit());
        }

        if (currentError != null) {
            statusMap.put("error", currentError);
        }

        return statusMap;
    }
}


