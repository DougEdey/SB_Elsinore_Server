package com.sb.elsinore.inputs;

import jGPIO.GPIO.Direction;
import jGPIO.InPin;
import jGPIO.InvalidGPIOException;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.json.simple.JSONObject;

import com.sb.elsinore.BrewServer;
import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.NanoHTTPD.Response;
import com.sb.elsinore.annotations.PhSensorType;
import com.sb.elsinore.annotations.UrlEndpoint;
import com.sb.util.MathUtil;

public class PhSensor {

    private int ainPin = -1;
    private String dsAddress = "";
    private String dsOffset = "";
    private String model = "";
    private String name = "pH Sensor";
    private BigDecimal phReading = new BigDecimal(0);
    private BigDecimal offset = new BigDecimal(0);
    private InPin ainGPIO = null;
    private boolean stopLogging = false;

    /**
     * Create a blank pH Sensor.
     */
    public PhSensor() {
    }

    /**
     * Setup a phSensor using an analog pin.
     * @param newPin The analog pin.
     * @throws InvalidGPIOException If the pin is invalid.
     */
    public PhSensor(final int newPin) throws InvalidGPIOException {
        this.ainPin = newPin;
        ainGPIO = new InPin(this.ainPin, Direction.ANALOGUE);
    }

    /**
     * Override the ToString.
     * @return The name of the PhSensor
     */
    public final String toString() {
        return this.name;
    }
    /**
     * Setup a pH Sensor using a DS2450 based Analog input.
     * @param address The DS2450 Address
     * @param newOffset The DS2450 Offset
     */
    public PhSensor(final String address, final String newOffset) {
        this.dsAddress = address;
        this.dsOffset = newOffset;
    }

    /**
     * Set the pH Sensor Type.
     * @param type The type of the sensor.
     */
    public final void setModel(final String type) {
        this.model = type;
    }

    public final void setAinPin(int newPin) {
        this.ainPin = newPin;
    }

    /**
     * Set the DS2450 Address.
     * @param address The address of the DS2450.
     */
    public final void setDsAddress(final String address) {
        this.dsAddress = address;
    }

    /**
     * Set the DS2450 Offset.
     * @param newoffset The Offset of the DS2450.
     */
    public final void setDsOffset(final String newoffset) {
        this.dsOffset = newoffset;
    }

    /**
     * Calibrate the Probe and calculate the offset.
     * @param targetRead The pH of the solution being measured.
     */
    public final void calibrate(final BigDecimal targetRead) {
        BigDecimal tolerance = new BigDecimal(0.3);
        BigDecimal currentValue = this.calcPhValue();
        if (currentValue.subtract(targetRead).plus()
                .compareTo(tolerance) > 0) {
            // We're outside of the tolerance. So Set the offset.
            offset = targetRead.subtract(currentValue);
        }

        // Verify we're good
        currentValue = this.calcPhValue();
        if (currentValue.subtract(targetRead).plus()
                .compareTo(tolerance) > 0) {
            LaunchControl.setMessage("Failed to calibrate. Difference is: "
                + currentValue.subtract(targetRead));
        }
    }

    /**
     * Get the current status in JSON Form.
     * @return The current status.
     */
    public final JSONObject getJsonStatus() {
        JSONObject retVal = new JSONObject();
        retVal.put("phReading", phReading);
        retVal.put("name", name);
        retVal.put("deviceType", model);
        return retVal;
    }

