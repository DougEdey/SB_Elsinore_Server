package com.sb.elsinore.models;

import java.math.BigDecimal;

public interface TemperatureInterface {
    String getName();

    void setName(String name);

    String getDevice();

    void setDevice(String device);

    String getProbe();

    String getScale();

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

    Temperature getModel();
}
