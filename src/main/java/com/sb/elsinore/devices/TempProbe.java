package com.sb.elsinore.devices;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.sb.elsinore.*;
import com.sb.util.MathUtil;
import jGPIO.GPIO.Direction;
import jGPIO.InPin;
import jGPIO.InvalidGPIOException;
import org.owfs.jowfsclient.OwfsException;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sb.common.SBStringUtils.getTempRegexp;

/**
 * TempProbe class is used to monitor temperature on the one wire system.
 *
 * @author Doug Edey
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@DiscriminatorValue("T")
@EntityListeners(TemperatureListeners.class)
public class TempProbe extends Device implements Comparable<TempProbe> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TempProbe.class);

    @Expose
    private boolean hidden = false;
    @Expose
    boolean cutoffEnabled = false;

    @JsonProperty("i2c_device")
    I2CDevice i2cDevice = null;
    @JsonProperty("i2c_channel")
    int i2cChannel = -1;

    @JsonProperty("device")
    @NotNull
    @javax.validation.constraints.Size(min = 1)
    String device = "";

    public TempProbe() {
    }

    public TempProbe(Device device) {
        super(device);

        if (!(device instanceof TempProbe)) {
            return;
        }
        TempProbe other = (TempProbe) device;
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
     */
    public TempProbe(String name, String inProbe) {
        this.device = inProbe;
        log.info("Adding " + this.device);


        setName(name);
        this.probeName = this.device;
        log.info(this.probeName + " added.");
        LaunchControl.getInstance().addTemp(this);
    }

    /**
     * @param name The name to set this TempProbe to.
     */
    public void setName(String name) {
        this.name = name;
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
            log.warn("Disabling cut off temperature");
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
            log.error("{} doesn't match {}", this.cutoffTemp, getTempRegexp().pattern());
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
            cutoffTemp = null,
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
     * @return The temp unit/Scale
     */
    public String getScale() {
        return this.scale;
    }

    /**
     * @param s Value to set the temperature unit to.
     */
    public void setScale(final String s) {
        log.warn("Cut off is: {}", this.cutoffTemp);

        if (s.equalsIgnoreCase("F")) {
            this.calibration = this.calibration.multiply(new BigDecimal(1.8));

            this.scale = s;
        }

        if (s.equalsIgnoreCase("C")) {
            this.calibration = this.calibration.divide(new BigDecimal(1.8), new MathContext(2));
            this.scale = s;
        }
        log.warn("Cut off is now: {}", this.cutoffTemp);
    }

    /**
     * @return The current cutoff temp.
     */
    String getCutoff() {
        return this.cutoffTemp.toPlainString();
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
            log.info("Volume ADC at: {} - {}", this.volumeAddress, offset);
            String owfsPath = this.volumeAddress + "/volt." + offset;
            String temp =
                    LaunchControl.getInstance().readOWFSPath(owfsPath);

            // Check to make sure we can read OK
            if (temp.equals("")) {
                log.error(
                        "Couldn't read the Volume from {}", owfsPath);
                return false;
            } else {
                log.info("Volume reads {}", temp);
            }
        } catch (IOException e) {
            log.error("IOException when access the ADC over 1wire", e);
            return false;
        } catch (OwfsException e) {
            log.error("OWFSException when access the ADC over 1wire", e);
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
            log.warn("Invalid Analog GPIO specified {}", analogPin);
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
                        log.error("Recovered volume level reading for {}", this.name);
                        this.stopVolumelogging = false;
                    }
                } catch (Exception e) {
                    if (!this.stopVolumelogging) {
                        log.error("Could not update the volume reading from OWFS");
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
                log.error("Uncaught exception when creating the volume set", e);
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
                } catch (RuntimeException | IOException re) {
                    re.printStackTrace();
                    return false;
                }
            } catch (NumberFormatException e) {
                log.warn("Bad Analog input value!");
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
            log.error("{} doesn't match {}", calibration, getTempRegexp().pattern());
        }
    }

    public BigDecimal getCalibration() {
        return this.calibration;
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
            this.triggerControl.setOutputControl(getName());
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
    public int compareTo(@Nonnull final TempProbe o) {
        if (o.getPosition() == this.position) {
            return o.getName().compareTo(this.name);
        }
        if (this.position == -1) {
            return 1;
        }
        return this.position - o.getPosition();
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


    public boolean getCutoffEnabled() {
        return this.cutoffTemp != null;
    }

    public BigDecimal getCutoffTemp() {
        return this.cutoffTemp;
    }
}
