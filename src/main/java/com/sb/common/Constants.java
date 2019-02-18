package com.sb.common;

import java.math.BigDecimal;

public interface Constants {
    BigDecimal ERROR_TEMP = new BigDecimal(-999);
    /**
     * Base path for BBB System TempProbe.
     */
    String bbbSystemTemp =
            "/sys/class/hwmon/hwmon0/device/temp1_input";
    /**
     * Base path for RPi System TempProbe.
     */
    String rpiSystemTemp =
            "/sys/class/thermal/thermal_zone1/tempProbe";

    String SYSTEM = "System";

    String RepoURL = "http://dougedey.github.io/SB_Elsinore_Server/";
}
