package com.sb.elsinore.models;

import com.sb.elsinore.TemperatureListeners;
import com.sb.elsinore.interfaces.TemperatureInterface;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

import static com.google.common.base.Strings.isNullOrEmpty;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"nameLowercased"}),
        @UniqueConstraint(columnNames = {"device"})
})
@EntityListeners(TemperatureListeners.class)
public class TemperatureModel implements TemperatureInterface {
    @NotNull
    @javax.validation.constraints.Size(min = 1)
    private String device = "";
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private boolean hidden = false;
    @NotNull
    @javax.validation.constraints.Size(min = 1)
    private String name;
    private String nameLowercased;
    private BigDecimal cutoffTemp = null;
    /**
     * Other strings, obviously named.
     */
    private String scale = "C", volumeAddress = "", volumeOffset = "",
            volumeUnit = "";
    /**
     * Are we measuring volume?
     */
    private Boolean volumeMeasurementEnabled = false;
    /**
     * Volume analog input number.
     */
    private Integer volumeAIN = -1;
    private BigDecimal calibration = BigDecimal.ZERO;
    private Integer position = -1;
    private Integer i2cChannel = -1;
    private String i2cAddress = null;
    private String i2cNumber = null;
    private String i2cType = null;

    public TemperatureModel() {
    }

    public TemperatureModel(String name, @NotNull String device) {
        this.device = device;


        setName(name);
    }

    public TemperatureModel(TemperatureModel other) {
        this.name = other.name;
        this.scale = other.scale;
        this.device = other.device;
    }

    @PrePersist
    @PreUpdate
    private void prepare() {
        this.nameLowercased = this.name.toLowerCase();
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
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

    public String getNameLowercased() {
        return this.nameLowercased;
    }
    
    public void setNameLowercased(String nameLowercased) {
        this.nameLowercased = nameLowercased;
    }

    @Override
    public String getDevice() {
        return this.device;
    }

    @Override
    public void setDevice(String device) {
        if (isNullOrEmpty(device)) {
            this.device = "";
        } else {
            this.device = device;
        }
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
    public void setVolumeAIN(Integer volumeAIN) {
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
    public Boolean isHidden() {
        return this.hidden;
    }

    @Override
    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * @return The position of this temp probe in the list.
     */
    @Override
    public Integer getPosition() {
        return this.position;
    }

    /**
     * Set the position of this temp probe.
     *
     * @param newPos The new position.
     */
    @Override
    public void setPosition(final Integer newPos) {
        this.position = newPos;
    }

    @Override
    public String getI2cNumber() {
        return this.i2cNumber;
    }

    @Override
    public void setI2cNumber(String number) {
        this.i2cNumber = number;
    }

    @Override
    public String getI2cAddress() {
        return this.i2cAddress;
    }

    @Override
    public void setI2cAddress(String address) {
        this.i2cAddress = address;
    }

    @Override
    public String getI2cChannel() {
        if (this.i2cChannel == -1) {
            return "";
        }
        return Integer.toString(this.i2cChannel);
    }

    @Override
    public void setI2cChannel(String channel) {
        try {
            this.i2cChannel = Integer.parseInt(channel);
        } catch (NumberFormatException nfe) {
            this.i2cChannel = -1;
        }
    }

    @Override
    public String getI2cType() {
        return this.i2cType;
    }

    @Override
    public void setI2cType(String devType) {
        this.i2cType = devType;
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
    public Boolean getVolumeMeasurementEnabled() {
        return this.volumeMeasurementEnabled;
    }

    @Override
    public void setVolumeMeasurementEnabled(Boolean enabled) {
        this.volumeMeasurementEnabled = enabled;
    }

    @Override
    public TemperatureModel getModel() {
        return this;
    }
}
