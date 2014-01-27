package com.sb.elsinore;
import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PID implements Runnable {
	private OutputControl OC = null;
	private Thread outputThread = null;
	
	// inner class to hold the settings
	public class Settings {
		public double duty_cycle, cycle_time, proportional, integral, derivative, set_point;
		public double calculatedDuty;
		
		public Settings() {
			duty_cycle = cycle_time = proportional = integral = derivative = set_point = 0.0;
		}
	}
	
	/****** 
	 * Create a new PID
	 * @param aTemp Temperature Object to use
	 * @param aName Name of the PID
	 * @param aDuty Duty Cycle % being set
	 * @param aTime Cycle Time in seconds
	 * @param p Proportional value
	 * @param i Integral Value
	 * @param d Differential value
	 * @param GPIO GPIO to be used to control the output
	 */
	public PID (Temp aTemp, String aName, double aDuty, double aTime, double p, double i, double d, String GPIO) {
		mode = "off";
		heatSetting = new Settings();
		heatSetting.set_point = 175;
		heatSetting.duty_cycle = aDuty;
		heatSetting.cycle_time = aTime;
		heatSetting.proportional = p;
		heatSetting.integral = i;
		heatSetting.derivative = d;
		heatSetting.calculatedDuty = 0.0;
		
		fName = aName;
		fTemp = aTemp;
		
		fGPIO = detectGPIO(GPIO);
		
	}
	
	
	private String detectGPIO (String GPIO) {
		// Determine what kind of GPIO Mapping we have
		Pattern pinPattern = Pattern.compile("(GPIO)([0-9])_([0-9]*)");
		Pattern pinPatternAlt = Pattern.compile("(GPIO)?([0-9]*)");
		
		Matcher pinMatcher = pinPattern.matcher(GPIO);
		
		BrewServer.log.info("Matches: " + pinMatcher.groupCount());
		
		if(pinMatcher.groupCount() > 0) {
			// Beagleboard style input
			BrewServer.log.info("Matched GPIO pinout for Beagleboard: " + GPIO + ". OS: " + System.getProperty("os.level"));
			return GPIO;
		} else {
			pinMatcher = pinPatternAlt.matcher(GPIO);
			if(pinMatcher.groupCount() > 0) {
				BrewServer.log.info("Direct GPIO Pinout detected. OS: " + System.getProperty("os.level"));
				// The last group (should be the second group) gives us the GPIO number
				return pinMatcher.group(pinMatcher.groupCount());
			} else {
				BrewServer.log.info("Could not match the GPIO!");
				return "";
			}
		}
	}
	/******
	 * Update the current values of the PID
	 * @param m String indicating mode (manual, auto, off)
	 * @param duty Duty Cycle % being set
	 * @param cycle Cycle Time in seconds
	 * @param setpoint Target temperature for auto mode
	 * @param p Proportional value
	 * @param i Integral Value
	 * @param d Differential value
	 */
	public void updateValues(String m, double duty, double cycle, double setpoint, double p, double i, double d ) {
		mode = m;
		if(mode.equals("manual")) {
			heatSetting.duty_cycle = duty;
		}
		heatSetting.cycle_time = cycle;
		heatSetting.set_point = setpoint;
		BrewServer.log.info(heatSetting.proportional + ": " + heatSetting.integral + ": " + heatSetting.derivative);
		heatSetting.proportional = p;
		heatSetting.integral = i;
		heatSetting.derivative = d;
		BrewServer.log.info(heatSetting.proportional + ": " + heatSetting.integral + ": " + heatSetting.derivative);	
		LaunchControl.savePID(this.fName, heatSetting, fGPIO, auxGPIO);
		return;
	}

	/****
	 * Get the Status of the current PID, heating, off, etc...
	 * @return
	 */
	public synchronized String getStatus() {
		if(OC != null) {
			return OC.getStatus();
		}
		// Output control is broken
		return "No output on! Duty Cyle: " + heatSetting.duty_cycle + " - Temp: " + getTempC();
	}

	
	/******
	 * Main loop for using a PID Thread
	 */
	public void run() {
		BrewServer.log.info( "Running " + fName + " PID." );
		// setup the first time
		previousTime = System.currentTimeMillis();
		// create the Output if needed
		if(!fGPIO.equals("")) {
			OC = new OutputControl(fName, fGPIO, heatSetting.cycle_time);	
			outputThread = new Thread(OC);
			outputThread.start();
		} else {
			return;
		}
		
		// Detect an Auxilliary output
		if (auxGPIO != null && !auxGPIO.equals("")) {
			try {
				auxPin = new OutPin(auxGPIO);
			} catch (InvalidGPIOException e) {
				BrewServer.log.log(Level.SEVERE, "Couldn't parse " + auxGPIO + " as a valid GPIO");
				System.exit(-1);
			} catch (RuntimeException e) {
				BrewServer.log.log(Level.SEVERE, "Couldn't setup " + auxGPIO + " as a valid GPIO");
				System.exit(-1);
			}
		}

		// Main loop
		while (true) {
			try {
				synchronized ( fTemp ) {
					// do the bulk of the work here
					fTemp_C = fTemp.getTempC();
					currentTime = fTemp.getTime();
					fTemp_F = fTemp.getTempF();
					// if the GPIO is blank we do not need to do any of this;
					if(fGPIO != "") {
						if(temp_list.size() >= 5) {
							temp_list.remove(0);
						}	
	
						temp_list.add(fTemp.getTemp());
	
						double temp_avg = calc_average();
	
	
						// we have the current temperature
						if (mode.equals("auto")) {
							heatSetting.calculatedDuty = calcPID_reg4(temp_avg, true);
							BrewServer.log.info("Calculated: " + heatSetting.calculatedDuty);
							OC.setDuty(heatSetting.calculatedDuty);
						} else if (mode.equals("manual")) {
							OC.setDuty(heatSetting.duty_cycle);
						} else if (mode.equals("off")) {
							OC.setDuty(0);
						}
						// determine if the heat needs to be on or off
						
						OC.setHTime(heatSetting.cycle_time);

						BrewServer.log.info(mode + ": " + fName + " status: " + fTemp_F + " duty cycle: " + OC.getDuty());
					}
					//notify all waiters of the change of state
				}

				//pause execution for a seconds
				Thread.sleep( 1000 );
			}
			catch ( InterruptedException ex ){
				System.err.println( ex );
				Thread.currentThread().interrupt();
			}
		}
	}

	/********
	 * Set the duty time in %
	 * @param duty Duty Cycle percentage
	 */
	public void setDuty(double duty) {
		if(duty > 100) {
			duty = 100;
		} else if (duty < -100) {
			duty = -100;
		}

		heatSetting.duty_cycle = duty;
	}

	/****
	 * Set the target temperature for the auto mode
	 * @param temp
	 */
	public void setTemp(double temp) {
		if (temp < 0) {
			temp = 0;
		}

		heatSetting.set_point = temp;
	}

	/*******
	 * set an auxilliary manual GPIO (for dual element systems)
	 * @param gpio
	 */
	public void setAux(String gpio) {
		this.auxGPIO = detectGPIO(gpio);
		
		if (auxGPIO == null || auxGPIO.equals("")) {
			BrewServer.log.log(Level.WARNING, "Could not detect GPIO as valid: " + gpio);
		}
		
	}
	
	public void toggleAux() {
		// Flip the aux pin value
		if (auxPin != null) {
			// If the value if "1" we set it to false
			// If the value is not "1" we set it to true
			BrewServer.log.info("Aux Pin is being set to: "  + !auxPin.getValue().equals("1"));
			auxPin.setValue(!auxPin.getValue().equals("1"));
		} else {
			BrewServer.log.info("Aux Pin is not set for " + this.fName);
		}
	}
	
	public boolean hasAux() {
		return (auxPin != null);
	}
	
	
	/******
	 * Set the proportional value
	 * @param p
	 */
	public void setP(double p) {
		heatSetting.proportional = p;
	}

	/******
	 * Set the integral value
	 * @param i
	 */
	public void setI(double i) {
		heatSetting.integral = i;
	}

	/******
	 * Set the differential value
	 * @param d
	 */
	public void setD(double d) {
		heatSetting.derivative = d;
	}
	
	/*******
	 * Get the current mode
	 * @return
	 */
	public String getMode() {
		return mode;
	}

	/*****
	 * Get the temperature in celsius
	 * @return
	 */
	public double getTempC() {
		return fTemp_C;
	}

	/*****
	 * Get the temperature in fahrenheit
	 * @return
	 */
	public double getTempF() {
		return fTemp_C;
	}
	
	/*****
	 * Get the GPIO Pin
	 * @return
	 */
	public String getGPIO() {
		return fGPIO;
	}

	public String getAuxGPIO() {
		return auxGPIO;
	}
	
	/******
	 * Get the current duty cycle percentage
	 * @return
	 */
	public double getDuty() {
		return heatSetting.duty_cycle;
	}

	/****
	 * Get the current Duty Cycle Time
	 * @return
	 */
	public double getCycle() {
		return heatSetting.cycle_time;
	}

	/******
	 * Get the PID Target tempeture
	 * @return
	 */
	public double getSetPoint() {
		return heatSetting.set_point;
	}

	/******
	 * Get the current proportional value
	 * @return
	 */
	public double getP() {
		return heatSetting.proportional;
	}

	/*******
	 * Get the current Integral value
	 * @return
	 */
	public double getI() {
		return heatSetting.integral;
	}

	/*********
	 * Get the current Differential value
	 * @return
	 */
	public double getD() {
		return heatSetting.derivative;
	}

	/*****
	 * Get the current Temp object
	 * @return
	 */
	public Temp getTempProbe() {
		return fTemp;
	}

	/******
	 * Get the name of this device
	 * @return
	 */
	public String getName() {
		return fTemp.getName();
	}


  //PRIVATE ///
  	private long previousTime = 0;

  	/******
  	 * Calculate the average of the current temp list
  	 * @return
  	 */
	private double calc_average() {
		int size = temp_list.size();
		
		double total =0.0;
		for (double t : temp_list) {
			total += t;
		}
		return total/size;
	}

	private boolean fStatus = false;
	public Temp fTemp;
	private double fTemp_F, fTemp_C;
	private String fGPIO;
	private String auxGPIO = null;
	private List<Double> temp_list = new ArrayList<Double>();
	
	private String mode;
	private String fName;
	private long currentTime = System.currentTimeMillis();
	public Settings heatSetting;
	public Settings coldSetting;
	private OutPin auxPin = null;

	// Temp values for PID calculation
	double errorFactor = 0.0;
	double previous_error = 0.0;
	double integralFactor = 00.0;
	double derivativeFactor = 0.0;
	double output = 0.0;

	double GMA_HLIM = 100.0,
	GMA_LLIM = -100.0;

	/*****
	 * Calculate the current PID Duty
	 * @param avgTemp The current average temperature
	 * @param enable 
	 * @return  A Double of the duty cycle %
	 */
	private double calcPID_reg4(double avgTemp, boolean enable) {
		currentTime = System.currentTimeMillis();
		if(previousTime == 0.0) {
			previousTime = currentTime;
		}
		double dt = (currentTime - previousTime)/1000;
		if(dt == 0.0 || Double.isNaN(dt)) {
			return OC.getDuty();
		}

		errorFactor = heatSetting.set_point - avgTemp;
		integralFactor = (integralFactor - errorFactor) * dt;
		derivativeFactor = (errorFactor - previous_error)/dt;
		
		BrewServer.log.info("DT: " + dt + " Error: " + errorFactor + " integral: " + integralFactor + " derivative: " + derivativeFactor);
		
		
	    output = heatSetting.proportional*errorFactor 
	    		+ heatSetting.integral*integralFactor 
	    		+ heatSetting.derivative*derivativeFactor;
	    
		previous_error = errorFactor;

		// limit y[k] to GMA_HLIM and GMA_LLIM
		if (output > GMA_HLIM) {
			output = GMA_HLIM;
		}
		if (output < GMA_LLIM) {
			output = GMA_LLIM;
		}
		previousTime = currentTime;
		previous_error = errorFactor;
		return output;
        
	}

	/****
	 * Used as a shutdown hook to close off everything
	 */
	public void shutdown() {
		if(OC != null && outputThread != null) {
			outputThread.interrupt();
			while (!outputThread.isInterrupted()) {
				BrewServer.log.warning("Waiting for OC thread to terminate " + getName() );
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					BrewServer.log.warning(getName() + " interrupted during shutdown");
				}
			}
			OC.shutdown();
		}
		
		if (auxPin != null) {
			auxPin.close();
		}
		
		if (this.getName() != null && !getName().equals("")) {
			LaunchControl.savePID(this.getName(), heatSetting, fGPIO, auxGPIO);
		}
		
	}

	// set the cool values
	public void setCool(String GPIO, int duty, double delay, double p, double i, double k) {
		// set the values
		int j = 0;
		while(OC == null) {
			j++;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}

			if(j>10) {
				return;
			}
		}
		OC.setCool(GPIO, duty, delay);
	}
	
	/******
	 * Return a map of the current status
	 * @return
	 */
	public Map<String, Object> getMapStatus() {
		Map<String, Object> statusMap = new HashMap<String, Object>();
		statusMap.put("mode", getMode());
		statusMap.put("gpio", getGPIO());
		// hack to get the real duty out
		if(getMode().contains("auto")) {
			statusMap.put("actualduty", heatSetting.calculatedDuty);
		}
		statusMap.put("duty", getDuty());
		statusMap.put("cycle", getCycle());
		statusMap.put("setpoint", getSetPoint());
		statusMap.put("p", getP());
		statusMap.put("i", getI());
		statusMap.put("d", getD());
		statusMap.put("status", getStatus());
		
		if(auxPin != null) {
			// This value should be cached, but I don't trust someone to hit it with a different application
			statusMap.put("auxStatus", auxPin.getValue());
		}
		
		return statusMap;
	}
}
