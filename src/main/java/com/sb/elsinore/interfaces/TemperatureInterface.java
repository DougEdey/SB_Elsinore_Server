package com.sb.elsinore.interfaces;

import com.sb.elsinore.models.TemperatureModel;

import java.math.BigDecimal;

public interface TemperatureInterface {

    /**
     * @return The Id of this Temperature object
     */
    Long getId();

    void setId(Long id);

    /**
     * @return The name of this temperature probe
     */
    String getName();

    /**
     * @param name The new name of this temperature probe
     */
    void setName(String name);

    /**
     * @return The device probe to use to reading temperatures
     */
    String getDevice();

    /**
     * @param device The new device probe to use for reading temperatures
     */
    void setDevice(String device);

    /**
     * @return The scale of temperature readings
     */
    String getScale();

    /**
     * @param scale The new scale for temperature readings
     */
    void setScale(String scale);

    String getVolumeUnit();

    void setVolumeUnit(String unit);

    String getVolumeAIN();

    void setVolumeAIN(int volumeAIN);

    String getVolumeAddress();

    void setVolumeAddress(String volumeAddress);

    String getVolumeOffset();

    void setVolumeOffset(String volumeOffset);

    boolean hasVolume();

    BigDecimal getCalibration();

    void setCalibration(BigDecimal calibration);

    void hide();

    void show();

    boolean isHidden();

    void setHidden(boolean hidden);

    int getPosition();

    void setPosition(int position);

    String getI2cNumber();

    void setI2cNumber(String number);

    String getI2cAddress();

    void setI2cAddress(String address);

    String getI2cChannel();

    void setI2cChannel(String channel);

    String getI2cType();

    void setI2cType(String type);

    BigDecimal getCutoffTemp();

    void setCutoffTemp(BigDecimal cutoffTemp);

    boolean getVolumeMeasurementEnabled();

    void setVolumeMeasurementEnabled(boolean enabled);

    TemperatureModel getModel();
}
