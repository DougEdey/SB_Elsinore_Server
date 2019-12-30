package com.sb.elsinore.inputs;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.OWFSController;
import com.sb.elsinore.annotations.PhSensorType;
import com.sb.elsinore.devices.I2CDevice;
import com.sb.util.MathUtil;
import main.java.jGPIO.GPIO.Direction;
import main.java.jGPIO.InPin;
import main.java.jGPIO.InvalidGPIOException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.Transient;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class PhSensor {

    public static final String DS_ADDRESS = "dsAddress";
    public static final String DS_OFFSET = "dsOffset";
    public static final String AIN_PIN = "ainPin";
    public static final String OFFSET = "offset";
    public static final String MODEL = "model";
    private static final java.math.BigDecimal BIGDEC_THOUSAND = new BigDecimal(1000);
    public I2CDevice i2cDevice = null;
    public int i2cChannel = -1;
    private Logger logger = LoggerFactory.getLogger(PhSensor.class);
    private int ainPin = -1;
    private String dsAddress = "";
    private String dsOffset = "";
    private String model = "";
    private String name = "pH Sensor";
    private BigDecimal phReading = new BigDecimal(0);
    private BigDecimal offset = new BigDecimal(0);
    @Transient
    private InPin ainGPIO = null;
    private boolean stopLogging = false;
    @Autowired
    private OWFSController owfsController;

    /**
     * Create a blank pH Sensor.
     */
    public PhSensor() {
    }

    /**
     * Setup a phSensor using an analog pin.
     *
     * @param newPin The analog pin.
     * @throws InvalidGPIOException If the pin is invalid.
     */
    public PhSensor(final int newPin) throws InvalidGPIOException {
        this.ainPin = newPin;
        this.ainGPIO = new InPin(this.ainPin, Direction.ANALOGUE);
    }

    /**
     * Setup a pH Sensor using a DS2450 based Analog input.
     *
     * @param address   The DS2450 Address
     * @param newOffset The DS2450 Offset
     */
    public PhSensor(final String address, final String newOffset) {
        this.dsAddress = address;
        this.dsOffset = newOffset;
    }

    @Autowired
    public void setOWFSController(OWFSController owfsController) {
        this.owfsController = owfsController;
    }

    /**
     * Override the ToString.
     *
     * @return The name of the PhSensor
     */
    @Override
    public final String toString() {
        return this.name;
    }

    /**
     * Set the Analog Input pin.
     *
     * @param newPin The new input pin.
     */
    public final void setAinPin(final String newPin) {
        if (newPin == null || newPin.length() == 0) {
            this.ainPin = -1;
            return;
        }
        try {
            this.ainPin = Integer.parseInt(newPin);
        } catch (NumberFormatException nfe) {
            this.ainPin = -1;
        }
    }

    /**
     * Set the Analog Input pin.
     *
     * @param newPin The new input pin.
     */
    public final void setAinPin(final int newPin) {
        this.ainPin = newPin;
    }

    /**
     * Calibrate the Probe and calculate the offset.
     *
     * @param targetRead The pH of the solution being measured.
     */
    public final void calibrate(final BigDecimal targetRead) {
        BigDecimal tolerance = new BigDecimal(0.3);
        this.offset = BigDecimal.ZERO;
        BigDecimal currentValue = this.calcPhValue();
        if (currentValue.subtract(targetRead).abs()
                .compareTo(tolerance) > 0) {
            // We're outside of the tolerance. So Set the offset.
            this.offset = targetRead.subtract(currentValue);
        }

        // Verify we're good
        currentValue = this.calcPhValue();
        if (currentValue.subtract(targetRead).plus()
                .compareTo(tolerance) > 0) {
            //LaunchControl.setMessage(String.format("Failed to calibrate. Difference is: %s", currentValue.subtract(targetRead)));
        }
    }

    /**
     * Get the current status in JSON Form.
     *
     * @return The current status.
     */
    public final JSONObject getJsonStatus() {
        JSONObject retVal = new JSONObject();
        retVal.put("phReading", this.phReading);
        retVal.put("name", this.name);
        retVal.put("deviceType", this.model);
        retVal.put("offset", getOffset());
        return retVal;
    }

    /**
     * Update the current reading.
     *
     * @return the current Analog Value.
     */
    public final BigDecimal updateReading() {
        BigDecimal pinValue = new BigDecimal(0);
        LaunchControl lc = null;
        if (this.ainGPIO != null) {
            try {
                pinValue = new BigDecimal(this.ainGPIO.readValue());
                if (this.stopLogging) {
                    this.logger.warn("Recovered pH level reading for {}", this.name);
                    this.stopLogging = false;
                }
            } catch (Exception e) {
                if (!this.stopLogging) {
                    this.logger.warn("Could not update the pH reading from Analogue", e);
                    this.stopLogging = true;
                }
                this.logger.info("Reconnecting OWFS");
                this.owfsController.setupOWFS();
            }
        } else if (this.dsAddress != null && this.dsAddress.length() > 0
                && this.dsOffset != null && this.dsOffset.length() > 0) {
            try {
                pinValue = new BigDecimal(
                        this.owfsController.readOWFSPath(
                                this.dsAddress + "/volt." + this.dsOffset));
                if (this.stopLogging) {
                    this.logger.warn("Recovered pH level reading for {}", this.name);
                    this.stopLogging = false;
                }
            } catch (Exception e) {
                if (!this.stopLogging) {
                    this.logger.warn("Could not update the pH reading from OWFS", e);
                    this.stopLogging = true;
                }
                this.logger.info("Reconnecting OWFS");
                this.owfsController.setupOWFS();
            }
        } else if (this.i2cDevice != null && this.i2cChannel > -1) {
            this.logger.warn("Reading from {} channel {}", getI2CDevAddressString(), this.i2cChannel);
            pinValue = new BigDecimal(this.i2cDevice.readValue(this.i2cChannel)).divide(BIGDEC_THOUSAND);
        }

        this.logger.warn("Read: {}", pinValue);
        return pinValue;
    }

    /**
     * Get the current name.
     *
     * @return The name of this Sensor
     */
    public final String getName() {
        if (this.name == null) {
            this.name = "<unknown>";
        }
        return this.name.replaceAll(" ", "_");
    }

    /**
     * Set the name of this pH Sensor.
     *
     * @param newName The new name.
     */
    public final void setName(final String newName) {
        this.name = newName;
    }

    /**
     * Get the current pin.
     *
     * @return "" if no pin set.
     */
    public final String getAIN() {
        if (this.ainPin == -1) {
            return "";
        }
        return Integer.toString(this.ainPin);
    }

    /**
     * Get the calibration offset in pH.
     *
     * @return The Calibration offset in pH
     */
    public final BigDecimal getOffset() {
        return this.offset;
    }

    /**
     * Set the calibration offset.
     *
     * @param attribute The Calibration offset
     */
    public final void setOffset(final BigDecimal attribute) {
        this.offset = attribute;
    }

    /**
     * Set the calibration offset.
     *
     * @param attribute The Calibration offset
     */
    public final void setOffset(final String attribute) {
        if (attribute != null && attribute.length() > 0) {
            setOffset(new BigDecimal(attribute));
        }
    }

    /**
     * Return the current DS2450 Offset.
     *
     * @return DS2450 Offset
     */
    public final String getDsOffset() {
        return this.dsOffset;
    }

    /**
     * Set the DS2450 Offset.
     *
     * @param newoffset The Offset of the DS2450.
     */
    public final void setDsOffset(final String newoffset) {
        this.dsOffset = newoffset;
    }

    /**
     * Return the current DS2450 Address.
     *
     * @return DS2450 Address
     */
    public final String getDsAddress() {
        return this.dsAddress;
    }

    /**
     * Set the DS2450 Address.
     *
     * @param address The address of the DS2450.
     */
    public final void setDsAddress(final String address) {
        this.dsAddress = address;
    }

    /**
     * Get the current pH Sensor Model.
     *
     * @return The Model.
     */
    public final String getModel() {
        return this.model;
    }

    /**
     * Set the pH Sensor Type.
     *
     * @param type The type of the sensor.
     */
    public final void setModel(final String type) {
        this.model = type;
    }

    /**
     * Calculate the current PH Value based off the current pH Sensor type.
     *
     * @return The value of the pH Probe.
     */
    public final BigDecimal calcPhValue() {
        BigDecimal value = new BigDecimal(0);
        LaunchControl lc = null;
        for (java.lang.reflect.Method m
                : PhSensor.class.getDeclaredMethods()) {
            PhSensorType calcMethod =
                    m.getAnnotation(PhSensorType.class);
            if (calcMethod != null) {
                if (calcMethod.model().equalsIgnoreCase(this.model)) {
                    try {
                        value = (BigDecimal) m.invoke(this);
                        value = value.setScale(2, RoundingMode.CEILING);
                        if (value.compareTo(BigDecimal.ZERO) > 0) {
                            this.phReading = value;
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return value;
    }

    /**
     * Get the current list of available sensor types.
     *
     * @return The List of available sensor types.
     */
    public final List<String> getAvailableTypes() {
        List<String> typeList = new ArrayList<>();
        for (java.lang.reflect.Method m
                : PhSensor.class.getDeclaredMethods()) {
            PhSensorType calcMethod =
                    m.getAnnotation(PhSensorType.class);
            if (calcMethod != null) {
                typeList.add(calcMethod.model());
            }
        }

        return typeList;
    }

    /**
     * Calculate the current pH Value for the SEN0161 pH Sensor.
     *
     * @return The current pH value.
     */
    @PhSensorType(model = "SEN0161")
    public final BigDecimal calcSEN0161() {
        BigDecimal readValue = this.getAverage(3);
        if (readValue.compareTo(BigDecimal.ZERO) <= 0) {
            return readValue;
        }
        return MathUtil.multiply(readValue, 3.5).add(this.offset);
    }

    /**
     * Get the average value of the analog input.
     *
     * @param maxRead The number of samples to take.
     * @return The current average.
     */
    private final BigDecimal getAverage(final int maxRead) {
        BigDecimal readValue = new BigDecimal(0);
        BigDecimal t;
        int i;
        int badRead = 0;
        for (i = 0; i < maxRead; ) {
            t = this.updateReading();
            if (t.compareTo(BigDecimal.ZERO) > 0) {
                readValue = readValue.add(t);

                i++;
            } else {
                badRead++;
            }
            if (badRead > 5) {
                return new BigDecimal(-1);
            }
        }

        return MathUtil.divide(readValue, new BigDecimal(i));
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

    public String getI2CDevicePath() {
        if (this.i2cDevice == null) {
            return "";
        }
        return this.i2cDevice.getDevicePath();
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
}