    /**
     * Update the current reading.
     * @return the current Analog Value.
     */
    public final BigDecimal updateReading() {
        BigDecimal pinValue = new BigDecimal(0);

        if (ainGPIO != null) {
            try {
                pinValue = new BigDecimal(ainGPIO.readValue());
                if (this.stopLogging) {
                    BrewServer.LOG.log(Level.SEVERE,
                        "Recovered pH level reading for " + this.name);
                    this.stopLogging = false;
                }
            } catch (Exception e) {
                if (!this.stopLogging) {
                    BrewServer.LOG.log(Level.SEVERE,
                        "Could not update the pH reading from Analogue", e);
                    this.stopLogging = true;
                }
                BrewServer.LOG.info("Reconnecting OWFS");
                LaunchControl.setupOWFS();
            }
        } else if (dsAddress != null && dsAddress.length() > 0
                && dsOffset != null && dsOffset.length() > 0) {
            try {
                pinValue = new BigDecimal(
                    LaunchControl.readOWFSPath(
                        dsAddress + "/volt." + dsOffset));
                if (this.stopLogging) {
                    BrewServer.LOG.log(Level.SEVERE,
                        "Recovered pH level reading for " + this.name);
                    this.stopLogging = false;
                }
            } catch (Exception e) {
                if (!this.stopLogging) {
                    BrewServer.LOG.log(Level.SEVERE,
                        "Could not update the pH reading from OWFS", e);
                    this.stopLogging = true;
                }
                BrewServer.LOG.info("Reconnecting OWFS");
                LaunchControl.setupOWFS();
            }
        }

        return pinValue;
    }

    /**
     * Get the current name.
     * @return The name of this Sensor
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Get the current pin.
     * @return "" if no pin set.
     */
    public final String getAIN() {
        if (this.ainPin == -1) {
            return "";
        }
        return Integer.toString(this.ainPin);
    }

    /**
     * Return the current DS2450 Offset.
     * @return DS2450 Offset
     */
    public final String getDsOffset() {
        return this.dsOffset;
    }

    /**
     * Return the current DS2450 Address.
     * @return DS2450 Address
     */
    public final String getDsAddress() {
        return this.dsAddress;
    }

    /**
     * Get the current pH Sensor Model.
     * @return The Model.
     */
    public final String getModel() {
        return this.model;
    }
    /**
     * Calculate the current PH Value based off the current pH Sensor type.
     * @param reading The current Analog read value.
     * @return The value of the pH Probe.
     */
    public final BigDecimal calcPhValue() {
        BigDecimal value = null;

        for (java.lang.reflect.Method m
                : PhSensor.class.getDeclaredMethods()) {
           PhSensorType calcMethod =
                   (PhSensorType) m.getAnnotation(PhSensorType.class);
           if (calcMethod != null) {
               if (calcMethod.model().equalsIgnoreCase(this.model)) {
                   try {
                       value = (BigDecimal) m.invoke(this);
                   } catch (IllegalAccessException e) {
                       //do nothing;
                   } catch (InvocationTargetException o) {
                       // do nothing
                   }
               }
           }
        }

        return value;
    }

    /**
     * Get the current list of available sensor types.
     * @return The List of available sensor types.
     */
    public final List<String> getAvailableTypes() {
        List<String> typeList = new ArrayList<String>();
        for (java.lang.reflect.Method m
                : PhSensor.class.getDeclaredMethods()) {
           PhSensorType calcMethod =
                   (PhSensorType) m.getAnnotation(PhSensorType.class);
           if (calcMethod != null) {
               typeList.add(calcMethod.model());
           }
        }

        return typeList;
    }

    /**
     * Calculate the current pH Value for the SEN0161 pH Sensor.
     * @return The current pH value.
     */
    @PhSensorType(model = "SEN0161")
    public final BigDecimal calcSEN0161() {
        BigDecimal readValue = this.getAverage(3);
        BigDecimal t = null;
        t = MathUtil.multiply(readValue, (5.0 / 1024));
        return MathUtil.multiply(t, 3.5).add(offset);
    }

    /**
     * Get the average value of the analog input.
     * @param maxRead The number of samples to take.
     * @return The current average.
     */
    public final BigDecimal getAverage(final int maxRead) {
        BigDecimal readValue = new BigDecimal(0);
        BigDecimal t = null;
        for (int i = 0; i <= maxRead; i++) {
            t = this.updateReading();
            if (t.compareTo(BigDecimal.ZERO) == 0) {
                i--;
            } else {
                readValue.add(t);
            }
        }

        return readValue.divide(new BigDecimal(maxRead));
    }

    /**
     * Set the name of this pH Sensor.
     * @param newName The new name.
     */
    public final void setName(final String newName) {
        this.name = newName;
    }
}
