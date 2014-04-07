package com.sb.elsinore;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;
import jGPIO.GPIO;

/**
 * OutputControl controls multiple output GPIOs.
 * Heat_SSR is a GPIO pin that represents a heating output.
 * Cool_SSR is a GPIO pin that represents a cooling output.
 * @author Doug Edey
 *
 */
public final class OutputControl implements Runnable {

    /**
     * Constructor for a heat only pin.
     * @param aName Name of this instance.
     * @param fGPIO the heating GPIO pin.
     * @param heatTime the duty time for the heating output.
     */
   public OutputControl(final String aName, final String fGPIO,
           final double heatTime) {
           // just for heating
        this.fName = aName;
        this.fGPIOh = fGPIO.toLowerCase();
        setHTime(heatTime);
   }

   /**
    * Create a heating and cooling output.
    * @param aName The name of this instance
    * @param gpioHeat The heating GPIO pin.
    * @param heatTime The heating cycle time.
    * @param gpioCool The cooling GPIO pin
    * @param coolTime The cooling cycle time
    * @param inCoolDelay The delay between cooling start/stop calls.
    */
   public OutputControl(final String aName, final String gpioHeat,
           final double heatTime, final String gpioCool,
           final double coolTime, final double inCoolDelay) {
           // just for heating and cooling
        this.fName = aName;
        this.fGPIOc = gpioCool.toLowerCase();
        this.fGPIOh = gpioHeat.toLowerCase();
        setHTime(heatTime);
        setCTime(coolTime);
        setCDelay(inCoolDelay);
   }

   /**
    * Set the current cooling information.
    * @param gpio The GPIO to use for the cooling output
    * @param time The cycle time for the cooling output
    * @param delay The delay between cooling start/stop calls.
    */
   public void setCool(final String gpio, final double time,
           final double delay) {
        fGPIOc = gpio;
        setCTime(time);
        setCDelay(delay);
   }


   /**
    * The main loop that checks and activates the outputs.
    * Based on the duty cycle and time.
    */
   @Override
   public void run() {
        double duty;
        double onTime, offTime;

        try {
            BrewServer.LOG.info(
                "Starting the (" + this.fGPIOh + ") heating output: "
                + GPIO.getPinNumber(this.fGPIOh));
            try {

                if (fGPIOc != null && !this.fGPIOh.equals("")) {
                    this.heatSSR = new OutPin(this.fGPIOh);
                    BrewServer.LOG.info("Started the heating output: "
                        + GPIO.getPinNumber(this.fGPIOh));
                }

                if (this.fGPIOc != null && !this.fGPIOc.equals("")) {
                    this.coolSSR = new OutPin(this.fGPIOc);
                }
            } catch (RuntimeException e) {

                BrewServer.LOG.warning(
                    "Could not control the GPIO Pin. Did you start as root?");
                e.printStackTrace();
                return;
            }

            try {
                while (true) {

                    if (this.fGPIOc != null && !this.fGPIOc.equals("")
                        && this.coolSSR == null) {
                        this.coolSSR = new OutPin(this.fGPIOc);
                    }
                    if (this.fGPIOh != null && !this.fGPIOh.equals("")
                        && this.heatSSR == null) {
                        this.heatSSR = new OutPin(this.fGPIOh);
                    }
                    BrewServer.LOG.info("Fduty: " + this.fDuty);
                    if (this.fDuty == 0) {
                        allOff();
                        Thread.sleep(this.fTimeh);
                    } else if (this.fDuty == 100) {
                        heatOn();
                        Thread.sleep(fTimeh);
                    } else if (this.fDuty == -100) {
                        // check to see if we've slept long enough
                        Long lTime = System.currentTimeMillis() - coolStopTime;

                        if ((lTime / 1000) > this.coolDelay) {
                            // not slept enough
                            break;
                        }

                        coolOn();
                        Thread.sleep(fTimec);
                        allOff();

                        coolStopTime = System.currentTimeMillis();
                    } else if (fDuty > 0) {
                        // calc the on off time
                        duty = fDuty / 100;
                        onTime = duty * fTimeh;
                        offTime = fTimeh * (1 - duty);
                        BrewServer.LOG.info("On: " + onTime
                            + " Off; " + offTime);
                        heatOn();
                        Thread.sleep((int) onTime);

                        allOff();
                        Thread.sleep((int) offTime);
                    } else if (fDuty < 0) {
                        // calc the on off time
                        duty = Math.abs(fDuty) / 100;
                        onTime = duty * fTimec;
                        onTime = duty * fTimec;
                        offTime = fTimec * (1 - duty);

                        coolOn();
                        Thread.sleep((int) onTime);

                        allOff();
                        Thread.sleep((int) offTime);
                        coolStopTime = System.currentTimeMillis();
                    }
                } // end the while loop

            } catch (RuntimeException e) {
                BrewServer.LOG.warning(
                    "Could not control the GPIO Pin during loop."
                    + " Did you start as root?");
                e.printStackTrace();
                return;
            }
        } catch (InterruptedException e) {
            // Sleep interrupted
            coolStopTime = System.currentTimeMillis();
            System.out.print("Wakeup in " + fName);
        } catch (InvalidGPIOException e1) {
            System.out.println(e1.getMessage());
            e1.printStackTrace();
        } finally {
            BrewServer.LOG.warning(fName + " turning off outputs");
            allOff();
        }
    }

