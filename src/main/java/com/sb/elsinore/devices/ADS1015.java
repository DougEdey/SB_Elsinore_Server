package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;

/**
 * Read from an ADS1015 device on a specific channel
 * Created by doug on 12/07/15.
 */
@SuppressWarnings("unused")
public class ADS1015 extends I2CDevice {

    @Override
    public String getDevName() {
        return DEV_NAME;
    }

    public ADS1015(int deviceNo, int address) {
        super(deviceNo, address);
    }

    public static String DEV_NAME = "ADS1015";

    public int ADS1015_READBIT                 = 0x01;
/*=========================================================================*/

/*=========================================================================
    POINTER REGISTER
    -----------------------------------------------------------------------*/
public int ADS1015_REG_POINTER_MASK        = (0x03);
    public int ADS1015_REG_POINTER_CONVERT     = (0x00);
    public int ADS1015_REG_POINTER_CONFIG      = (0x10);
    public int ADS1015_REG_POINTER_LOWTHRESH   = (0x02);
    public int ADS1015_REG_POINTER_HITHRESH    = (0x03);
/*=========================================================================*/

/*=========================================================================
    CONFIG REGISTER
    -----------------------------------------------------------------------*/
    public int ADS1015_REG_CONFIG_OS_MASK      = (0x8000);
    public int ADS1015_REG_CONFIG_OS_SINGLE    = (0x8000);  // Write: Set to start a single-conversion
    public int ADS1015_REG_CONFIG_OS_BUSY      = (0x0000);  // Read: Bit = 0 when conversion is in progress
    public int ADS1015_REG_CONFIG_OS_NOTBUSY   = (0x8000);  // Read: Bit = 1 when device is not performing a conversion

    public int  ADS1015_REG_CONFIG_MUX_MASK     =  0x7000;
    public int  ADS1015_REG_CONFIG_MUX_DIFF_0_1 =  0x0000;  // Differential P = AIN0, N = AIN1 (default)
    public int  ADS1015_REG_CONFIG_MUX_DIFF_0_3 =  0x1000;  // Differential P = AIN0, N = AIN3
    public int  ADS1015_REG_CONFIG_MUX_DIFF_1_3 =  0x2000;  // Differential P = AIN1, N = AIN3
    public int  ADS1015_REG_CONFIG_MUX_DIFF_2_3 =  0x3000;  // Differential P = AIN2, N = AIN3
    public int  ADS1015_REG_CONFIG_MUX_SINGLE_0 =  0x4000;  // Single-ended AIN0
    public int  ADS1015_REG_CONFIG_MUX_SINGLE_1 =  0x5000;  // Single-ended AIN1
    public int  ADS1015_REG_CONFIG_MUX_SINGLE_2 =  0x6000;  // Single-ended AIN2
    public int  ADS1015_REG_CONFIG_MUX_SINGLE_3 =  0x7000;  // Single-ended AIN3

    public int  ADS1015_REG_CONFIG_PGA_MASK     =  0x0E00;
    public int  ADS1015_REG_CONFIG_PGA_6_144V   =  0x0000;  // +/-6.144V range
    public int  ADS1015_REG_CONFIG_PGA_4_096V   =  0x0200;  // +/-4.096V range
    public int  ADS1015_REG_CONFIG_PGA_2_048V   =  0x0400;  // +/-2.048V range (default)
    public int  ADS1015_REG_CONFIG_PGA_1_024V   =  0x0600;  // +/-1.024V range
    public int  ADS1015_REG_CONFIG_PGA_0_512V   =  0x0800;  // +/-0.512V range
    public int  ADS1015_REG_CONFIG_PGA_0_256V   =  0x0A00;  // +/-0.256V range

    public int  ADS1015_REG_CONFIG_MODE_MASK    =  0x0100;
    public int  ADS1015_REG_CONFIG_MODE_CONTIN  =  0x0000;  // Continuous conversion mode
    public int  ADS1015_REG_CONFIG_MODE_SINGLE  =  0x0100;  // Power-down single-shot mode (default)

    public int  ADS1015_REG_CONFIG_DR_MASK      =  0x00E0;
    public int  ADS1015_REG_CONFIG_DR_128SPS    =  0x0000;  // 128 samples per second
    public int  ADS1015_REG_CONFIG_DR_250SPS    =  0x0020;  // 250 samples per second
    public int  ADS1015_REG_CONFIG_DR_490SPS    =  0x0040;  // 490 samples per second
    public int  ADS1015_REG_CONFIG_DR_920SPS    =  0x0050;  // 920 samples per second
    public int  ADS1015_REG_CONFIG_DR_1600SPS   =  0x0080;  // 1600 samples per second (default)
    public int  ADS1015_REG_CONFIG_DR_2400SPS   =  0x00A0;  // 2400 samples per second
    public int  ADS1015_REG_CONFIG_DR_3300SPS   =  0x00C0;  // 3300 samples per second

