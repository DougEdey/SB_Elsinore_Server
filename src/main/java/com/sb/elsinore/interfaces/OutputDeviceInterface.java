package com.sb.elsinore.interfaces;

import javax.validation.constraints.NotNull;

public interface OutputDeviceInterface {
    Long getId();

    @NotNull String getName();

    void setName(@NotNull String name);

    PIDSettingsInterface getPIDSettings();

    void setPIDSettings(PIDSettingsInterface pidSettings);

    @NotNull String getType();

    void setType(@NotNull String type);
}
