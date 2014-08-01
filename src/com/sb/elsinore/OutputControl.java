package com.sb.elsinore;
import com.sb.util.MathUtil;
import java.math.BigDecimal;

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
     * Private magic numbers.
     */
    private static BigDecimal THOUSAND = new BigDecimal(1000);
    private static BigDecimal SIXTY = new BigDecimal(60);
    private static BigDecimal HUNDRED = new BigDecimal(100);
    
    private boolean invertOutput = false;
    public boolean shuttingDown = false;
    
    /**
     * Constructor for a heat only pin.
     * @param aName Name of this instance.
     * @param fGPIO the heating GPIO pin.
     * @param cycle_time the duty time for the heating output.
     */
   public OutputControl(final String aName, final String fGPIO,
           final BigDecimal cycle_time) {
           // just for heating
        this.fName = aName;
        this.fGPIOh = fGPIO.toLowerCase();
        
        String invOut = null;
        
        try {
            invOut = System.getProperty("invert_outputs");

            if (invOut != null) {
                System.out.println("Inverting outputs");
                invertOutput = true;
            }
        } catch (Exception e) {
            // Incase get property fails
            
        }
        setHTime(cycle_time);
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
           final BigDecimal heatTime, final String gpioCool,
           final BigDecimal coolTime, final BigDecimal inCoolDelay) {
           // just for heating and cooling
        this.fName = aName;
        this.fGPIOc = gpioCool.toLowerCase();
        this.fGPIOh = gpioHeat.toLowerCase();
        setHTime(heatTime);
        setCTime(coolTime);
        setCDelay(inCoolDelay);

        String temp = null;

        try {
            temp = System.getProperty("invert_outputs");
        } catch (Exception e) {
            // Incase get property fails
        }

        if (temp != null) {
            invertOutput = true;
        }

   }

   /**
    * Set the current cooling information.
    * @param gpio The GPIO to use for the cooling output
    * @param duty The cycle time for the cooling output
    * @param delay The delay between cooling start/stop calls.
    */
   public void setCool(final String gpio, final BigDecimal duty,
           final BigDecimal delay) {
        fGPIOc = gpio;
        setCTime(duty);
        setCDelay(delay);
   }


   /**
    * The main loop that checks and activates the outputs.
    * Based on the duty cycle and time.
    */
   @Override
   public void run() {
        BigDecimal duty;
        BigDecimal onTime, offTime;

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
                    try {
                        BrewServer.LOG.info("Fduty: " + this.fDuty);
                        if (this.fDuty.compareTo(BigDecimal.ZERO) == 0) {
                            allOff();
                            Thread.sleep(this.fTimeh.intValue());
                        } else if (this.fDuty.compareTo(HUNDRED) == 0) {
                            heatOn();
                            Thread.sleep(fTimeh.intValue());
                        } else if (this.fDuty.compareTo(
                                HUNDRED.negate()) == 0) {
                            // check to see if we've slept long enough
                            BigDecimal lTime = new BigDecimal(
                                    System.currentTimeMillis())
                                .subtract(coolStopTime);

                            if (MathUtil.divide(lTime, THOUSAND)
                                    .compareTo(this.coolDelay) > 0) {
                                // not slept enough
                                break;
                            }

                            coolOn();
                            Thread.sleep(fTimec.intValue());
                            allOff();

                            coolStopTime = BigDecimal.valueOf(
                                    System.currentTimeMillis());
                        } else if (fDuty.compareTo(BigDecimal.ZERO) > 0) {
                            if (this.heatSSR != null) {
                                // calc the on off time
                                duty = MathUtil.divide(fDuty, HUNDRED);
                                onTime = duty.multiply(fTimeh);
                                offTime = fTimeh.multiply(
                                        BigDecimal.ONE.subtract(duty));
                                BrewServer.LOG.info("On: " + onTime
                                    + " Off; " + offTime);
                                heatOn();
                                Thread.sleep(onTime.intValue());

                                allOff();
                                Thread.sleep(offTime.intValue());
                            } else {
                                fDuty = BigDecimal.ZERO;
                            }
                        } else if (fDuty.compareTo(BigDecimal.ZERO) < 0) {
                            if (this.coolSSR != null) {
                                // calc the on off time
                                duty = MathUtil.divide(fDuty.abs(), HUNDRED);
                                onTime = duty.multiply(fTimec);
                                offTime = fTimec.multiply(
                                        BigDecimal.ONE.subtract(duty));

                                coolOn();
                                Thread.sleep(onTime.intValue());

                                allOff();
                                Thread.sleep(offTime.intValue());
                                coolStopTime = new BigDecimal(
                                        System.currentTimeMillis());
                            } else {
                                fDuty = BigDecimal.ZERO;
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
                return;
            }
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
    public synchronized void setDuty(final BigDecimal duty) {
        this.fDuty = duty;
        BrewServer.LOG.info("IN: " + duty + " OUT: " + fDuty);
    }

    /**
     * @param time The heating time in seconds.
     */
    public void setHTime(final BigDecimal time) {
        // time is coming in in seconds
        this.fTimeh = time.multiply(THOUSAND);
    }

    /**
     * @param time The new cooling time in seconds.
     */
    public void setCTime(final BigDecimal time) {
        // time is coming in in seconds
        this.fTimec = time.multiply(THOUSAND);
    }

    /**
     * @param time The cooling delay time in minutes.
     */
    public void setCDelay(final BigDecimal time) {
        // delay is coming in in minutes
        this.coolDelay = time.multiply(THOUSAND).multiply(SIXTY);
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
            this.coolSet(false);
        }

        if (this.heatSSR != null) {
            this.heatSet(true);
        }
    }

    /**
     * Turn the cool output on.
     */
    private void coolOn() {
        this.coolStatus = true;
        this.heatStatus = false;

        if (this.heatSSR != null) {
            this.heatSet(false);
        }

        if (this.coolSSR != null) {
            this.coolSet(true);
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
                this.heatSet(false);
            }
        }
        if (this.coolSSR != null) {
            synchronized (this.coolSSR) {
                this.coolSet(false);
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
    private BigDecimal fTimeh = BigDecimal.ZERO;
    /**
     * The cooling cycle time.
     */
    private BigDecimal fTimec = BigDecimal.ZERO;
    /**
     * The delay between cooling start/stop calls.
     */
    private BigDecimal coolDelay = new BigDecimal(1000*60); //One Minute Default to prevent code from erroring out when this is not set.
    /**
     * The Duty cycle.  Initialized to ZERO to prevent use of duty before it is set.
     */
    private BigDecimal fDuty = BigDecimal.ZERO;
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
    private BigDecimal coolStopTime = new BigDecimal(0);

    /**
     * @return The current duty cycle
     */
    public synchronized BigDecimal getDuty() {
        return fDuty;
    }

    private void coolSet(boolean value) {
        if (this.coolSSR == null) {
            return;
        }

        // invert the output if needed
        if (this.invertOutput) {
            value = !value;
        }

        this.coolSSR.setValue(value);
    }

    private void heatSet(boolean value) {
        if (this.heatSSR == null) {
            return;
        }

        // invert the output if needed
        if (this.invertOutput) {
            value = !value;
        }
        
        this.heatSSR.setValue(value);
    }
}

