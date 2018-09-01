package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;
import com.sun.jna.Native;

/**
 * Created by doug on 18/07/15.
 */
public class ADS1115 extends ADS1015 {

    public static String DEV_NAME = "ADS1115";
    /*
     * =========================================================================
     * CONFIG REGISTER
     * -----------------------------------------------------------------------
     */
    private static int ADS1015_REG_CONFIG_OS_MASK = 0x8000;
    // start a
    // single-conversion
    private static int ADS1015_REG_CONFIG_OS_BUSY = 0x0000; // Read: Bit = 0
    // when conversion
    // is in progress
    private static int ADS1015_REG_CONFIG_OS_NOTBUSY = 0x8000; // Read: Bit = 1
    private static int ADS1015_REG_CONFIG_MUX_MASK = 0x7000;
    // when device
    // is not
    // performing a
    // conversion
    private static int ADS1015_REG_CONFIG_MUX_DIFF_0_1 = 0x0000; // Differential
    // P = AIN0,
    // N = AIN1
    // (default)
    private static int ADS1015_REG_CONFIG_MUX_DIFF_0_3 = 0x1000; // Differential
    // P = AIN0,
    // N = AIN3
    private static int ADS1015_REG_CONFIG_MUX_DIFF_1_3 = 0x2000; // Differential
    // P = AIN1,
    // N = AIN3
    private static int ADS1015_REG_CONFIG_MUX_DIFF_2_3 = 0x3000; // Differential
    private static int ADS1015_REG_CONFIG_PGA_MASK = 0x0E00;
    // AIN3
    private static int ADS1015_REG_CONFIG_PGA_6_144V = 0x0000; // +/-6.144V
    // range
    private static int ADS1015_REG_CONFIG_PGA_2_048V = 0x0400; // +/-2.048V
    // range
    // (default)
    private static int ADS1015_REG_CONFIG_PGA_1_024V = 0x0600; // +/-1.024V
    // range
    private static int ADS1015_REG_CONFIG_PGA_0_512V = 0x0800; // +/-0.512V
    // range
    private static int ADS1015_REG_CONFIG_PGA_0_256V = 0x0A00; // +/-0.256V
    private static int ADS1015_REG_CONFIG_MODE_MASK = 0x0100;
    // range
    private static int ADS1015_REG_CONFIG_MODE_CONTIN = 0x0000; // Continuous
    private static int ADS1015_REG_CONFIG_DR_MASK = 0x00E0;
    // single-shot
    // mode
    // (default)
    private static int ADS1015_REG_CONFIG_DR_128SPS = 0x0000; // 128 samples per
    // second
    private static int ADS1015_REG_CONFIG_DR_250SPS = 0x0020; // 250 samples per
    // second
    private static int ADS1015_REG_CONFIG_DR_490SPS = 0x0040; // 490 samples per
    // second
    private static int ADS1015_REG_CONFIG_DR_1600SPS = 0x0080; // 1600 samples
    // per second
    // (default)
    private static int ADS1015_REG_CONFIG_DR_2400SPS = 0x00A0; // 2400 samples
    // per second
    private static int ADS1015_REG_CONFIG_DR_3300SPS = 0x00C0; // 3300 samples
    private static int ADS1015_REG_CONFIG_CMODE_MASK = 0x0010;
    // per second
    // comparator
    // with
    // hysteresis
    // (default)
    private static int ADS1015_REG_CONFIG_CMODE_WINDOW = 0x0010; // Window
    // comparator
    private static int ADS1015_REG_CONFIG_CPOL_MASK = 0x0008;
    // pin is
    // low when
    // active
    // (default)
    private static int ADS1015_REG_CONFIG_CPOL_ACTVHI = 0x0008; // ALERT/RDY
    private static int ADS1015_REG_CONFIG_CLAT_MASK = 0x0004; // Determines if
    // pin is
    // high when
    // active
    // comparator
    // (default)
    private static int ADS1015_REG_CONFIG_CLAT_LATCH = 0x0004; // Latching
    private static int ADS1015_REG_CONFIG_CQUE_MASK = 0x0003;
    // comparator
    private static int ADS1015_REG_CONFIG_CQUE_1CONV = 0x0000; // Assert
    // ALERT/RDY
    // after one
    // conversions
    private static int ADS1015_REG_CONFIG_CQUE_2CONV = 0x0001; // Assert
    // ALERT/RDY
    // after two
    // conversions
    private static int ADS1015_REG_CONFIG_CQUE_4CONV = 0x0002; // Assert

