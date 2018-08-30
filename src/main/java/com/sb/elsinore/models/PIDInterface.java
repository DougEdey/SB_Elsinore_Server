package com.sb.elsinore.models;

import java.math.BigDecimal;

public interface PIDInterface {
    TemperatureInterface getTemperature();

    String getName();

    com.sb.elsinore.devices.PID.PIDMode getPidMode();

    void setPidMode(com.sb.elsinore.devices.PID.PIDMode pidMode);

    String getHeatGPIO();

    String getCoolGPIO();

    String getAuxGPIO();

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

    PIDSettings getHeatSetting();

    PIDSettings getCoolSetting();
}
