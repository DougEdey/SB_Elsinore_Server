/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sb.elsinore.devices;

import com.sb.elsinore.BrewServer;
import jGPIO.InvalidGPIOException;
import java.math.BigDecimal;

/**
 * This class represents a compressor based device that needs a pause between run cycles.
 * @author Andy
 */
public class CompressorDevice extends OutputDevice {

    protected long lastStopTime = -1L;
    protected long lastStartTime = -1L;
    protected boolean running = false;
    protected long delayBetweenRuns = 1000 * 60 * 60 * 3; // 3 Minutes

    public CompressorDevice(String name, String gpio, BigDecimal cycleTimeSeconds) {
        super(name, gpio, cycleTimeSeconds);
    }

    @Override
    public void runCycle(BigDecimal duty) throws InterruptedException, InvalidGPIOException {
        initializeSSR();

        if (duty.compareTo(HUNDRED) == 0) {
            if (System.currentTimeMillis() - lastStopTime > delayBetweenRuns) {
                if (!running) {
                    BrewServer.LOG.warning("Starting compressor device.");
                    lastStartTime = System.currentTimeMillis();
                }
                running = true;
                setValue(true);
            } else {
                BrewServer.LOG.warning("Need to wait before starting compressor again.");
            }
        }
        Thread.sleep(cycleTime.intValue());
    }

    @Override
    public void turnOff() {
        if (running) {
            lastStopTime = System.currentTimeMillis();
            BrewServer.LOG.warning("Stopping compressor device.");
            BrewServer.LOG.warning("Ran for " + (lastStopTime - lastStartTime) / 60000f + " minutes");
        }
        running = false;
        setValue(false);
    }
}