    /**
     * Shutdown the thread.
     */
    public void shutdown() {
        BrewServer.LOG.info("Shutting down OC");
        allOff();
        allDisable();
    }

    /**
     * @return The current status of this object.
     */
   public String getStatus() {
        if (coolStatus) {
            return "cooling";
        }

        if (heatStatus) {
            return "heating";
        }
      return "off";
   }

   /**
    * @param duty The duty to set this control with.
    */
    public synchronized void setDuty(final double duty) {
        this.fDuty = duty;
        BrewServer.LOG.info("IN: " + duty + " OUT: " + fDuty);
    }

    /**
     * @param time The heating time in seconds.
     */
    public void setHTime(final double time) {
        // time is coming in in seconds
        this.fTimeh = (int) time * 1000;
    }

    /**
     * @param time The new cooling time in seconds.
     */
    public void setCTime(final double time) {
        // time is coming in in seconds
        this.fTimec = (int) time * 1000;
    }

    /**
     * @param time The cooling delay time in minutes.
     */
    public void setCDelay(final double time) {
        // delay is coming in in minutes
        this.coolDelay = (int) time * 1000 * 60;
    }

    /**
     * Turn the heat output on.
     */
    private void heatOn() {
        this.coolStatus = false;
        this.heatStatus = true;

        if (this.coolSSR == null && this.heatSSR == null) {
            BrewServer.LOG.info("No SSRs to turn on");
        }

        if (this.coolSSR != null) {
            this.coolSSR.setValue(false);
        }

        if (this.heatSSR != null) {
            this.heatSSR.setValue(true);
        }
    }

    /**
     * Turn the cool output on.
     */
    private void coolOn() {
        this.coolStatus = true;
        this.heatStatus = false;

        if (this.heatSSR != null) {
            this.heatSSR.setValue(false);
        }

        if (this.coolSSR != null) {
            this.coolSSR.setValue(true);
        }
    }

    /**
     * Turn off all the ouputs.
     */
    private void allOff() {
        this.coolStatus = false;
        this.heatStatus = false;

        if (this.heatSSR != null) {
            synchronized (this.heatSSR) {
                this.heatSSR.setValue(false);
            }
        }
        if (this.coolSSR != null) {
            synchronized (this.coolSSR) {
                this.coolSSR.setValue(false);
            }
        }
    }

    /**
     * Disable all outputs.
     */
    private void allDisable() {
        if (this.heatSSR != null) {
            synchronized (this.heatSSR) {
                this.heatSSR.close();
                this.heatSSR = null;
            }
        }

        if (this.coolSSR != null) {
            synchronized (this.coolSSR) {
                this.coolSSR.close();
                this.coolSSR = null;
            }
        }
    }

    // PRIVATE ////
    /**
     * The heating output pin.
     */
    private OutPin heatSSR = null;
    /**
     * The cooling output pin.
     */
    private OutPin coolSSR = null;
    /**
     * The name of this output.
     */
    private String fName;
    /**
     * The raw GPIO heating pin name.
     */
    private String fGPIOh = "";
    /**
     * The raw GPIO cooling pin name.
     */
    private String fGPIOc = "";
    /**
     * The heating cycle time.
     */
    private int fTimeh;
    /**
     * The cooling cycle time.
     */
    private int fTimec;
    /**
     * The delay between cooling start/stop calls.
     */
    private int coolDelay;
    /**
     * The Duty cycle.
     */
    private double fDuty;
    /**
     * The cooling status flag.
     */
    private boolean coolStatus = false;
    /**
     * The Heating status flag.
     */
    private boolean heatStatus = false;
    /**
     * The timestamp for when the cooling element was stopped.
     */
    private long coolStopTime = -1;

    /**
     * @return The current duty cycle
     */
    public synchronized double getDuty() {
        return fDuty;
    }
}