    public ADS1115(int deviceNo, int address) {
        super(deviceNo, address);
    }

    @Override
    public String getDevName() {
        return DEV_NAME;
    }

    private Integer getConfigDefaults() {
        // Start with default values
        int ADS1015_REG_CONFIG_CQUE_NONE = 0x0003;
        int ADS1015_REG_CONFIG_CLAT_NONLAT = 0x0000;
        int ADS1015_REG_CONFIG_CPOL_ACTVLOW = 0x0000;
        int ADS1015_REG_CONFIG_CMODE_TRAD = 0x0000;
        int ADS1015_REG_CONFIG_DR_920SPS = 0x0060;
        int ADS1015_REG_CONFIG_MODE_SINGLE = 0x0100;
        Integer config = ADS1015_REG_CONFIG_CQUE_NONE | // Disable the
                // comparator
                // (default
                // val)
                ADS1015_REG_CONFIG_CLAT_NONLAT | // Non-latching
                // (default
                // val)
                ADS1015_REG_CONFIG_CPOL_ACTVLOW | // Alert/Rdy active
                // low
                // (default val)
                ADS1015_REG_CONFIG_CMODE_TRAD | // Traditional
                // comparator
                // (default val)
                ADS1015_REG_CONFIG_DR_920SPS | // 1600 samples per
                // second
                // (default)
                ADS1015_REG_CONFIG_MODE_SINGLE; // Single-shot mode
        // (default)

        // Set PGA/voltage range
        int ADS1015_REG_CONFIG_PGA_4_096V = 0x0200;
        config |= ADS1015_REG_CONFIG_PGA_4_096V; // +/- 6.144V range
        // (limited to
        // VDD +0.3V max!)
        return config;
    }

    @Override
    public float readValue(int devChannel) {
        ads1015Init();

        Integer config = getConfigDefaults();
        // Set configuration
        switch (devChannel) {
            case (0):
                int ADS1015_REG_CONFIG_MUX_SINGLE_0 = 0x4000;
                config |= ADS1015_REG_CONFIG_MUX_SINGLE_0;
                break;
            case (1):
                int ADS1015_REG_CONFIG_MUX_SINGLE_1 = 0x5000;
                config |= ADS1015_REG_CONFIG_MUX_SINGLE_1;
                break;
            case (2):
                int ADS1015_REG_CONFIG_MUX_SINGLE_2 = 0x6000;
                config |= ADS1015_REG_CONFIG_MUX_SINGLE_2;
                break;
            case (3):
                int ADS1015_REG_CONFIG_MUX_SINGLE_3 = 0x7000;
                config |= ADS1015_REG_CONFIG_MUX_SINGLE_3;
                break;
        }

        int ADS1015_REG_CONFIG_OS_SINGLE = 0x8000;
        config |= ADS1015_REG_CONFIG_OS_SINGLE;

        if (ads1015WriteRegister(this.ADS1015_REG_POINTER_CONFIG, config) != ads1015error.ADS1015_ERROR_OK) {
            close();
            return 0;
        }

        // Read configuration
        int v = ina219Read16(this.ADS1015_REG_POINTER_CONVERT);
        close();

        float value = (((float) v) * 4096 / 32767);
        BrewServer.LOG.warning("Read value: " + value);

        return value;
    }

    @Override
    public boolean i2cInit() {
        String devpath = String.format(I2CDevice.BASE_PATH, getDeviceNumber());
        this.fd = this.libC.open(devpath, O_RDWR);
        if (this.fd < 0) {
            BrewServer.LOG.warning(String.format("Failed to read from %s. Error %s", devpath, Native.getLastError()));
            return false;
        } else {
            BrewServer.LOG.warning(String.format("Opened fd %s", this.fd));
        }

        int iovalue = this.libC.ioctl(this.fd, I2CDevice.I2C_SLAVE, getAddress());

        if (iovalue < 0) {
            BrewServer.LOG.warning(String.format("Failed to open slave device at address %s, wrote %s", getAddress(), iovalue));
            BrewServer.LOG.warning(String.format("Error %s", Native.getLastError()));
            return false;
        } else {
            BrewServer.LOG.warning(String.format("I2C Slave %s opened %s", getAddress(), iovalue));
        }
        return true;
    }
}
