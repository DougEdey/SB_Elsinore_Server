package com.sb.elsinore.interfaces;

import javax.validation.constraints.NotNull;

public interface OutputDeviceInterface {
    Long getId();

    void setId(Long id);

    @NotNull String getName();

    void setName(@NotNull String name);

    PIDSettingsInterface getPidSettings();

    void setPidSettings(PIDSettingsInterface pidSettings);

    @NotNull String getType();

    void setType(@NotNull String type);
}
