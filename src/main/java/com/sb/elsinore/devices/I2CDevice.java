package com.sb.elsinore.devices;

import com.sb.elsinore.models.I2CSettings;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wrap an I2C device to make it easy to read/write using standard paths and formats
 * Created by doug on 11/07/15.
 */
public abstract class I2CDevice implements Serializable {
    /* /dev/i2c-X ioctl commands.  The ioctl's parameter is always an
     * unsigned long, except for:
     *  - I2C_FUNCS, takes pointer to an unsigned long
     *  - I2C_RDWR, takes pointer to struct i2c_rdwr_ioctl_data
     *  - I2C_SMBUS, takes pointer to struct i2c_smbus_ioctl_data
     */
    public static int I2C_RETRIES = 0x0701;  /* number of times a device address should
                   be polled when not acknowledging */
    public static int I2C_TIMEOUT = 0x0702;  /* set timeout in units of 10 ms */
    public static int I2C_SLAVE_FORCE = 0x0706;  /* Use this slave address, even if it
                   is already in use by a driver! */
    public static int I2C_TENBIT = 0x0704;  /* 0 for 7 bit addrs, != 0 for 10 bit */
    public static int I2C_FUNCS = 0x0705;  /* Get the adapter functionality mask */
    public static int I2C_RDWR = 0x0707;  /* Combined R/W transfer (one STOP only) */
    public static int I2C_PEC = 0x0708;  /* != 0 to use PEC with SMBus */
    public static int I2C_SMBUS = 0x0720;  /* SMBus transfer */
    static String BASE_PATH = "/dev/i2c-%s";
    static int O_RDWR = 0x02;
    /* NOTE: Slave address is 7 or 10 bits, but 10-bit addresses
     * are NOT supported! (due to code brokenness)
     */
    static int I2C_SLAVE = 0x0703;  /* Use this slave address */
    private static int I2C_BUFSIZE = 64;
    private static String[] available_devices = null;
    private static Logger logger = LoggerFactory.getLogger(I2CDevice.class);
    public ads1015error error = ads1015error.ADS1015_ERROR_OK;
    Linux_C_lib libC = new Linux_C_lib_DirectMapping();
    boolean _adsInitialised = false;
    int fd = -1;
    private I2CSettings i2cSettings;
    private String device_path;
    private byte[] I2CMasterBuffer = new byte[I2C_BUFSIZE];
    private byte[] I2CSlaveBuffer = new byte[I2C_BUFSIZE];
    private int I2CReadLength, I2CWriteLength;

    I2CDevice(I2CSettings i2cSettings) {
        this.i2cSettings = i2cSettings;
        init();
    }

    I2CDevice(int deviceNumber, int address, String type) {
        this.i2cSettings = new I2CSettings();
        this.i2cSettings.setDeviceNumber(deviceNumber);
        this.i2cSettings.setAddress(address);
        this.i2cSettings.setDeviceType(type);
        init();
    }

    public static ArrayList<String> getAvailableAddresses(String device_path) {
        int devNo;

        if (device_path.startsWith("/dev/i2c-")) {
            device_path = device_path.replace("/dev/i2c-", "");
        }
        if (device_path.startsWith("i2c-")) {
            device_path = device_path.replace("i2c-", "");
        }

        try {
            devNo = Integer.parseInt(device_path);
        } catch (NumberFormatException nfe) {
            logger.warn("Failed to get a device number from {}", device_path);
            return new ArrayList<>();
        }

        return getAvailableAddresses(devNo);
    }

    /**
     * Get a list of the available addresses on the bus
     *
     * @param device The I2C device to use
     * @return A list of available addresses on the bus
     */
    private static ArrayList<String> getAvailableAddresses(int device) {

        List<String> commands = new ArrayList<>();
        commands.add("i2cdetect");
        commands.add("-r");
        commands.add("-y");
        commands.add("-a");
        commands.add(Integer.toString(device));
        ProcessBuilder pb = new ProcessBuilder(commands);

        pb.redirectErrorStream(true);
        Process process;
        try {
            process = pb.start();
            process.waitFor();
            logger.info(process.getOutputStream().toString());
        } catch (IOException | InterruptedException e3) {
            logger.info("Couldn't check for devices on the I2C bus");
            e3.printStackTrace();
            return new ArrayList<>();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                process.getInputStream()));
        String line;
        ArrayList<String> devices = new ArrayList<>();