    public int  ADS1015_REG_CONFIG_CMODE_MASK   =  0x0010;
    public int  ADS1015_REG_CONFIG_CMODE_TRAD   =  0x0000;  // Traditional comparator with hysteresis (default)
    public int  ADS1015_REG_CONFIG_CMODE_WINDOW =  0x0010;  // Window comparator

    public int  ADS1015_REG_CONFIG_CPOL_MASK    =  0x0008;
    public int  ADS1015_REG_CONFIG_CPOL_ACTVLOW =  0x0000;  // ALERT/RDY pin is low when active (default)
    public int  ADS1015_REG_CONFIG_CPOL_ACTVHI  =  0x0008;  // ALERT/RDY pin is high when active

    public int  ADS1015_REG_CONFIG_CLAT_MASK    =  0x0004;  // Determines if ALERT/RDY pin latches once asserted
    public int  ADS1015_REG_CONFIG_CLAT_NONLAT  =  0x0000;  // Non-latching comparator (default)
    public int  ADS1015_REG_CONFIG_CLAT_LATCH   =  0x0004;  // Latching comparator

    public int  ADS1015_REG_CONFIG_CQUE_MASK    =  0x0003;
    public int  ADS1015_REG_CONFIG_CQUE_1CONV   =  0x0000;  // Assert ALERT/RDY after one conversions
    public int  ADS1015_REG_CONFIG_CQUE_2CONV   =  0x0001;  // Assert ALERT/RDY after two conversions
    public int  ADS1015_REG_CONFIG_CQUE_4CONV   =  0x0002;  // Assert ALERT/RDY after four conversions
    public int  ADS1015_REG_CONFIG_CQUE_NONE    =  0x0003;  // Disable the comparator and put ALERT/RDY in high state (default)
/*=========================================================================*/

    /**
     * Read a single value from an ADS1015 device.
     * @param channel
     * @return
     */
    public float readValue(int channel)
    {
        error = ads1015error.ADS1015_ERROR_OK;

        if (!(_adsInitialised)) {
            ads1015Init();
        }

        if (channel > 3) {
            error = ads1015error.ADS1015_ERROR_INVALIDCHANNEL;
            return -1f;
        }

        // Start with default values
        int config = ADS1015_REG_CONFIG_CQUE_NONE    | // Disable the comparator (default val)
                ADS1015_REG_CONFIG_CLAT_NONLAT  | // Non-latching (default val)
                ADS1015_REG_CONFIG_CPOL_ACTVLOW | // Alert/Rdy active low   (default val)
                ADS1015_REG_CONFIG_CMODE_TRAD   | // Traditional comparator (default val)
                ADS1015_REG_CONFIG_DR_1600SPS   | // 1600 samples per second (default)
                ADS1015_REG_CONFIG_MODE_SINGLE;   // Single-shot mode (default)

        // Set PGA/voltage range
        config |= ADS1015_REG_CONFIG_PGA_6_144V;            // +/- 6.144V range (limited to VDD +0.3V max!)

        // Set single-ended input channel
        switch (channel)
        {
            case (0):
                config |= ADS1015_REG_CONFIG_MUX_SINGLE_0;
                break;
            case (1):
                config |= ADS1015_REG_CONFIG_MUX_SINGLE_1;
                break;
            case (2):
                config |= ADS1015_REG_CONFIG_MUX_SINGLE_2;
                break;
            case (3):
                config |= ADS1015_REG_CONFIG_MUX_SINGLE_3;
                break;
        }

        // Set 'start single-conversion' bit
        config |= ADS1015_REG_CONFIG_OS_SINGLE;

        // Write config register to the ADC
        error = ads1015WriteRegister(ADS1015_REG_POINTER_CONFIG, config);
        if (error != ads1015error.ADS1015_ERROR_OK) return -1f;

        // Wait for the conversion to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Do nothing
        }

        // Read the conversion results

        int value = ina219Read16(ADS1015_REG_POINTER_CONVERT);
        if (value == -1) {
            BrewServer.LOG.warning("Failed to read ADS1015");
            return -1f;
        }

        // Shift results 4-bits to the right
        value = value >> 4;
        return value;
    }
}
