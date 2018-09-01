package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;
import com.sb.elsinore.interfaces.OutputDeviceInterface;
import com.sb.elsinore.interfaces.PIDSettingsInterface;
import com.sb.elsinore.models.OutputDeviceModel;
import com.sb.util.MathUtil;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

import java.math.BigDecimal;

/**
 * This class represents a single heating/cooling device that can have a duty
 * applied when it runs.
 *
 * @author Andy
 */
public class OutputDevice {

    static BigDecimal HUNDRED = new BigDecimal(100);
    private final Object ssrLock = new Object();
    protected PIDSettingsInterface settings = null;
    private boolean invertOutput = false;
    private OutPin ssr = null;    //The output pin.

    private OutputDeviceInterface outputDeviceInterface;

    public OutputDevice(String name, String type, PIDSettingsInterface pidSettings) {
        this.outputDeviceInterface = new OutputDeviceModel();
        this.outputDeviceInterface.setName(name);
        this.outputDeviceInterface.setType(type);
        this.outputDeviceInterface.setPIDSettings(pidSettings);
    }

    public OutputDevice(OutputDeviceInterface outputDeviceInterface) {
        this.outputDeviceInterface = outputDeviceInterface;
        // Check for inverted outputs using a property.
        try {
            String invOut = System.getProperty("invert_outputs");

            if (invOut != null) {
                BrewServer.LOG.warning("Inverting outputs");
                this.invertOutput = true;
            }
        } catch (Exception e) {
            // Incase get property fails
        }

        try {
            initializeSSR();
        } catch (Exception e) {
            BrewServer.LOG.warning("Unable to initialize the SSR: " + e.getMessage());
        }
    }

    public void disable() {
        if (this.ssr != null) {
            synchronized (this.ssrLock) {
                this.ssr.close();
                this.ssr = null;
            }
        }
    }

    public void turnOff() {
        setValue(false);
    }

    void initializeSSR() throws InvalidGPIOException {
        String gpio = this.settings.getGPIO();
        if (this.ssr == null) {
            if (gpio != null && gpio.length() > 0) {
                synchronized (this.ssrLock) {
                    this.ssr = new OutPin(gpio);
                }
                turnOff();
            }
        }

        if (gpio == null && this.ssr != null) {
            disable();
        }
    }

    public String getGpio() {
        if (this.ssr == null) {
            return null;
        }
        return this.ssr.getGPIOName();
    }

    /**
     * Run through a cycle and turn the device on/off as appropriate based on the input duty.
     *
     * @param duty The percentage of time / power to run.  This will only run if the duty
     *             is between 0 and 100 and not null.
     */
    public void runCycle(BigDecimal duty) throws InterruptedException, InvalidGPIOException {
        // Run if the duty is not null and is between 0 and 100 inclusive.
        if (duty != null
                && duty.compareTo(BigDecimal.ZERO) > 0
                && duty.compareTo(HUNDRED) <= 0) {
            initializeSSR();

            duty = MathUtil.divide(duty, HUNDRED);
            BigDecimal onTime = duty.multiply(this.settings.getCycleTime());
            BigDecimal offTime = this.settings.getCycleTime().subtract(onTime);
            BrewServer.LOG.info("On: " + onTime
                    + " Off; " + offTime);

            if (onTime.intValue() > 0) {
                setValue(true);
                Thread.sleep(onTime.intValue());
            }

            if (duty.abs().compareTo(HUNDRED) < 0 && offTime.intValue() > 0) {
                setValue(false);
                Thread.sleep(offTime.intValue());
            }
        }
    }

    protected void setValue(boolean value) {
        if (this.ssr != null) {
            synchronized (this.ssrLock) {
                // invert the output if needed
                if (this.invertOutput) {
                    value = !value;
                }
                this.ssr.setValue(value);
            }
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.outputDeviceInterface.getName();
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.outputDeviceInterface.setName(name);
    }

    /**
     * Sets the outputs to be inverted for this device.
     *
     * @param inverted True to invert the output.
     */
    public final void setInverted(final boolean inverted) {
        this.invertOutput = inverted;
    }
}
