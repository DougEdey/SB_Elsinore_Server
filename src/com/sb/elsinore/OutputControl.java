package com.sb.elsinore;
import com.sb.elsinore.devices.CompressorDevice;
import com.sb.elsinore.devices.OutputDevice;
import java.math.BigDecimal;

import jGPIO.InvalidGPIOException;

/**
 * OutputControl controls multiple output GPIOs.
 * Heat_SSR is a GPIO pin that represents a heating output.
 * Cool_SSR is a GPIO pin that represents a cooling output.
 * @author Doug Edey
 *
 */
public final class OutputControl implements Runnable {

    public boolean shuttingDown = false;
    
    private OutputDevice cooler = null;
    private OutputDevice heater = null;
    
    /**
     * The Duty cycle.  Initialized to ZERO to prevent use of duty before it is set.
     */
    private BigDecimal fDuty = BigDecimal.ZERO;
    
    private String status = "off";
    
    public OutputControl() {
        
    }
    /**
     * Constructor for a heat only pin.
     * @param aName Name of this instance.
     * @param fGPIO the heating GPIO pin.
     * @param cycle_time the duty time for the heating output.
     */
   public OutputControl(final String aName, final String fGPIO,
           final BigDecimal cycle_time) {
           // just for heating
        heater = new OutputDevice(aName, fGPIO, cycle_time);
        //cooler = new OutputDevice(aName, null, cycle_time);
        

   }

   /**
    * Set the current cooling information.
    * @param gpio The GPIO to use for the cooling output
    * @param cycle_time The cycle time for the cooling output
    * @param delay The delay between cooling start/stop calls.
    */
   public void setCool(final String gpio, final BigDecimal cycle_time,
           final BigDecimal delay) {
       
        //If there is a cooling delay between cycles, then assume this is a compressor device
        if( BigDecimal.ZERO.compareTo(delay) == -1 ) {
            CompressorDevice coolDevice = new CompressorDevice("cooler", gpio, cycle_time);
            coolDevice.setDelay(delay);
            setCooler(coolDevice);
        }
        else {
            setCooler(new OutputDevice("cooler", gpio, cycle_time));
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
                     BrewServer.LOG.info("Fduty: " + this.fDuty);
                     switch(fDuty.compareTo(BigDecimal.ZERO))
                     {
                         case 0:
                             status = "off";
                             if (getHeater() != null) 
                                 getHeater().turnOff();
                             if (getCooler() != null)
                                 getCooler().turnOff();
                             //Need to sleep because we're not running a cycle
                             Thread.sleep(1000);
                             break;
                         case -1: //Less than 0
                             status = "cooling";
                             if (getHeater() != null)
                                 getHeater().turnOff();
                             if (getCooler() != null)
                                 getCooler().runCycle(fDuty.negate());
                             break;
                         case 1: //Greater than 0
                             status = "heating";
                             if (getCooler() != null)
                                 getCooler().turnOff();
                             if (getHeater() != null)
                                 getHeater().runCycle(fDuty);
                             break;
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
         catch (InvalidGPIOException e1) {
             System.out.println(e1.getMessage());
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
        return status;
   }

   /**
    * @param duty The duty to set this control with.
    */
    public synchronized void setDuty(BigDecimal duty) {
        // Fix Defect #28: Cap the duty as positive or negative.
        if (this.cooler == null && duty.compareTo(BigDecimal.ZERO) < 0) {
            duty = BigDecimal.ZERO;
        }

        if (this.heater == null && duty.compareTo(BigDecimal.ZERO) > 0) {
            duty = BigDecimal.ZERO;
        }
        this.fDuty = duty;
        BrewServer.LOG.info("IN: " + duty + " OUT: " + fDuty);
    }

    /**
     * @return The current duty cycle
     */
    public synchronized BigDecimal getDuty() {
        return fDuty;
    }

    /**
     * @return the cooler
     */
    public OutputDevice getCooler() {
        return cooler;
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
        return heater;
    }

    /**
     * @param heater the heater to set
     */
    public void setHeater(OutputDevice heater) {
        this.heater = heater;
    }
}

