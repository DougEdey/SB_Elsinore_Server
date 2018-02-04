package com.sb.elsinore;

import com.sb.elsinore.devices.CompressorDevice;
import com.sb.elsinore.devices.OutputDevice;
import jGPIO.InvalidGPIOException;

import java.math.BigDecimal;

/**
 * OutputControl controls multiple output GPIOs.
 * Heat_SSR is a GPIO pin that represents a heating output.
 * Cool_SSR is a GPIO pin that represents a cooling output.
 *
 * @author Doug Edey
 */
public final class OutputControl implements Runnable {

    public boolean shuttingDown = false;
    private OutputDevice cooler = null;
    private OutputDevice heater = null;

    /**
     * The Duty cycle.
     * Initialized to ZERO to prevent use of duty before it is set.
     */
    private BigDecimal fDuty = BigDecimal.ZERO;

    private String status = "off";

    public OutputControl() {
    }

    /**
     * Constructor for a heat only pin.
     *
     * @param name     Name of this instance.
     * @param settings The Pid Settings to created the heater output for
     */
    public OutputControl(final String name, PIDSettings settings) {
        this.heater = new OutputDevice(name, settings);
    }

    /**
     * Set the current cooling information.
     */
    public void setCool(PIDSettings settings) {

        //If there is a cooling delay between cycles,
        //then assume this is a compressor device
        if (BigDecimal.ZERO.compareTo(settings.getCycleTime()) == -1) {
            CompressorDevice coolDevice =
                    new CompressorDevice("cooler", settings);
            setCooler(coolDevice);
        } else {
            setCooler(new OutputDevice("cooler", settings));
        }

    }


    /**
     * The main loop that checks and activates the outputs.
     * Based on the duty cycle and time.
     */
    @Override
    public void run() {
        try {
            while (true) {

                try {

                    if (this.fDuty.compareTo(BigDecimal.ZERO) == 0) {
                        this.status = "off";
                        if (getHeater() != null) {
                            getHeater().turnOff();
                        }
                        if (getCooler() != null) {
                            getCooler().turnOff();
                        }
                        //Need to sleep because we're not running a cycle
                        Thread.sleep(1000);
                    } else if (this.fDuty.compareTo(BigDecimal.ZERO) < 0) {
                        this.status = "cooling";
                        if (getHeater() != null) {
                            getHeater().turnOff();
                        }
                        if (getCooler() != null) {
                            getCooler().runCycle(this.fDuty.abs());
                        }
                    } else {
                        this.status = "heating";
                        if (getCooler() != null) {
                            getCooler().turnOff();
                        }
                        if (getHeater() != null) {
                            getHeater().runCycle(this.fDuty);
                        }
                    }
                } catch (InterruptedException e) {
                    // Sleep interrupted, why did we wakeup
                    if (this.shuttingDown) {
                        return;
                    }
                }
            } // end the while loop

        } catch (RuntimeException e) {
            BrewServer.LOG.warning(
                    "Could not control the GPIO Pin during loop."
                            + " Did you start as root?");
            e.printStackTrace();
        } catch (InvalidGPIOException e1) {
            BrewServer.LOG.warning(e1.getMessage());
            e1.printStackTrace();
        } finally {
            BrewServer.LOG.warning("Output Control turning off outputs");
            if (getHeater() != null) {
                getHeater().turnOff();
            }
            if (getCooler() != null) {
                getCooler().turnOff();
            }
        }
    }

    /**
     * Shutdown the thread.
     */
    public void shutdown() {
        BrewServer.LOG.info("Shutting down OC");
        if (getHeater() != null) {
            getHeater().turnOff();
            getHeater().disable();
        }
        if (getCooler() != null) {
            getCooler().turnOff();
            getCooler().disable();
        }
    }

    /**
     * @return The current status of this object.
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * @param duty The duty to set this control with.
     */
    public synchronized boolean setDuty(BigDecimal duty) {
        // Fix Defect #28: Cap the duty as positive or negative.
        if (this.cooler == null && duty.compareTo(BigDecimal.ZERO) < 0) {
            duty = BigDecimal.ZERO;
        }

        if (this.heater == null && duty.compareTo(BigDecimal.ZERO) > 0) {
            duty = BigDecimal.ZERO;
        }
        if (this.fDuty.compareTo(duty) == 0) {
            return false;
        }
        this.fDuty = duty;
        BrewServer.LOG.info("IN: " + duty + " OUT: " + this.fDuty);
        return true;
    }

    /**
     * @return The current duty cycle
     */
    public synchronized BigDecimal getDuty() {
        return this.fDuty;
    }

    /**
     * @return the cooler
     */
    public OutputDevice getCooler() {
        return this.cooler;
    }

    /**
     * @param cooler the cooler to set
     */
    public void setCooler(OutputDevice cooler) {
        this.cooler = cooler;
    }

    /**
     * @return the heater
     */
    public OutputDevice getHeater() {
        return this.heater;
    }

    /**
     * @param heater the heater to set
     */
    public void setHeater(OutputDevice heater) {
        this.heater = heater;
    }
}

