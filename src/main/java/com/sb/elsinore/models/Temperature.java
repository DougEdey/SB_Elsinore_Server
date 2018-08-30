package com.sb.elsinore.models;

import com.sb.elsinore.TemperatureListeners;
import io.leangen.graphql.annotations.GraphQLQuery;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

import static com.google.common.base.Strings.isNullOrEmpty;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@EntityListeners(TemperatureListeners.class)
public class Temperature implements TemperatureInterface {

    @NotNull
    @javax.validation.constraints.Size(min = 1)
    @GraphQLQuery(name = "device", description = "A temp probes device")
    String device = "";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @GraphQLQuery(name = "id", description = "A temp probe's id")
    private Long id;
    private boolean hidden = false;

    private String name;
    private String probeName;
    private BigDecimal cutoffTemp = null;

    /**
     * Other strings, obviously named.
     */
    private String scale = "C", volumeAddress = "", volumeOffset = "",
            volumeUnit = "";

    /**
     * Are we measuring volume?
     */
    private boolean volumeMeasurementEnabled = false;
    /**
     * Volume analog input number.
     */
    private int volumeAIN = -1;
    private BigDecimal calibration = BigDecimal.ZERO;
    private Integer position = -1;

    private int i2cChannel = -1;
    private String i2cDevAddress = null;
    private String i2cDevNumber = null;
    private String i2cDevType = null;

    public Temperature() {
    }

    public Temperature(String name, String device) {
        this.device = device;


        setName(name);
        this.probeName = this.device;
    }

    public Temperature(Temperature other) {
        this.name = other.name;
        this.scale = other.scale;
        this.device = other.device;
    }

    /**
     * @return The name of this probe
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * @param name The name to set this TempProbe to.
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDevice() {
        return this.device;
    }

    @Override
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
    @Override
    public String getProbe() {
        return this.probeName;
    }

    /**
     * @return The temp unit/Scale
     */
    @Override
    public String getScale() {
        return this.scale;
    }

    /**
     * @param scale Value to set the temperature unit to.
     */
    @Override
    public void setScale(String scale) {
        this.scale = scale;
    }

    @Override
    public BigDecimal getCalibration() {
        return this.calibration;
    }

    @Override
    public void setCalibration(BigDecimal calibration) {
        this.calibration = calibration;
    }

    /**
     * @return The current volume unit
     */
    @Override
    public String getVolumeUnit() {
        return this.volumeUnit;
    }

    /**
     * @param unit Unit to set the volume units to.
     */
    @Override
    public void setVolumeUnit(final String unit) {
        this.volumeUnit = unit;
    }

    /**
     * @return The analogue input
     */
    @Override
    public String getVolumeAIN() {
        if (this.volumeAIN == -1) {
            return "";
        }
        return Integer.toString(this.volumeAIN);
    }

    @Override
    public void setVolumeAIN(int volumeAIN) {
        this.volumeAIN = volumeAIN;
    }

    /**
     * @return Get the volume address
     */
    @Override
    public String getVolumeAddress() {
        return this.volumeAddress;
    }

    @Override
    public void setVolumeAddress(String volumeAddress) {
        this.volumeAddress = volumeAddress;
    }

    /**
     * @return The current volume offset
     */
    @Override
    public String getVolumeOffset() {
        return this.volumeOffset;
    }

    @Override
    public void setVolumeOffset(String volumeOffset) {
        this.volumeOffset = volumeOffset;
    }

    /**
     * Check to see if this object has a valid volume input.
     *
     * @return true if there's a valid volume input on this class
     */
    @Override
    public boolean hasVolume() {
        return (this.volumeAddress != null && !this.volumeAddress.equals("")
                && this.volumeOffset != null && !this.volumeOffset.equals("")) || (this.volumeAIN != -1);
    }

    @Override
    public void hide() {
        this.hidden = true;
    }

    @Override
    public void show() {
        this.hidden = false;
    }

    @Override
    public boolean isHidden() {
        return this.hidden;
    }

    /**
     * @return The position of this temp probe in the list.
     */
    @Override
    public int getPosition() {
        return this.position;
    }

    /**
     * Set the position of this temp probe.
     *
     * @param newPos The new position.
     */
    @Override
    public void setPosition(final int newPos) {
        this.position = newPos;
    }

    @Override
    public String getI2CNumber() {
        return this.i2cDevNumber;
    }

    @Override
    public void setI2CNumber(String number) {
        this.i2cDevNumber = number;
    }

    @Override
    public String getI2CAddress() {
        return this.i2cDevAddress;
    }

    @Override
    public void setI2CAddress(String address) {
        this.i2cDevAddress = address;
    }

    @Override
    public String getI2CChannel() {
        if (this.i2cChannel == -1) {
            return "";
        }
        return Integer.toString(this.i2cChannel);
    }

    @Override
    public void setI2CChannel(String channel) {
        try {
            this.i2cChannel = Integer.parseInt(channel);
        } catch (NumberFormatException nfe) {
            this.i2cChannel = -1;
        }
    }

    @Override
    public String getI2CDevType() {
        return this.i2cDevType;
    }

    @Override
    public void setI2CDevType(String devType) {
        this.i2cDevType = devType;
    }

    @Override
    public BigDecimal getCutoffTemp() {
        return this.cutoffTemp;
    }

    @Override
    public void setCutoffTemp(BigDecimal cutoffTemp) {
        this.cutoffTemp = cutoffTemp;
    }

    @Override
    public boolean getVolumeMeasurementEnabled() {
        return this.volumeMeasurementEnabled;
    }

    @Override
    public void setVolumeMeasurementEnabled(boolean enabled) {
        this.volumeMeasurementEnabled = enabled;
    }

    @Override
    public Temperature getModel() {
        return this;
    }
}
