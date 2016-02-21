package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;
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

    protected boolean invertOutput = false;
    protected static BigDecimal HUNDRED = new BigDecimal(100);
    protected static BigDecimal THOUSAND = new BigDecimal(1000);

    protected BigDecimal cycleTime = new BigDecimal(5000);    //5 second default
    protected OutPin ssr = null;    //The output pin.
    private final Object ssrLock = new Object();
    protected String name;    //The name of this device
    private String gpio;    //The gpio pin

    public OutputDevice(String name, String gpio, BigDecimal cycleTimeSeconds) {
        // Check for inverted outputs using a property.
        try {
            String invOut = System.getProperty("invert_outputs");

            if (invOut != null) {
                BrewServer.LOG.warning("Inverting outputs");
                invertOutput = true;
            }
        } catch (Exception e) {
            // Incase get property fails
        }

        this.name = name;
        setCycleTime(cycleTimeSeconds);
        this.gpio = gpio;
        try {
            initializeSSR();
        } catch (Exception e) {
            BrewServer.LOG.warning("Unable to initialize the SSR: " + e.getMessage());
        }
    }

    public void disable() {
        if (ssr != null) {
            synchronized (ssrLock) {
                ssr.close();
                ssr = null;
            }
        }
    }

    public void turnOff() {
        setValue(false);
    }

    protected void initializeSSR() throws InvalidGPIOException {
        if (ssr == null) {
            if (gpio != null && gpio.length() > 0) {
                synchronized (ssrLock) {
                    ssr = new OutPin(gpio);
                }
                turnOff();
            }
        }

        if (gpio == null && ssr != null) {
            disable();
        }
    }

    
    /**
     * Run through a cycle and turn the device on/off as appropriate based on the input duty.
     * @param duty The percentage of time / power to run.  This will only run if the duty
     *              is between 0 and 100 and not null.
     */
    public void runCycle(BigDecimal duty) throws InterruptedException, InvalidGPIOException {
        // Run if the duty is not null and is between 0 and 100 inclusive.
        if (duty != null
                && duty.compareTo(BigDecimal.ZERO) > 0
                && duty.compareTo(HUNDRED) <= 0) {
            initializeSSR();

            duty = MathUtil.divide(duty, HUNDRED);
            BigDecimal onTime = duty.multiply(cycleTime);
            BigDecimal offTime = cycleTime.subtract(onTime);
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
            synchronized (ssrLock) {
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
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param cycleTime the cycleTime to set
     */
    public void setCycleTime(BigDecimal cycleTime) {
        if (cycleTime != null) {
            this.cycleTime = cycleTime.multiply(THOUSAND);
        }
    }

    /**
     * @return the GPIO.
     */
    public final String getGpio() {
        return gpio;
    }

    /**
     * @param gpioIn the GPIO to set.
     */
    public final void setGpio(final String gpioIn){
        this.gpio = gpioIn;
    }

    /**
     * Sets the outputs to be inverted for this device.
     * @param inverted True to invert the output.
     */
    public final void setInverted(final boolean inverted) {
        this.invertOutput = inverted;
    }

}
