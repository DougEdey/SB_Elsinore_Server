package com.sb.elsinore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.sb.elsinore.devices.I2CDevice;
import com.sb.util.MathUtil;
import jGPIO.GPIO.Direction;
import jGPIO.InPin;
import jGPIO.InvalidGPIOException;
import org.apache.commons.lang3.StringUtils;
import org.owfs.jowfsclient.OwfsException;

import javax.annotation.Nonnull;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Temp class is used to monitor temperature on the one wire system.
 *
 * @author Doug Edey
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@DiscriminatorValue("T")
@EntityListeners(TemperatureListeners.class)
public class Temp extends Device implements Comparable<Temp> {

    private static final Logger log = Logger.getLogger(Temp.class.getCanonicalName());

    public static final String TYPE = "temp";
    /**
     * Strings for the Nodes.
     */
    public static final String PROBE_ELEMENT = "probe";
    public static final String POSITION = "position";

    /**
     * Magic numbers.
     * F_TO_C_MULT: Multiplier to convert F to C.
     * C_TO_F_MULT: Multiplier to convert C to F.
     * FREEZING: The freezing point of Fahrenheit.
     */
    @Transient
    public MathContext context = new MathContext(2, RoundingMode.HALF_DOWN);
    @Transient
    private static BigDecimal FREEZING = new BigDecimal(32);
    @Transient
    private static BigDecimal ERROR_TEMP = new BigDecimal(-999);
    @Transient
    private boolean badTemp = false;
    @Transient
    private boolean keepAlive = true;
    @Transient
    private boolean isStarted = false;
    @Transient
    private String fileProbe = null;
    @Expose
    private boolean hidden = false;
    @Expose
    boolean cutoffEnabled = false;
    @Transient
    boolean initialized = false;

    /**
     * Base path for BBB System Temp.
     */
    private final String bbbSystemTemp =
            "/sys/class/hwmon/hwmon0/device/temp1_input";
    /**
     * Base path for RPi System Temp.
     */
    private final String rpiSystemTemp =
            "/sys/class/thermal/thermal_zone1/temp";
    /**
     * Match the temperature regexp.
     */
    @Transient
    private Pattern tempRegexp = null;

    @JsonProperty("i2c_device")
    I2CDevice i2cDevice = null;
    @JsonProperty("i2c_channel")
    int i2cChannel = -1;
    @JsonProperty("device")
    @NotNull
    @javax.validation.constraints.Size(min = 1)
    String device = "";

    private boolean loggingOn = true;

    public Temp() {
    }

    public Temp(Device device) {
        super(device);

        if (!(device instanceof Temp)) {
            return;
        }
        Temp other = (Temp) device;
        this.name = other.name;
        this.scale = other.scale;
        this.device = other.device;
    }

    /**
     * Standard constructor.
     *
     * @param name    The probe name or input name.
     *                Use "system" to create a system temperature probe
     * @param inProbe The address of this temperature probe.
     * @param type    The type of this device (Defaults to {@link #TYPE}
     */
    public Temp(String name, String inProbe, String type) {
        super(type);
        this.device = inProbe;
        log.info("Adding " + this.device);


        setName(name);
        this.probeName = this.device;
        log.info(this.probeName + " added.");
        LaunchControl.getInstance().addTemp(this);
    }

    /**
     * Standard constructor.
     *
     * @param name    The probe name or input name.
     *                Use "system" to create a system temperature probe
     * @param inProbe The address of this temperature probe.
     */
    public Temp(String name, String inProbe) {
        this(name, inProbe, TYPE);
    }

    /**
     * @param n The name to set this Temp to.
     */
    public void setName(String name) {
        this.name = name;
    }

