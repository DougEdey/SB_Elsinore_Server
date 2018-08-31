package com.sb.elsinore.models;

import java.math.BigDecimal;

public interface TemperatureInterface {

    /**
     * @return The Id of this Temperature object
     */
    Long getId();

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

    int getPosition();

    void setPosition(int position);

    String getI2CNumber();

    void setI2CNumber(String number);

    String getI2CAddress();

    void setI2CAddress(String address);

    String getI2CChannel();

    void setI2CChannel(String channel);

    String getI2CDevType();

    void setI2CDevType(String type);

    BigDecimal getCutoffTemp();

    void setCutoffTemp(BigDecimal cutoffTemp);

    boolean getVolumeMeasurementEnabled();

    void setVolumeMeasurementEnabled(boolean enabled);

    TemperatureModel getModel();
}
