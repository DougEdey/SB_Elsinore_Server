package com.sb.elsinore;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PID implements Runnable {
	private OutputControl OC = null;

	// inner class to hold the settings
	public class Settings {
		public double duty_cycle, cycle_time, proportional, integral, derivative, set_point;
		public double calculatedDuty;
		
		public Settings() {
			duty_cycle = cycle_time = proportional = integral = derivative = set_point = 0.0;
		}
	}
	
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
		Pattern pinPattern = Pattern.compile("(GPIO)([0-9])_([0-9]*)");
		Pattern pinPatternAlt = Pattern.compile("(GPIO)?([0-9]*)");
		
		Matcher pinMatcher = pinPattern.matcher(GPIO);
		
		BrewServer.log.info("Matches: " + pinMatcher.groupCount());
		
		if(pinMatcher.groupCount() > 0) {
			// Beagleboard style input
			BrewServer.log.info("Matched GPIO pinout for Beagleboard: " + GPIO + ". OS: " + System.getProperty("os.level"));
			fGPIO = GPIO;
		} else {
			pinMatcher = pinPatternAlt.matcher(GPIO);
			if(pinMatcher.groupCount() > 0) {
				BrewServer.log.info("Direct GPIO Pinout detected. OS: " + System.getProperty("os.level"));
				fGPIO = pinMatcher.group(pinMatcher.groupCount());
			} else {
				BrewServer.log.info("Could not match the GPIO!");
				fGPIO = "";
			}
		}
		
		
	}
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
		LaunchControl.savePID(this.fName, heatSetting);
		return;
	}

	public synchronized String getStatus() {
		if(OC != null) {
			return OC.getStatus();
		}
		return "No output on! Duty Cyle: " + heatSetting.duty_cycle + " - Temp: " + getTemp();
	}

	public void run() {
		BrewServer.log.info( "Running " + fName + " PID." );
		// setup the first time
		previousTime = System.currentTimeMillis();
		// create the Output if needed
		if(!fGPIO.equals("")) {
			OC = new OutputControl(fName, fGPIO, heatSetting.cycle_time);	
			Thread outputThread = new Thread(OC);
			outputThread.start();
		} else {
			return;
		}

		
		while (true) {
			try {
				synchronized ( fTemp ) {
					// do the bulk of the work here
					fTemp_C = fTemp.getTempC();
					currentTime = fTemp.getTime();
					fTemp_F = fTemp.getTempF();
					// if the GPIO is blank we do not need to do any of this;
					if(fGPIO != "") {
						if(temp_F_list.size() >= 5) {
							temp_F_list.remove(0);
						}	
	
						temp_F_list.add(fTemp_F);
	
						double temp_F_avg = calc_average();
	
	
						// we have the current temperature
						if (mode.equals("auto")) {
							heatSetting.calculatedDuty = calcPID_reg4(temp_F_avg, true);
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

				//pause execution for a few seconds
				Thread.sleep( 1000 );
			}
			catch ( InterruptedException ex ){
				System.err.println( ex );
				Thread.currentThread().interrupt();
			}
		}
	}

	public void setDuty(double duty) {
		if(duty > 100) {
			duty = 100;
		} else if (duty < -100) {
			duty = -100;
		}

		heatSetting.duty_cycle = duty;
	}

	public void setTemp(double temp) {
		if (temp < 0) {
			temp = 0;
		}

		heatSetting.set_point = temp;
	}

	public void setP(double p) {
		heatSetting.proportional = p;
	}

	public void setI(double i) {
		heatSetting.integral = i;
	}

	public void setD(double d) {
		heatSetting.derivative = d;
	}
	
	public String getMode() {
		return mode;
	}

	public double getTemp() {
		return fTemp_C;
	}

	public String getGPIO() {
		return fGPIO;
	}

	public double getDuty() {
		return heatSetting.duty_cycle;
	}

	public double getCycle() {
		return heatSetting.cycle_time;
	}

	public double getSetPoint() {
		return heatSetting.set_point;
	}

	public double getP() {
		return heatSetting.proportional;
	}

	public double getI() {
		return heatSetting.integral;
	}

	public double getD() {
		return heatSetting.derivative;
	}

	public Temp getTempProbe() {
		return fTemp;
	}

	public String getName() {
		return fTemp.getName();
	}


  //PRIVATE ///
  	private long previousTime = 0;

	private double calc_average() {
		int size = temp_F_list.size();
		
		double total =0.0;
		for (double t : temp_F_list) {
			total += t;
		}
		return total/size;
	}

	private double fToC(double tempF) {
		// routine to convert basic F temps to C
		return (5 * (tempF - 32)) / 9;
	}

	private double cToF(double tempC) {
		// routine to convert Celcius to Farenheit
		return ((tempC * 9) / 5) + 32;
	}
	private boolean fStatus = false;
	private Temp fTemp;
	private double fTemp_F, fTemp_C;
	private String fGPIO;
	private List<Double> temp_F_list = new ArrayList<Double>();
	
	private String mode;
	private String fName;
	private long currentTime = System.currentTimeMillis();
	public Settings heatSetting;
	public Settings coldSetting;

	// Temp values for PID calculation
	double errorFactor = 0.0;
	double previous_error = 0.0;
	double integralFactor = 00.0;
	double derivativeFactor = 0.0;
	double output = 0.0;

	double GMA_HLIM = 100.0,
	GMA_LLIM = -100.0;

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

	public void shutdown() {
		if(OC != null) {
			OC.shutdown();
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
}
