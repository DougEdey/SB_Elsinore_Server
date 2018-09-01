package com.sb.elsinore.interfaces;

import com.sb.elsinore.models.PIDSettings;

import java.math.BigDecimal;

public interface PIDSettingsInterface {
    Long getId();

    String getGPIO();

    void setGPIO(String newGPIO);

    BigDecimal getCycleTime();

    void setCycleTime(BigDecimal cycle_time);

    BigDecimal getProportional();

    void setProportional(BigDecimal proportional);

    BigDecimal getIntegral();

    void setIntegral(BigDecimal integral);

    BigDecimal getDerivative();

    void setDerivative(BigDecimal derivative);

    BigDecimal getDelay();

    void setDelay(BigDecimal delay);

    PIDSettings.PID_MODE getMode();

    void setMode(PIDSettings.PID_MODE mode);

    String getProfile();

    void setProfile(String profile);

    boolean isInverted();

    void setInverted(boolean inverted);
}
