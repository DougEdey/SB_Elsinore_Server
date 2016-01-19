package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;
import com.sun.jna.Native;

/**
 * Created by doug on 18/07/15.
 */
public class ADS1115 extends ADS1015 {

    public static String DEV_NAME = "ADS1115";
    @Override
    public String getDevName() {
        return DEV_NAME;
    }

    public static int OS 	= 15;
    public static int MUXX2 	= 14;
    public static int MUXX1 =	13;
    public static int MUXX0 =	12;
    public static int PGA2 	= 11;
    public static int PGA1 	= 10;
    public static int PGA0 	= 9;
    public static int MODE 	= 8;
    public static int DR2 	= 7;
    public static int DR1 	= 6;
    public static int DR0 	= 5;
    public static int COMP_MODE =	4;
    public static int COMP_POL =	3;
    public static int COMP_LAT =	2;
    public static int COMP_QUE1 =	1;
    public static int COMP_QUE0 =	0;

    // Programmable gain amplifier (PGA) configuration
    // 000 : FS = ±6.144V¹
    public static int PGA_6_144 	=	(0);
    // 001 : FS = ±4.096V¹
    public static int PGA_4_096 	=	PGA0;
    // 010 : FS = ±2.048V (default)
    public static int PGA_2_048 	=	PGA1;
    // 011 : FS = ±1.024V
    public static int PGA_1_024 	= PGA1 | PGA0;
    // 100 : FS = ±0.512V
    public static int PGA_0_512 	= PGA2;
    // 101 : FS = ±0.256V
    // 110 : FS = ±0.256V
    // 111 : FS = ±0.256V
    public static int PGA_0_256		= (PGA2 | PGA1 | PGA0);

    // Data rate (DR)
    // 000 : 8SPS
    public static int DR_8SPS 		= 0;
    // 100 : 128SPS (default)
    public static int DR_128SPS 		= DR2;

    // Input multiplexer configuration
    // 000 : AINP = AIN0 and AINN = AIN1 (default)
    public static int IM_IN0_IN1 	= 0;
    // 001 : AINP = AIN0 and AINN = AIN3
    public static int IM_IN0_IN3 	= MUXX0;
    // 010 : AINP = AIN1 and AINN = AIN3
    public static int IM_IN1_IN3 	= MUXX1;
    // 011 : AINP = AIN2 and AINN = AIN3
    public static int IM_IN2_IN3 =		(MUXX1 | MUXX0);

    // 100 : AINP = AIN0 and AINN = GND
    public static int IM_IN0_GND 	= MUXX2;
    // 101 : AINP = AIN1 and AINN = GND
    public static int IM_IN1_GND 	= (MUXX2 | MUXX0);
    // 110 : AINP = AIN2 and AINN = GND
    public static int IM_IN2_GND 	=	(MUXX2 | MUXX1);
    // 111 : AINP = AIN3 and AINN = GND
    public static int IM_IN3_GND 	= (MUXX2 | MUXX1 | MUXX0);

    public int ADS1115_CFG_CH0 	= (OS | MODE | DR_8SPS | PGA_4_096 | COMP_QUE1 | COMP_QUE0);

    public static int ADS1115_REG_CONVERSION =	0x00;
    public static int ADS1115_REG_CONFIG =	0x01;


    public static int ADS1115_ERR_CONNECTION_LOST = -32768;

    public ADS1115(int deviceNo, int address) {
        super(deviceNo, address);
    }

    @Override
    public float readValue(int devChannel)
    {
        if (!(_adsInitialised)) {
            ads1015Init();
        }
        // Set configuration
        switch (devChannel)
        {
            case 0:
                ADS1115_CFG_CH0 |= IM_IN0_GND;
                break;
            case 1:
                ADS1115_CFG_CH0 |= IM_IN1_GND;
                break;
            case 2:
                ADS1115_CFG_CH0 |= IM_IN2_GND;
                break;
            case 3:
                ADS1115_CFG_CH0 |= IM_IN3_GND;
                break;
        }

        if (ads1015WriteRegister(ADS1115_REG_CONFIG, ADS1115_CFG_CH0 ) != ads1015error.ADS1015_ERROR_OK)
        {
            return 0;
        }

        // Read configuration
        int v = ina219Read16(ADS1115_REG_CONVERSION);
        float value = (((float)v)*4096/32767);
        BrewServer.LOG.warning("Read value: " + value);

        return value;
    }

    public boolean i2cInit()
    {
        String devpath = String.format(I2CDevice.BASE_PATH, device_number);
        fd = libC.open(devpath, O_RDWR);
        if (fd < 0)
        {
            BrewServer.LOG.warning(String.format("Failed to read from %s. Error %s", devpath, Native.getLastError()));
            return false;
        }
        else {
            BrewServer.LOG.warning(String.format("Opened fd %s", fd));
        }

        int iovalue = libC.ioctl(fd, I2CDevice.I2C_SLAVE, address);

        if (iovalue < 0)
        {
            BrewServer.LOG.warning(String.format("Failed to open slave device at address %s, wrote %s", address, iovalue));
            BrewServer.LOG.warning(String.format("Error %s", Native.getLastError()));
            return false;
        }
        else
        {
            BrewServer.LOG.warning(String.format("I2C Slave %s opened %s", address, iovalue));
        }
        return true;
    }
}