        try {
            while ((line = br.readLine()) != null) {
                for (String s : line.split(" ")) {
                    s = s.trim();
                    if (s.equals("--") || s.endsWith(":") || s.equalsIgnoreCase("UU")
                            || s.length() <= 1) {
                        continue;
                    }
                    devices.add(String.format("0x%s", s));
                }
            }
        } catch (IOException e2) {
            logger.info("Couldn't read a line when checking SHA");
            e2.printStackTrace();
            process.destroy();
        }
        return devices;
    }

    public static I2CDevice create(String devNumber, String devAddress, String devType) {
        I2CDevice i2cDevice = null;
        int iDevAddress = 0;
        if (devAddress.startsWith("0x")) {
            iDevAddress = Integer.valueOf(devAddress.substring(2), 16);
        } else {
            iDevAddress = Integer.valueOf(devAddress);
        }

        if (devNumber.startsWith("i2c-")) {
            devNumber = devNumber.replace("i2c-", "");
        }
        if (devType.equals(ADS1015.DEV_NAME)) {
            i2cDevice = new ADS1015(Integer.parseInt(devNumber), iDevAddress);
        } else if (devType.equals(ADS1115.DEV_NAME)) {
            i2cDevice = new ADS1115(Integer.parseInt(devNumber), iDevAddress);
        }
        return i2cDevice;
    }

    public static String[] getAvailableTypes() {
        String[] types = new String[2];
        types[0] = ADS1015.DEV_NAME;
        types[1] = ADS1115.DEV_NAME;
        return types;
    }

    public static String[] getAvailableDevices() {
        if (available_devices == null) {
            ArrayList<String> list = new ArrayList<>();
            File devFile = new File("/dev/");
            if (devFile.isDirectory()) {
                File[] array = devFile.listFiles((file, name) -> name != null && name.startsWith("i2c-"));
                if (array == null) {
                    return null;
                }
                Arrays.stream(array).forEach(f -> list.add(f.getName()));
            }
            available_devices = new String[list.size()];
            available_devices = list.toArray(available_devices);
        }
        return available_devices;
    }

    private void init() {
        this.device_path = String.format(I2CDevice.BASE_PATH, this.i2cSettings.getDeviceNumber());
    }

    public abstract String getDevName();

    public String getDevNumberString() {
        if (getDeviceNumber() != -1) {
            return Integer.toString(getDeviceNumber());
        }
        return "";
    }

    public void setDevice(String dev) {
        this.device_path = dev;
    }

    public String getDevicePath() {
        return this.device_path;
    }

    public void close() {
        if (this.fd > -1) {
            if (this.libC.close(this.fd) != 0) {
                logger.warn("Failed to close: {}", Native.getLastError());
            }
        }
        this.fd = -1;
    }

    ads1015error ads1015WriteRegister(int reg, int value) {
        ads1015error error = ads1015error.ADS1015_ERROR_OK;

        // Clear write buffers
        int i;
        for (i = 0; i < I2C_BUFSIZE; i++) {
            this.I2CMasterBuffer[i] = 0x00;
        }

        this.I2CWriteLength = 3;
        this.I2CReadLength = 0;
        this.I2CMasterBuffer[0] = (byte) reg;                   // Register
        System.out.println(String.format("value: 0x%02x", (byte) (value >> 8)));
        System.out.println(String.format("value: 0x%02x", (byte) (value & 0xFF)));
        this.I2CMasterBuffer[1] = (byte) (value >> 8); //0xC3;            // Upper 8-bits
        this.I2CMasterBuffer[2] = (byte) (value & 0xFF); //0x03;          // Lower 8-bits

        int ret = this.libC.write(this.fd, this.I2CMasterBuffer, this.I2CWriteLength);
        if (ret != this.I2CWriteLength) {
            logger.warn("Failed to write to I2C Device {}. Error {}", ret, Native.getLastError());
            error = ads1015error.ADS1015_ERROR_I2CBUSY;
        }
        // ToDo: Add in proper I2C error-checking
        return error;
    }

    ads1015error ads1015Init() {
        ads1015error error = ads1015error.ADS1015_ERROR_OK;

        // Initialise I2C
        if (!i2cInit()) {
            return ads1015error.ADS1015_ERROR_I2CINIT;    /* Fatal error */
        }

        this._adsInitialised = true;
        return error;

    }

    int ina219Read16(int reg) {

        // Clear write buffers
        for (int i = 0; i < I2C_BUFSIZE; i++) {
            this.I2CMasterBuffer[i] = 0x00;
        }

        this.I2CWriteLength = 2;
        this.I2CReadLength = 2;
        this.I2CSlaveBuffer[0] = (byte) 0x0;           // I2C device address
        this.I2CSlaveBuffer[1] = (byte) 0x0;                       // Command register

        while ((this.I2CSlaveBuffer[0] & 0x80) == 0) {
            this.libC.read(this.fd, this.I2CSlaveBuffer, this.I2CReadLength);
        }
        this.I2CMasterBuffer[0] = 0x0;
        this.libC.write(this.fd, this.I2CMasterBuffer, 1);

        if (this.I2CReadLength != this.libC.read(this.fd, this.I2CMasterBuffer, this.I2CReadLength)) {
            logger.warn("Error reading {}", Native.getLastError());
        }

        int msb = this.I2CMasterBuffer[0];
        int lsb = this.I2CMasterBuffer[1];
        if (lsb < 0) {
            lsb = 256 + lsb;
        }

        return ((msb << 8) + lsb);
        // Shift values to create properly formed integer
        //return (short) ((I2CMasterBuffer[0] << 8) | I2CMasterBuffer[1]);
    }

    public boolean i2cInit() {
        this.fd = this.libC.open(this.device_path, O_RDWR);
        if (this.fd < 0) {
            logger.warn("Failed to read from {}. Error {}", this.device_path, Native.getLastError());
            return false;
        } else {
            logger.warn("Opened fd {}", this.fd);
        }

        int iovalue = this.libC.ioctl(this.fd, I2CDevice.I2C_SLAVE, getAddress());

        if (iovalue < 0) {
            logger.warn("Failed to open slave device at address {}, wrote {}",
                    getAddress(), iovalue);
            logger.warn("Error {}", Native.getLastError());
            return false;
        } else {
            logger.warn("I2C Slave {} opened {}", getAddress(), iovalue);
        }
        return true;
    }

    public abstract float readValue(int devChannel);

    public int getAddress() {
        return this.i2cSettings.getAddress();
    }

    public int getDeviceNumber() {
        return this.i2cSettings.getDeviceNumber();
    }

    enum ads1015error {
        ADS1015_ERROR_OK,                // Everything executed normally
        ADS1015_ERROR_I2CINIT,               // Unable to initialise I2C
        ADS1015_ERROR_I2CBUSY,               // I2C already in use
        ADS1015_ERROR_INVALIDCHANNEL,        // Invalid channel specified
        ADS1015_ERROR_LAST
    }


    public interface Linux_C_lib extends com.sun.jna.Library {

        long memcpy(int[] dst, short[] src, long n);

        int memcpy(int[] dst, short[] src, int n);

        int pipe(int[] fds);

        int tcdrain(int fd);

        int fcntl(int fd, int cmd, int arg);

        int ioctl(int fd, int cmd, int arg);

        int open(String path, int flags);

        int close(int fd);

        int write(int fd, byte[] buffer, int count);

        int read(int fd, byte[] buffer, int count);

        long write(int fd, byte[] buffer, long count);

        long read(int fd, byte[] buffer, long count);

        int select(int n, int[] read, int[] write, int[] error, timeval timeout);

        int poll(int[] fds, int nfds, int timeout);

        int tcflush(int fd, int qs);

        void perror(String msg);

        int tcsendbreak(int fd, int duration);

        class timeval extends Structure {
            public NativeLong tv_sec;
            public NativeLong tv_usec;

            @Override
            protected List getFieldOrder() {
                return Arrays.asList(//
                        "tv_sec",//
                        "tv_usec"//
                );
            }
        }


    }

    public static class Linux_C_lib_DirectMapping implements Linux_C_lib {

        static {
            try {
                Native.register("c");
                logger.warn("OK: Managed to load CLibrary");
            } catch (Exception e) {
                e.printStackTrace();
                logger.warn("BAD: Failed to load CLibrary");
            }
        }

        @Override
        native public long memcpy(int[] dst, short[] src, long n);

        @Override
        native public int memcpy(int[] dst, short[] src, int n);

        @Override
        native public int pipe(int[] fds);

        @Override
        native public int tcdrain(int fd);

        @Override
        native public int fcntl(int fd, int cmd, int arg);

        @Override
        native public int ioctl(int fd, int cmd, int arg);

        @Override
        native public int open(String path, int flags);

        @Override
        native public int close(int fd);

        @Override
        native public int write(int fd, byte[] buffer, int count);

        @Override
        native public int read(int fd, byte[] buffer, int count);

        @Override
        native public long write(int fd, byte[] buffer, long count);

        @Override
        native public long read(int fd, byte[] buffer, long count);

        @Override
        native public int select(int n, int[] read, int[] write, int[] error, timeval timeout);

        @Override
        native public int poll(int[] fds, int nfds, int timeout);

        @Override
        native public int tcflush(int fd, int qs);

        @Override
        native public void perror(String msg);

        @Override
        native public int tcsendbreak(int fd, int duration);
    }
}