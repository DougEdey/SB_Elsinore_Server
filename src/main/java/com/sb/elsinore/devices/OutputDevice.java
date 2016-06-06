package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;
import com.sb.elsinore.PIDSettings;
import com.sb.util.MathUtil;
import com.sun.media.jfxmedia.logging.Logger;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;
import org.w3c.tidy.TidyMessage;

import java.math.BigDecimal;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

/**
 * This class represents a single heating/cooling device that can have a duty
 * applied when it runs.
 *
 * @author Andy
 */
public class OutputDevice implements Observer {

    private boolean invertOutput = false;
    static BigDecimal HUNDRED = new BigDecimal(100);
    private static BigDecimal THOUSAND = new BigDecimal(1000);

    private OutPin ssr = null;    //The output pin.
    private final Object ssrLock = new Object();
    protected String name;    //The name of this device
    protected PIDSettings settings = null;

    public OutputDevice(String name, PIDSettings settings) {
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
        settings.addObserver(this);

        this.name = name;
        this.settings = settings;

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

    void initializeSSR() throws InvalidGPIOException {
        String gpio = settings.getGPIO();
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

    public String getGpio()
    {
        return ssr.getGPIOName();
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
            BigDecimal onTime = duty.multiply(settings.getCycleTime());
            BigDecimal offTime = settings.getCycleTime().subtract(onTime);
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
     * Sets the outputs to be inverted for this device.
     * @param inverted True to invert the output.
     */
    public final void setInverted(final boolean inverted) {
        this.invertOutput = inverted;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof String)
        {
            String propName = (String) arg;
            switch (propName)
            {
                case "cycle_time":
                case "proportional":
                case "integral":
                case "derivative":
                case "delay":
                case "mode":
                case "inverted":
                    Thread.currentThread().interrupt();
                    break;
                case "gpio":
                    try {
                        initializeSSR();
                    } catch (InvalidGPIOException e)
                    {
                        BrewServer.LOG.log(Level.WARNING, "Failed to update GPIO.", e);
                    }
            }
        }
    }
}
