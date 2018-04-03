package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;
import com.sb.elsinore.PIDSettings;
import jGPIO.InvalidGPIOException;
import java.math.BigDecimal;

/**
 * This class represents a compressor based device that needs a pause between
 * run cycles.
 *
 * @author Andy
 */
public class CompressorDevice extends OutputDevice {

    protected long lastStopTime = -1L;
    protected long lastStartTime = -1L;
    protected boolean running = false;
    protected long delayBetweenRuns = 1000 * 60 * 3; // 3 Minutes

    public CompressorDevice(String name, PIDSettings settings) {
        super(name, settings);
    }

    
    /**
     * Run through a cycle and turn the device on/off as appropriate based on the input duty.
     * @param duty The percentage of time / power to run.  This will only run if the duty
     *              is between 0 and 100 and not null.
     */
    @Override
    public void runCycle(BigDecimal duty) throws InterruptedException, InvalidGPIOException {
        // Run if the duty is not null and is between 0 and 100 inclusive.
        if (duty != null && 
            duty.compareTo(BigDecimal.ZERO) > -1 &&
            duty.compareTo(HUNDRED) < 1) {
            initializeSSR();

            if (duty.compareTo(HUNDRED) == 0) {
                if (System.currentTimeMillis() - this.lastStopTime > this.delayBetweenRuns) {
                    if (!this.running) {
                        BrewServer.LOG.warning("Starting compressor device.");
                        this.lastStartTime = System.currentTimeMillis();
                    }
                    this.running = true;
                    setValue(true);
                } else {
                    BrewServer.LOG.warning("Need to wait before starting compressor again.: "+(this.delayBetweenRuns - (System.currentTimeMillis() - this.lastStopTime)));
                }
            }
            Thread.sleep(this.settings.getCycleTime().intValue());
        }
    }

    @Override
    public void turnOff() {
        if (this.running) {
            this.lastStopTime = System.currentTimeMillis();
            BrewServer.LOG.warning("Stopping compressor device.");
            BrewServer.LOG.warning("Ran for " + (this.lastStopTime - this.lastStartTime) / 60000f + " minutes");
        }
        this.running = false;
        setValue(false);
    }
    
    public void setDelay(BigDecimal delay)
    {
        this.delayBetweenRuns = delay.longValue() * 1000 * 60;
    }
}
