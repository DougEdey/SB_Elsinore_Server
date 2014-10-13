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

    protected static boolean invertOutput = false;
    protected static BigDecimal HUNDRED = new BigDecimal(100);
    protected static BigDecimal THOUSAND = new BigDecimal(1000);

    static {
        try {
            String invOut = System.getProperty("invert_outputs");

            if (invOut != null) {
                System.out.println("Inverting outputs");
                invertOutput = true;
            }
        } catch (Exception e) {
            // Incase get property fails
        }
    }
    protected BigDecimal cycleTime = new BigDecimal(5000);    //5 second default
    protected OutPin ssr = null;    //The output pin.
    protected String name;    //The name of this device
    private String gpio;    //The gpio pin

    public OutputDevice(String name, String gpio, BigDecimal cycleTimeSeconds) {
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
            synchronized (ssr) {
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
                ssr = new OutPin(gpio);
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
        if (duty != null && 
            duty.compareTo(BigDecimal.ZERO) > -1 &&
            duty.compareTo(HUNDRED) < 1) {
            initializeSSR();

            duty = MathUtil.divide(duty, HUNDRED);
            BigDecimal onTime = duty.multiply(cycleTime);
            BigDecimal offTime = cycleTime.subtract(onTime);
            BrewServer.LOG.info("On: " + onTime
                    + " Off; " + offTime);

            setValue(true);
            Thread.sleep(onTime.intValue());

            if (duty.compareTo(HUNDRED) < 0) {
                setValue(false);
                Thread.sleep(offTime.intValue());
            }
        }
    }

    protected void setValue(boolean value) {
        if (this.ssr != null) {

            // invert the output if needed
            if (this.invertOutput) {
                value = !value;
            }
            synchronized (ssr) {
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
     * @return the cycleTime
     */
    public BigDecimal getCycleTime() {
        return this.cycleTime.divide(THOUSAND);
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
     * @return the gpio
     */
    public String getGpio(){
        return gpio;
    }

    /**
     * @param gpio the gpio to set
     */
    public void setGpio(String gpio){
        this.gpio = gpio;
    }
}
