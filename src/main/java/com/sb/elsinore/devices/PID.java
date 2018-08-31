package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;
import com.sb.elsinore.models.PIDInterface;
import com.sb.elsinore.models.PIDSettings;
import com.sb.elsinore.models.Temperature;
import com.sb.elsinore.models.TemperatureInterface;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The PIDModel class calculates the Duty cycles and updates the output control.
 *
 * @author Doug Edey
 */
public class PID implements PIDInterface {
    private PIDInterface pidInterface = null;
    private BigDecimal calculatedDuty = new BigDecimal(0);
    private OutPin auxPin = null;

    public PID(PIDInterface pidInterface) throws InvalidGPIOException {
        this.pidInterface = pidInterface;
        init();
    }

    private void init() throws InvalidGPIOException {
        if (!StringUtils.isEmpty(this.pidInterface.getAuxGPIO())) {
            this.auxPin = new OutPin(this.pidInterface.getAuxGPIO());
        }
    }

    @Override
    public TemperatureInterface getTemperature() {
        return this.pidInterface.getTemperature();
    }

    @Override
    public void setTemperature(Temperature temperature) {
        this.pidInterface.setTemperature(temperature);
    }

    @Override
    public String getName() {
        return this.pidInterface.getName();
    }

    @Override
    public PIDMode getPidMode() {
        return this.pidInterface.getPidMode();
    }

    @Override
    public void setPidMode(PIDMode pidMode) {
        this.pidInterface.setPidMode(pidMode);
    }

    @Override
    public String getHeatGPIO() {
        return this.pidInterface.getHeatGPIO();
    }

    @Override
    public String getCoolGPIO() {
        return this.pidInterface.getCoolGPIO();
    }

    @Override
    public String getAuxGPIO() {
        return this.pidInterface.getAuxGPIO();
    }

    @Override
    public BigDecimal getDutyCycle() {
        return this.pidInterface.getDutyCycle();
    }

    @Override
    public void setDutyCycle(BigDecimal duty) {
        this.pidInterface.setDutyCycle(duty);
    }

    @Override
    public BigDecimal getSetPoint() {
        return this.pidInterface.getSetPoint();
    }

    @Override
    public void setSetPoint(BigDecimal setPoint) {
        this.pidInterface.setSetPoint(setPoint);
    }

    @Override
    public Boolean isAuxInverted() {
        return this.pidInterface.isAuxInverted();
    }

    @Override
    public void setCool(String gpio, BigDecimal duty, BigDecimal delay, BigDecimal cycle,
                        BigDecimal p, BigDecimal i, BigDecimal d) {
        this.pidInterface.setCool(gpio, duty, delay, cycle, p, i, d);
    }

    @Override
    public BigDecimal getMinTemp() {
        return this.pidInterface.getMinTemp();
    }

    @Override
    public void setMinTemp(BigDecimal minTemp) {
        this.pidInterface.setMinTemp(minTemp);
    }

    @Override
    public BigDecimal getMaxTemp() {
        return this.pidInterface.getMaxTemp();
    }

    @Override
    public void setMaxTemp(BigDecimal maxTemp) {
        this.pidInterface.setMaxTemp(maxTemp);
    }

    @Override
    public BigDecimal getMinTime() {
        return this.pidInterface.getMinTime();
    }

    @Override
    public void setMinTime(BigDecimal minTime) {
        this.pidInterface.setMinTime(minTime);
    }

    @Override
    public void setInvertAux(boolean invert) {
        this.pidInterface.setInvertAux(invert);
    }

    @Override
    public BigDecimal getManualDuty() {
        return this.pidInterface.getManualDuty();
    }

    @Override
    public void setManualDuty(BigDecimal duty) {
        this.pidInterface.setManualDuty(duty);
    }

    @Override
    public BigDecimal getManualTime() {
        return this.pidInterface.getManualTime();
    }

    @Override
    public void setManualTime(BigDecimal time) {
        this.pidInterface.setManualTime(time);
    }

    @Override
    public PIDSettings getHeatSetting() {
        return this.pidInterface.getHeatSetting();
    }

    @Override
    public PIDSettings getCoolSetting() {
        return this.pidInterface.getCoolSetting();
    }

    @Override
    public Long getId() {
        return this.pidInterface.getId();
    }

    /**
     * Determine if the GPIO is valid and return it if it is.
     *
     * @param gpio The GPIO String to check.
     * @return The GPIO pin if it's valid, or blank if it's not.
     */
    private String detectGPIO(final String gpio) {
        // Determine what kind of GPIO Mapping we have
        Pattern pinPattern = Pattern.compile("(GPIO)([0-9])_([0-9]+)");
        Pattern pinPatternAlt = Pattern.compile("(GPIO)?_?([0-9]+)");

        Matcher pinMatcher = pinPattern.matcher(gpio);

        BrewServer.LOG.info(gpio + " Matches: " + pinMatcher.groupCount());

        if (pinMatcher.matches()) {
            // Beagleboard style input
            BrewServer.LOG.info("Matched GPIO pinout for Beagleboard: "
                    + gpio + ". OS: " + System.getProperty("os.level"));
            return gpio;
        } else {
            pinMatcher = pinPatternAlt.matcher(gpio);
            if (pinMatcher.matches()) {
                BrewServer.LOG.info("Direct GPIO Pinout detected. OS: "
                        + System.getProperty("os.level"));
                // The last group gives us the GPIO number
                return gpio;
            } else {
                BrewServer.LOG.info("Could not match the GPIO!");
                return null;
            }
        }
    }

    /**
     * Toggle the aux pin from it's current state.
     */
    void toggleAux() {
        // Flip the aux pin value
        if (this.auxPin != null) {
            // If the value if "1" we set it to false
            // If the value is not "1" we set it to true
            BrewServer.LOG.info("Aux Pin is being set to: " + !this.auxPin.getValue().equals("1"));
            setAux(!getAuxStatus());
        } else {
            BrewServer.LOG.info("Aux Pin is not set for " + this.pidInterface.getName());
        }
    }

    public void setAux(boolean on) {
        if (this.pidInterface.isAuxInverted()) {
            this.auxPin.setValue(!on);
        } else {
            this.auxPin.setValue(on);
        }
    }

    /**
     * @return True if there's an aux pin
     */
    public boolean hasAux() {
        return (this.auxPin != null);
    }

    private boolean getAuxStatus() {
        if (this.pidInterface.isAuxInverted()) {
            return this.auxPin.getValue().equals("0");
        }
        return this.auxPin.getValue().equals("1");
    }

    public enum PIDMode {
        HYSTERIA("Hysteria"),
        AUTO("Auto"),
        MANUAL("Manual"),
        OFF("Off");

        private String value;

        PIDMode(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
