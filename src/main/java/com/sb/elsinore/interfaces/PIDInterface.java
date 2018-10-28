package com.sb.elsinore.interfaces;

import com.sb.elsinore.models.TemperatureModel;

import java.math.BigDecimal;

public interface PIDInterface {
    TemperatureInterface getTemperature();

    void setTemperature(TemperatureModel temperature);

    String getName();

    com.sb.elsinore.devices.PID.PIDMode getPidMode();

    void setPidMode(com.sb.elsinore.devices.PID.PIDMode pidMode);

    String getHeatGPIO();

    String getCoolGPIO();

    String getAuxGPIO();

    void setAuxGPIO(String auxGPIO);

    BigDecimal getDutyCycle();

    void setDutyCycle(BigDecimal duty);

    BigDecimal getSetPoint();

    void setSetPoint(BigDecimal setPoint);

    Boolean isAuxInverted();

    void setCool(String gpio, BigDecimal duty,
                 BigDecimal delay, BigDecimal cycle, BigDecimal p,
                 BigDecimal i, BigDecimal d);

    BigDecimal getMinTemp();

    void setMinTemp(BigDecimal minTemp);

    BigDecimal getMaxTemp();

    void setMaxTemp(BigDecimal maxTemp);

    BigDecimal getMinTime();

    void setMinTime(BigDecimal minTime);

    void setInvertAux(boolean invert);

    BigDecimal getManualDuty();

    void setManualDuty(BigDecimal duty);

    BigDecimal getManualTime();

    void setManualTime(BigDecimal time);

    PIDSettingsInterface getHeatSetting();

    void setHeatSetting(PIDSettingsInterface heatSetting);

    PIDSettingsInterface getCoolSetting();

    void setCoolSetting(PIDSettingsInterface coolSetting);

    Long getId();

    void setId(Long id);
}
