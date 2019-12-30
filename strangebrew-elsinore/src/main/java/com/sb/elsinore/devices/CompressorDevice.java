package com.sb.elsinore.devices;

import com.sb.elsinore.interfaces.PIDSettingsInterface;
import main.java.jGPIO.InvalidGPIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * This class represents a compressor based device that needs a pause between
 * run cycles.
 *
 * @author Andy
 */
public class CompressorDevice extends OutputDevice {

    public static String TYPE = "compressor";
    protected long lastStopTime = -1L;
    protected long lastStartTime = -1L;
    protected boolean running = false;
    protected long delayBetweenRuns = 1000 * 60 * 3; // 3 Minutes
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public CompressorDevice(String name, PIDSettingsInterface settings) {
        super(name, TYPE, settings);
    }


    /**
     * Run through a cycle and turn the device on/off as appropriate based on the input duty.
     *
     * @param duty The percentage of time / power to run.  This will only run if the duty
     *             is between 0 and 100 and not null.
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
                        this.logger.warn("Starting compressor device.");
                        this.lastStartTime = System.currentTimeMillis();
                    }
                    this.running = true;
                    setValue(true);
                } else {
                    this.logger.warn("Need to wait before starting compressor again: {}", (this.delayBetweenRuns - (System.currentTimeMillis() - this.lastStopTime)));
                }
            }
            Thread.sleep(this.settings.getCycleTime().intValue());
        }
    }

    @Override
    public void turnOff() {
        if (this.running) {
            this.lastStopTime = System.currentTimeMillis();
            this.logger.warn("Stopping compressor device.");
            this.logger.warn("Ran for {} minutes", (this.lastStopTime - this.lastStartTime) / 60000f);
        }
        this.running = false;
        setValue(false);
    }

    public void setDelay(BigDecimal delay) {
        this.delayBetweenRuns = delay.longValue() * 1000 * 60;
    }
}
