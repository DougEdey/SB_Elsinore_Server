package com.sb.elsinore.devices;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.TriggerControl;
import com.sb.elsinore.VolumeUnits;
import com.sb.elsinore.interfaces.TemperatureInterface;
import com.sb.elsinore.models.TemperatureModel;
import com.sb.util.MathUtil;
import jGPIO.GPIO.Direction;
import jGPIO.InPin;
import jGPIO.InvalidGPIOException;
import org.owfs.jowfsclient.OwfsException;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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
public class TempProbe implements TemperatureInterface {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TempProbe.class);
    /**
     * Hold the current error string.
     */
    public String currentError = null;

    I2CDevice i2cDevice = null;
    /**
     * The current temp.
     */
    private BigDecimal currentTemp = new BigDecimal(0),
            currentVolume = new BigDecimal(0),
            gravity = new BigDecimal(1.000),
            volumeConstant = new BigDecimal(0),
            volumeMultiplier = new BigDecimal(0.0);
    /**
     * The current timestamp.
     */
    private Long currentTime = 0L;

    /**
     * The baselist of volume measurements.
     */
    private ConcurrentHashMap<BigDecimal, BigDecimal> volumeBase = null;
    /**
     * The input pin to read.
     */
    private InPin volumePin = null;
    private TriggerControl triggerControl = null;
    private TemperatureInterface temperatureInterface;
    private boolean stopVolumeLogging = false;

    private TempProbe() {
    }

    public TempProbe(TemperatureInterface temperatureInterface) {
        this.temperatureInterface = temperatureInterface;
    }

    /**
     * Standard constructor.
     *
     * @param name   The probe name or input name.
     *               Use "system" to create a system temperature probe
     * @param device The address of this temperature probe.
     */
    public TempProbe(String name, String device) {
        log.info("Adding " + device);
        this.temperatureInterface = new TemperatureModel(name, device);
        LaunchControl.getInstance().addTemp(this);
    }


    @Override
    public Long getId() {
        return this.temperatureInterface.getId();
    }

    @Override
    public String getName() {
        return this.temperatureInterface.getName();
    }

    @Override
    public void setName(String name) {
        this.temperatureInterface.setName(name);
    }

    @Override
    public String getDevice() {
        return this.temperatureInterface.getDevice();
    }

    @Override
    public void setDevice(String device) {
        this.temperatureInterface.setDevice(device);
    }

    @Override
    public String getScale() {
        return this.temperatureInterface.getScale();
    }

    @Override
    public void setScale(String scale) {
        this.temperatureInterface.setScale(scale);
        if ("F".equalsIgnoreCase(scale)) {
            setCalibration(getCalibration().multiply(new BigDecimal(1.8)));
            this.temperatureInterface.setScale(scale);
        }

        if ("C".equalsIgnoreCase(scale)) {
            this.setCalibration(getCalibration().divide(new BigDecimal(1.8), new MathContext(2)));
            this.temperatureInterface.setScale(scale);
        }
    }

    public String getCutoff() {
        return this.temperatureInterface.getCutoffTemp().toPlainString();
    }

    /**
     * Setup the volume reading.
     *
     * @param address One Wire device address.
     * @param offset  Device offset input.
     * @param unit    The volume unit to read as.
     */
    boolean setupVolume(String address, String offset, String unit) {
        // start a volume measurement at the same time
        if (unit == null
                || (!unit.equals(VolumeUnits.LITRES)
                && !unit.equals(VolumeUnits.UK_GALLONS)
                && !unit.equals(VolumeUnits.US_GALLONS))) {
            return false;
        }

        String volumeAddress = address.replace("-", ".");


        try {
            log.info("Volume ADC at: {} - {}", volumeAddress, offset);
            String owfsPath = volumeAddress + "/volt." + offset;
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

        setupVolumes(volumeAddress, offset, unit);
        return true;
    }

    public void setupVolumes(String volumeAddress, String offset, String unit) {
        setVolumeAddress(volumeAddress);
        setVolumeOffset(offset);
        setVolumeUnit(unit);
    }

    @Override
    public String getVolumeUnit() {
        return this.temperatureInterface.getVolumeUnit();
    }

    @Override
    public void setVolumeUnit(String unit) {
        this.temperatureInterface.setVolumeUnit(unit);
    }

    @Override
    public String getVolumeAIN() {
        return this.temperatureInterface.getVolumeAIN();
    }

    @Override
    public void setVolumeAIN(int volumeAIN) {
        this.temperatureInterface.setVolumeAIN(volumeAIN);
    }

    @Override
    public String getVolumeAddress() {
        return this.temperatureInterface.getVolumeAddress();
    }

    @Override
    public void setVolumeAddress(String volumeAddress) {
        this.temperatureInterface.setVolumeAddress(volumeAddress);
    }

    @Override
    public String getVolumeOffset() {
        return this.temperatureInterface.getVolumeOffset();
    }

    @Override
    public void setVolumeOffset(String volumeOffset) {
        this.temperatureInterface.setVolumeOffset(volumeOffset);
    }

    @Override
    public boolean hasVolume() {
        return this.temperatureInterface.hasVolume();
    }

    @Override
    public BigDecimal getCalibration() {
        return this.temperatureInterface.getCalibration();
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

            setCalibration(temperature.setScale(2, RoundingMode.HALF_DOWN));
        } else {
            log.error("{} doesn't match {}", calibration, getTempRegexp().pattern());
        }
    }

    @Override
    public void setCalibration(BigDecimal calibration) {
        this.temperatureInterface.setCalibration(calibration);
    }

    @Override
    public void hide() {
        this.temperatureInterface.hide();
    }

    @Override
    public void show() {
        this.temperatureInterface.show();
    }

    @Override
    public boolean isHidden() {
        return this.temperatureInterface.isHidden();
    }

    @Override
    public int getPosition() {
        return this.temperatureInterface.getPosition();
    }

    @Override
    public void setPosition(int position) {
        this.temperatureInterface.setPosition(position);
    }

    @Override
    public String getI2CNumber() {
        return this.temperatureInterface.getI2CNumber();
    }

    @Override
    public void setI2CNumber(String number) {

    }

    @Override
    public String getI2CAddress() {
        return this.temperatureInterface.getI2CAddress();
    }

    @Override
    public void setI2CAddress(String address) {

    }

    @Override
    public String getI2CChannel() {
        return this.temperatureInterface.getI2CChannel();
    }

    @Override
    public void setI2CChannel(String channel) {
        this.temperatureInterface.setI2CChannel(channel);
    }

    @Override
    public String getI2CDevType() {
        return this.temperatureInterface.getI2CDevType();
    }

    @Override
    public void setI2CDevType(String type) {
        this.temperatureInterface.setI2CDevType(type);
    }

    public boolean isCutoffEnabled() {
        return getCutoffTemp() != null;
    }

    @Override
    public BigDecimal getCutoffTemp() {
        return this.temperatureInterface.getCutoffTemp();
    }

    @Override
    public void setCutoffTemp(BigDecimal cutoffTemp) {
        this.temperatureInterface.setCutoffTemp(cutoffTemp);
    }

    /******
     * Method to take a cutoff value, parse it to the correct scale
     *  and then update the cutoffTemp.
     * @param cutoffInput String describing the temperature
     */
    void setCutoffTemp(String cutoffInput) {
        if (isNullOrEmpty(cutoffInput)) {
            setCutoffTemp((BigDecimal) null);
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
            setCutoffTemp(new BigDecimal(number));
        } else {
            log.error("{} doesn't match {}", getCutoffTemp(), getTempRegexp().pattern());
        }
    }

    /**
     * Setup volumes for AIN pins.
     *
     * @param analogPin The AIN pin number
     * @param unit      The volume unit
     * @return True is setup OK
     * @throws InvalidGPIOException If the Pin cannot be setup
     */
    boolean setupVolumes(int analogPin, String unit) throws InvalidGPIOException {
        // start a volume measurement at the same time
        if (unit == null
                || (!unit.equals(VolumeUnits.LITRES)
                && !unit.equals(VolumeUnits.UK_GALLONS)
                && !unit.equals(VolumeUnits.US_GALLONS))) {
            return false;
        }

        try {
            this.volumePin = new InPin(analogPin, Direction.ANALOGUE);
        } catch (InvalidGPIOException e) {
            log.warn("Invalid Analog GPIO specified {}", analogPin);
            throw (e);
        }

        setupVolume();

        if (this.volumeConstant.compareTo(BigDecimal.ZERO) >= 0
                || this.volumeMultiplier.compareTo(BigDecimal.ZERO) >= 0) {
            setVolumeMeasurementEnabled(false);
            return false;
        }


        setVolumeAIN(analogPin);
        setVolumeUnit(unit);
        return getVolumeMeasurementEnabled();

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
            if (this.volumePin != null) {
                pinValue = new BigDecimal(this.volumePin.readValue());
            } else if (getVolumeAddress() != null && getVolumeOffset() != null) {
                try {
                    pinValue = new BigDecimal(
                            LaunchControl.getInstance().readOWFSPath(
                                    getVolumeAddress() + "/volt." + getVolumeOffset()));
                    if (this.stopVolumeLogging) {
                        log.error("Recovered volume level reading for {}", getName());
                        this.stopVolumeLogging = false;
                    }
                } catch (Exception e) {
                    if (!this.stopVolumeLogging) {
                        log.error("Could not update the volume reading from OWFS");
                        this.stopVolumeLogging = true;
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
                                            getVolumeAddress() + "/volt." + getVolumeOffset())));
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
                + volume + " " + getVolumeUnit());

        this.addVolumeMeasurement(volume, avgValue);
        System.out.println(getName() + ": Added volume data point " + volume);
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
     * @return The current volume base map
     */
    public ConcurrentHashMap<BigDecimal, BigDecimal> getVolumeBase() {
        return this.volumeBase;
    }

    public boolean isSetup() {
        return true;
    }

    public void toggleVisibility() {
        if (isHidden()) {
            show();
        } else {
            hide();
        }
    }

    public BigDecimal getGravity() {
        return this.gravity;
    }

    public void setGravity(BigDecimal newGravity) {
        this.gravity = newGravity;
    }

    @Override
    public String toString() {
        return getName();
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

    boolean setupVolumeI2C(String i2c_device, String i2c_address, String i2c_channel, String i2c_type, String units) {
        return setupVolumeI2C(LaunchControl.getInstance().getI2CDevice(i2c_device, i2c_address, i2c_type), i2c_channel, units);
    }

    private boolean setupVolumeI2C(I2CDevice i2c_device, String i2c_channel, String volumeUnits) {
        this.i2cDevice = i2c_device;
        setI2CChannel(i2c_channel);
        setVolumeUnit(volumeUnits);
        return (this.i2cDevice != null);
    }

    /**
     * @return The current volume measurement,
     * -1.0 is there is no measurement enabled.
     */
    public BigDecimal getVolume() {
        if (getVolumeMeasurementEnabled()) {
            return this.currentVolume;
        }

        return BigDecimal.ONE.negate();
    }

    @Override
    public boolean getVolumeMeasurementEnabled() {
        return this.temperatureInterface.getVolumeMeasurementEnabled();
    }

    @Override
    public void setVolumeMeasurementEnabled(boolean enabled) {
        this.temperatureInterface.setVolumeMeasurementEnabled(enabled);
    }

    @Override
    public TemperatureModel getModel() {
        if (this.temperatureInterface instanceof TemperatureModel) {
            return (TemperatureModel) this.temperatureInterface;
        }
        return null;
    }
}
