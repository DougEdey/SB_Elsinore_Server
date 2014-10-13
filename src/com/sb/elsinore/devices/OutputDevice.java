package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;
import com.sb.util.MathUtil;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;
import java.math.BigDecimal;

/**
 * This class represents a single heating/cooling device that can have a duty applied when it runs.
 * @author Andy
 */
public class OutputDevice {
    
    protected static boolean invertOutput = false;
    protected static BigDecimal HUNDRED = new BigDecimal(100);
    protected static BigDecimal THOUSAND = new BigDecimal(1000);
    
    static
    {
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
    protected String gpio;    //The gpio pin
    
    public OutputDevice(String name, String gpio, BigDecimal cycleTimeSeconds)
    {
        this.name = name;
        setCycleTime(cycleTimeSeconds);
        this.gpio = gpio;
        try
        {
            initializeSSR();
        }
        catch(Exception e)
        {
            
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
    
    protected void initializeSSR() throws InvalidGPIOException
    {
        if(ssr == null) {
            if( gpio != null && gpio.length() > 0 ) {
                ssr = new OutPin(gpio);
            }
        }
    }
    
    public void runCycle(BigDecimal duty) throws InterruptedException, InvalidGPIOException {
        initializeSSR();
        
        duty = MathUtil.divide(duty, HUNDRED);
        BigDecimal onTime = duty.multiply(cycleTime);
        BigDecimal offTime = cycleTime.subtract(onTime);
        BrewServer.LOG.info("On: " + onTime
            + " Off; " + offTime);
        
        setValue(true);
        Thread.sleep(onTime.intValue());

        if( duty.compareTo(HUNDRED) < 0 )
        {
            setValue(false);
            Thread.sleep(offTime.intValue());
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
        this.cycleTime = cycleTime.multiply(THOUSAND);;
    }
    
    
}