    private void initialize() {
        if (this.name.equalsIgnoreCase("system")) {
            this.device = "System";
            File tempFile = new File(this.rpiSystemTemp);
            if (tempFile.exists()) {
                this.fileProbe = this.rpiSystemTemp;
            } else {
                tempFile = new File(this.bbbSystemTemp);
                if (tempFile.exists()) {
                    this.fileProbe = this.bbbSystemTemp;
                } else {
                    log.warning(
                            "Couldn't find a valid system temperature probe");
                    return;
                }
            }

        } else if (!this.name.equalsIgnoreCase("Blank")) {
            this.fileProbe = "/sys/bus/w1/devices/" + this.device + "/w1_slave";
            File probePath =
                    new File(this.fileProbe);

            // Lets assume that OWFS has "." separated names
            if (!probePath.exists() && this.device.contains(".")) {
                String[] newAddress = this.device.split("\\.|-");

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

                    String fixedAddress = devFamily + "-"
                            + devAddress.toLowerCase();

                    log.info("Converted address: " + fixedAddress);

                    this.fileProbe = "/sys/bus/w1/devices/" + fixedAddress + "/w1_slave";
                    probePath = new File(this.fileProbe);
                    if (probePath.exists()) {
                        this.device = fixedAddress;
                    }
                }
            }
        }
        this.probeName = this.device;
        log.info(this.probeName + " initialized.");
        this.initialized = true;
    }

    /**
     * @return The name of this probe
     */
    public String getName() {
        return this.name;
    }

    public String getDevice() {
        return this.device;
    }

    public void setDevice(String device) {
        if (isNullOrEmpty(device)) {
            this.device = null;
        } else {
            this.device = device;
        }
    }

    /**
     * @return The address of this probe
     */
    public String getProbe() {
        return this.probeName;
    }

    /******
     * Method to take a cutoff value, parse it to the correct scale
     *  and then update the cutoffTemp.
     * @param cutoffInput String describing the temperature
     */
    void setCutoffTemp(final String cutoffInput) {
        if (isNullOrEmpty(cutoffInput)) {
            this.cutoffEnabled = false;
            this.cutoffTemp = null;
            log.warning("Disabling cut off temperature");
            return;
        }

        Matcher tempMatcher = getTempRegexp().matcher(cutoffInput);

        if (tempMatcher.find()) {
            // We have matched against the TEMP_REGEXP
            String number = tempMatcher.group(1);
            if (number == null) {
                number = "+";
            }
            // Get the integer
            if (tempMatcher.group(2) != null) {
                number += tempMatcher.group(2);
            }
            // Do we have a decimal?
            if (tempMatcher.group(3) != null) {
                number += tempMatcher.group(3);
            }
            // Create the temp
            this.cutoffTemp = new BigDecimal(number);
        } else {
            log.severe(this.cutoffTemp + " doesn't match "
                    + getTempRegexp().pattern());
        }
    }

    // PRIVATE ////
    @JsonProperty("name")
    private String name;
    @JsonProperty("probeName")
    private String probeName;

    /**
     * Hold the current error string.
     */
    @Expose
    public String currentError = null;

    /**
     * The current temp.
     */
    @Expose
    private BigDecimal currentTemp = new BigDecimal(0),
            currentVolume = new BigDecimal(0),
            cutoffTemp = new BigDecimal(-999.0),
            volumeConstant = new BigDecimal(0),
            volumeMultiplier = new BigDecimal(0.0),
            gravity = new BigDecimal(1.000);

    /**
     * The current timestamp.
     */
    @Transient
    private Long currentTime = 0L;
    /**
     * Other strings, obviously named.
     */
    @Expose
    private String scale = "C", volumeAddress = "", volumeOffset = "",
            volumeUnit = "";

    /**
     * Are we measuring volume?
     */
    @Expose
    private boolean volumeMeasurement = false;
    /**
     * Volume analog input number.
     */
    @Expose
    private int volumeAIN = -1;
    /**
     * The baselist of volume measurements.
     */
    private ConcurrentHashMap<BigDecimal, BigDecimal> volumeBase = null;

    /**
     * The input pin to read.
     */
    @Transient
    private InPin volumePin = null;
    private Boolean stopVolumelogging;
    private BigDecimal calibration = BigDecimal.ZERO;
    private TriggerControl triggerControl = null;
    private Integer position = -1;

    /**
     * @return Get the current temperature
     */
    public BigDecimal getTemp() {
        updateTemp();
        // set up the reader
        if (this.scale.equals("F")) {
            return getTempF();
        }
        return getTempC();
    }

    /**
     * @return The temp unit/Scale
     */
    public String getScale() {
        return this.scale;
    }

    /**
     * @param s Value to set the temperature unit to.
     */
    public void setScale(final String s) {
        log.warning("Cut off is: " + this.cutoffTemp);

        if (s.equalsIgnoreCase("F")) {
            // Do we need to convert the cutoff temp
            if (this.cutoffTemp.compareTo(ERROR_TEMP) != 0
                    && !this.scale.equalsIgnoreCase(s)) {
                this.cutoffTemp = cToF(this.cutoffTemp);
            }

            this.calibration = this.calibration.multiply(new BigDecimal(1.8));

            this.scale = s;
        }

        if (s.equalsIgnoreCase("C")) {
            // Do we need to convert the cutoff temp
            if (this.cutoffTemp.compareTo(ERROR_TEMP) != 0
                    && !this.scale.equalsIgnoreCase(s)) {
                this.cutoffTemp = fToC(this.cutoffTemp);
            }
            this.calibration = this.calibration.divide(new BigDecimal(1.8), this.context);
            this.scale = s;
        }
        log.warning("Cut off is now: " + this.cutoffTemp);
    }

    /**
     * @return The current temperature in fahrenheit.
     */
    BigDecimal getTempF() {
        if (this.scale.equals("F")) {
            return this.currentTemp.add(this.calibration);
        }
        return cToF(this.currentTemp.add(this.calibration));
    }

    /**
     * @return The current temperature in celsius.
     */
    BigDecimal getTempC() {
        if (this.scale.equals("C")) {
            return this.currentTemp.add(this.calibration);
        }
        return fToC(this.currentTemp.add(this.calibration));
    }

    /**
     * @param currentTemp temperature to convert in Fahrenheit
     * @return Temperature in celsius
     */
    private static BigDecimal fToC(final BigDecimal currentTemp) {
        BigDecimal t = currentTemp.subtract(FREEZING);
        t = MathUtil.divide(MathUtil.multiply(t, 5), 9);
        return t;
    }

    /**
     * @param currentTemp temperature to convert in Celsius
     * @return Temperature in Fahrenheit
     */
    static BigDecimal cToF(final BigDecimal currentTemp) {
        BigDecimal t = MathUtil.divide(MathUtil.multiply(currentTemp, 9), 5);
        t = t.add(FREEZING);
        return t;
    }

    /**
     * @return The current timestamp.
     */
    public long getTime() {
        return this.currentTime;
    }

    /**
     * @return The current cutoff temp.
     */
    String getCutoff() {
        return this.cutoffTemp.toPlainString();
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
            log.warning("Trying to recover " + this.getName());
        }
        if (this.probeName == null) {
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
            log.warning("Recovered temperature reading for " + this.getName());
            if (this.currentError.startsWith("Could")) {
                this.currentError = "";
            }
        }

        // OWFS/One wire always uses Celsius
        if (this.scale.equals("F")) {
            result = cToF(result);
        }

        this.currentTemp = result;
        this.currentTime = System.currentTimeMillis();
        this.currentError = null;

        if (this.cutoffEnabled
                && this.currentTemp.compareTo(this.cutoffTemp) >= 0) {
            log.log(Level.SEVERE,
                    this.currentTemp + ": ****** CUT OFF TEMPERATURE ("
                            + this.cutoffTemp + ") EXCEEDED *****");
            System.exit(-1);
        }
        return result;
    }

    /**
     * @return Get the current temperature from the OWFS server
     */
    public BigDecimal updateTempFromOWFS() {
        // Use the OWFS connection
        if (isNullOrEmpty(this.probeName) || this.probeName.equals("Blank")) {
            return new BigDecimal(0.0);
        }

        BigDecimal temp = ERROR_TEMP;
        String rawTemp = "";

        try {
            rawTemp = LaunchControl.getInstance().readOWFSPath(this.probeName + "/temperature");
            if (rawTemp.equals("")) {
                log.severe(
                        "Couldn't find the probe " + this.probeName + " for " + this.name);
                LaunchControl.getInstance().setupOWFS();
            } else {
                temp = new BigDecimal(rawTemp);
            }
        } catch (IOException e) {
            this.currentError = "Couldn't read " + this.probeName;
            log.log(Level.SEVERE, this.currentError, e);
        } catch (OwfsException e) {
            this.currentError = "Couldn't read " + this.probeName;
            log.log(Level.SEVERE, this.currentError, e);
            LaunchControl.getInstance().setupOWFS();
        } catch (NumberFormatException e) {
            this.currentError = "Couldn't parse" + rawTemp;
            log.log(Level.SEVERE, this.currentError, e);
        }

        this.loggingOn = (!temp.equals(ERROR_TEMP));

        return temp;
    }

    public BigDecimal updateTempFromFile() {
        if (StringUtils.isEmpty(this.fileProbe)) {
            log.warning("No File to probe");
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
                log.warning(this.currentError);
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
                    log.warning(ie.getLocalizedMessage());
                }
            }
        }
        if (newTemperature == null) {
            newTemperature = getTempC();
        }
        return newTemperature;
    }

    /**
     * Setup the volume reading.
     *
     * @param address One Wire device address.
     * @param offset  Device offset input.
     * @param unit    The volume unit to read as.
     */
    boolean setupVolumes(final String address, final String offset,
                         final String unit) {
        // start a volume measurement at the same time
        if (unit == null
                || (!unit.equals(VolumeUnits.LITRES)
                && !unit.equals(VolumeUnits.UK_GALLONS)
                && !unit.equals(VolumeUnits.US_GALLONS))) {
            return false;
        }

        this.volumeMeasurement = true;
        this.volumeUnit = unit;
        this.volumeAddress = address.replace("-", ".");
        this.volumeOffset = offset.toUpperCase();

        try {
            log.log(Level.INFO,
                    "Volume ADC at: " + this.volumeAddress + " - " + offset);
            String temp =
                    LaunchControl.getInstance().readOWFSPath(this.volumeAddress + "/volt." + offset);

            // Check to make sure we can read OK
            if (temp.equals("")) {
                log.severe(
                        "Couldn't read the Volume from " + this.volumeAddress
                                + "/volt." + offset);
                return false;
            } else {
                log.log(Level.INFO, "Volume reads " + temp);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE,
                    "IOException when access the ADC over 1wire", e);
            return false;
        } catch (OwfsException e) {
            log.log(Level.SEVERE,
                    "OWFSException when access the ADC over 1wire", e);
            return false;
        }

        return true;
    }

    /**
     * Setup volumes for AIN pins.
     *
     * @param analogPin The AIN pin number
     * @param unit      The volume unit
     * @return True is setup OK
     * @throws InvalidGPIOException If the Pin cannot be setup
     */
    boolean setupVolumes(final int analogPin, final String unit)
            throws InvalidGPIOException {
        // start a volume measurement at the same time
        if (unit == null
                || (!unit.equals(VolumeUnits.LITRES)
                && !unit.equals(VolumeUnits.UK_GALLONS)
                && !unit.equals(VolumeUnits.US_GALLONS))) {
            return false;
        }

        this.volumeMeasurement = true;
        this.volumeUnit = unit;

        try {
            this.volumePin = new InPin(analogPin, Direction.ANALOGUE);
        } catch (InvalidGPIOException e) {
            this.volumeMeasurement = false;
            log.warning("Invalid Analog GPIO specified " + analogPin);
            throw (e);
        }

        setupVolume();

        if (this.volumeConstant.compareTo(BigDecimal.ZERO) >= 0
                || this.volumeMultiplier.compareTo(BigDecimal.ZERO) >= 0) {
            this.volumeMeasurement = false;
        }

        return this.volumeMeasurement;

    }

    /**
     * Calculate volume reading maths.
     */
    private void setupVolume() {
        if (this.volumeBase == null) {
            return;
        }

        // Calculate the values of b*value + c = volume
        // get the value of c
        this.volumeMultiplier = new BigDecimal(0);
        this.volumeConstant = new BigDecimal(0);

        // for the rest of the values
        Iterator<Entry<BigDecimal, BigDecimal>> it = this.volumeBase.entrySet().iterator();
        Entry<BigDecimal, BigDecimal> prevPair = null;

        while (it.hasNext()) {
            Entry<BigDecimal, BigDecimal> pairs = it.next();
            if (prevPair != null) {
                // diff the pair value and dive by the diff of the key
                BigDecimal keyDiff = pairs.getKey().subtract(prevPair.getKey());
                BigDecimal valueDiff =
                        pairs.getValue().subtract(prevPair.getValue());
                BigDecimal newMultiplier = MathUtil.divide(valueDiff, keyDiff);
                BigDecimal newConstant =
                        pairs.getValue().subtract(valueDiff.multiply(keyDiff));

                if (this.volumeMultiplier.compareTo(BigDecimal.ZERO) != 0) {
                    if (newMultiplier.equals(this.volumeMultiplier)) {
                        log.info(
                                "The newMultiplier isn't the same as the old one,"
                                        + " if this is a big difference, be careful!"
                                        + " You may need a quadratic!");
                        log.info("New: " + newMultiplier
                                + ". Old: " + this.volumeMultiplier);
                    }
                } else {
                    this.volumeMultiplier = newMultiplier;
                }

                if (this.volumeConstant.compareTo(BigDecimal.ZERO) != 0) {
                    if (newConstant.equals(this.volumeConstant)) {
                        log.info("The new constant "
                                + "isn't the same as the old one, if this is a big"
                                + " difference, be careful!"
                                + " You may need a quadratic!");
                        log.info("New: " + newConstant
                                + ". Old: " + this.volumeConstant);
                    }
                } else {
                    this.volumeConstant = newConstant;
                }
            }
            prevPair = pairs;
        }

        // we should be done now
    }


    /**
     * @return The latest volume reading
     */
    public BigDecimal updateVolume() {
        try {
            BigDecimal pinValue;
            if (this.volumeAIN != -1) {
                pinValue = new BigDecimal(this.volumePin.readValue());
            } else if (this.volumeAddress != null && this.volumeOffset != null) {
                try {
                    pinValue = new BigDecimal(
                            LaunchControl.getInstance().readOWFSPath(
                                    this.volumeAddress + "/volt." + this.volumeOffset));
                    if (this.stopVolumelogging) {
                        log.log(Level.SEVERE,
                                "Recovered volume level reading for " + this.name);
                        this.stopVolumelogging = false;
                    }
                } catch (Exception e) {
                    if (!this.stopVolumelogging) {
                        log.log(Level.SEVERE,
                                "Could not update the volume reading from OWFS");
                        this.stopVolumelogging = true;
                    }
                    log.info("Reconnecting OWFS");
                    LaunchControl.getInstance().setupOWFS();

                    return BigDecimal.ZERO;
                }

            } else {
                return BigDecimal.ZERO;
            }

            // Are we outside of the known range?
            BigDecimal curKey = null, prevKey = null;
            BigDecimal curValue = null, prevValue = null;
            SortedSet<BigDecimal> keys = null;
            try {
                keys = Collections.synchronizedSortedSet(
                        new TreeSet<>(this.volumeBase.keySet()));
            } catch (NullPointerException npe) {
                // No VolumeBase setup, so we're probably calibrating
                return pinValue;
            } catch (Exception e) {
                log.log(Level.SEVERE,
                        "Uncaught exception when creating the volume set", e);
                System.exit(-1);
            }

            BigDecimal tVolume = null;

            try {
                for (BigDecimal key : keys) {
                    if (prevKey == null) {
                        prevKey = key;
                        prevValue = this.volumeBase.get(key);
                        continue;
                    } else if (curKey != null) {
                        prevKey = curKey;
                        prevValue = curValue;
                    }

                    curKey = key;
                    curValue = this.volumeBase.get(key);

                    if (pinValue.compareTo(prevValue) >= 0
                            && pinValue.compareTo(curValue) <= 0) {
                        // We have a set encompassing the values!
                        // assume it's linear
                        BigDecimal volRange = curKey.subtract(prevKey);
                        BigDecimal readingRange = curValue.subtract(prevValue);
                        BigDecimal ratio = MathUtil.divide(pinValue.subtract(prevValue), readingRange);
                        BigDecimal volDiff = ratio.multiply(volRange);
                        tVolume = volDiff.add(prevKey);
                    }

                }

                if (tVolume == null && curKey != null) {
                    // Try to extrapolate
                    BigDecimal volRange = curKey.subtract(prevKey);
                    BigDecimal readingRange = curValue.subtract(prevValue);
                    BigDecimal ratio = MathUtil.divide(pinValue.subtract(prevValue), readingRange);
                    BigDecimal volDiff = ratio.multiply(volRange);
                    tVolume = volDiff.add(prevKey);
                }

            } catch (NoSuchElementException e) {
                // no more elements
                log.info("Finished reading Volume Elements");
            }

            if (tVolume == null) {
                // try to assume the value
                this.currentVolume = pinValue.subtract(this.volumeConstant)
                        .multiply(this.volumeMultiplier);
            } else {
                this.currentVolume = tVolume;
            }

            this.currentVolume = this.currentVolume.multiply(this.gravity);

            return pinValue;
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    /**
     * @param unit Unit to set the volume units to.
     */
    @SuppressWarnings("unused")
    private void setVolumeUnit(final String unit) {
        this.volumeUnit = unit;
    }

    /**
     * Append a volume measurement to the current list of calibrated values.
     *
     * @param volume Volume measurement to record.
     * @return True if added OK.
     */
    boolean addVolumeMeasurement(final BigDecimal volume) {
        // record 10 readings and average it
        BigDecimal maxReads = BigDecimal.TEN;
        BigDecimal total = new BigDecimal(0);
        for (int i = 0; i < maxReads.intValue(); i++) {
            try {
                try {
                    if (this.volumePin != null) {
                        total = total.add(new BigDecimal(
                                this.volumePin.readValue()));
                    } else {
                        try {
                            total = total.add(new BigDecimal(
                                    LaunchControl.getInstance().readOWFSPath(
                                            this.volumeAddress + "/volt." + this.volumeOffset)));
                        } catch (OwfsException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (RuntimeException re) {
                    re.printStackTrace();
                    return false;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } catch (NumberFormatException e) {
                log.warning("Bad Analog input value!");
                return false;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();

            }

        }

        // read in ten values
        BigDecimal avgValue = MathUtil.divide(total, maxReads);
        log.info("Read " + avgValue + " for "
                + volume + " " + this.volumeUnit);

        this.addVolumeMeasurement(volume, avgValue);
        System.out.println(this.name + ": Added volume data point " + volume);
        return true;
    }

    /**
     * Add a volume measurement at a specific key.
     *
     * @param key   The key to overwrite/set
     * @param value The value to overwrite/set
     */
    private void addVolumeMeasurement(
            final BigDecimal key, final BigDecimal value) {
        log.info("Adding " + key + " with value " + value);
        if (this.volumeBase == null) {
            this.volumeBase = new ConcurrentHashMap<>();
        }
        this.volumeBase.put(key, value);
    }

    /**
     * @return The current volume measurement,
     * -1.0 is there is no measurement enabled.
     */
    public BigDecimal getVolume() {
        if (this.volumeMeasurement) {
            return this.currentVolume;
        }

        return BigDecimal.ONE.negate();
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
    public String getVolumeAIN() {
        if (this.volumeAIN == -1) {
            return "";
        }
        return Integer.toString(this.volumeAIN);
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
    public ConcurrentHashMap<BigDecimal, BigDecimal> getVolumeBase() {
        return this.volumeBase;
    }

    /**
     * Check to see if this object has a valid volume input.
     *
     * @return true if there's a valid volume input on this class
     */
    boolean hasVolume() {
        return (this.volumeAddress != null && !this.volumeAddress.equals("")
                && this.volumeOffset != null && !this.volumeOffset.equals("")) || (this.volumeAIN != -1);
    }

    public void shutdown() {
        // Graceful shutdown.
        this.keepAlive = false;
        log.warning(this.getName() + " is shutting down.");
        Thread.currentThread().interrupt();
    }

    public boolean isStarted() {
        return this.isStarted;
    }

    public boolean isRunning() {
        return this.keepAlive;
    }

    public void started() {
        this.isStarted = true;
    }

    public void setCalibration(String calibration) {
        // Lock the calibration temp
        calibration = calibration.replace(",", ".");
        Matcher tempMatcher = getTempRegexp().matcher(calibration);

        if (tempMatcher.find()) {
            // We have matched against the TEMP_REGEXP
            String number = tempMatcher.group(1);
            if (number == null) {
                number = "+";
            }
            // Get the integer
            if (tempMatcher.group(2) != null) {
                number += tempMatcher.group(2);
            }
            // Do we have a decimal?
            if (tempMatcher.group(3) != null) {
                number += tempMatcher.group(3);
            }
            // Create the temp
            BigDecimal temperature = new BigDecimal(number);
            this.calibration = temperature.setScale(2, BigDecimal.ROUND_DOWN);
        } else {
            log.severe(calibration + " doesn't match "
                    + getTempRegexp().pattern());
        }
    }

    public String getCalibration() {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(this.calibration);
    }

    public boolean isSetup() {
        return !this.getName().equals(this.getProbe());
    }

    public void toggleVisibility() {
        if (this.hidden) {
            this.show();
        } else {
            this.hide();
        }
    }

    public void hide() {
        this.hidden = true;
    }

    public void show() {
        this.hidden = false;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setGravity(BigDecimal newGravity) {
        this.gravity = newGravity;
    }

    public BigDecimal getGravity() {
        return this.gravity;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Get the current TriggerControl object, create a new one if it doesn't
     * exist.
     *
     * @return the TriggerControl.
     */
    public TriggerControl getTriggerControl() {
        if (this.triggerControl == null) {
            this.triggerControl = new TriggerControl();
            this.triggerControl.setOutputControl(this.getName());
        }
        return this.triggerControl;
    }

    /**
     * @return The position of this temp probe in the list.
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * Set the position of this temp probe.
     *
     * @param newPos The new position.
     */
    public void setPosition(final int newPos) {
        this.position = newPos;
    }

    @Override
    public int compareTo(@Nonnull final Temp o) {
        if (o.getPosition() == this.position) {
            return o.getName().compareTo(this.name);
        }
        if (this.position == -1) {
            return 1;
        }
        return this.position - o.getPosition();
    }

    public BigDecimal convertF(BigDecimal temp) {
        if (this.scale.equals("C")) {
            return cToF(temp);
        }

        return temp;
    }


    public String getI2CDevNumberString() {
        if (this.i2cDevice == null) {
            return "";
        }
        return this.i2cDevice.getDevNumberString();
    }

    public String getI2CDevAddressString() {
        if (this.i2cDevice == null) {
            return "";
        }
        return Integer.toString(this.i2cDevice.getAddress());
    }

    public String geti2cChannel() {
        if (this.i2cChannel == -1) {
            return "";
        }
        return Integer.toString(this.i2cChannel);
    }

    public String getI2CDevType() {
        if (this.i2cDevice == null) {
            return "";
        }
        return this.i2cDevice.getDevName();
    }

    boolean setupVolumeI2C(String i2c_device, String i2c_address, String i2c_channel, String i2c_type, String units) {
        return setupVolumeI2C(LaunchControl.getInstance().getI2CDevice(i2c_device, i2c_address, i2c_type), i2c_channel, units);
    }

    private boolean setupVolumeI2C(I2CDevice i2c_device, String i2c_channel, String volumeUnits) {
        this.i2cDevice = i2c_device;
        this.i2cChannel = Integer.parseInt(i2c_channel);
        this.setVolumeUnit(volumeUnits);
        return (this.i2cDevice != null);
    }

    public Pattern getTempRegexp() {
        if (this.tempRegexp == null) {
            this.tempRegexp = Pattern.compile("^(-?)([0-9]+)(.[0-9]{1,2})?$");
        }
        return this.tempRegexp;
    }

}
