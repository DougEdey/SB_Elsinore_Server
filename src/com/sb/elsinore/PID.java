package com.sb.elsinore;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class PID implements Runnable {
	private OutputControl OC = null;

	public PID (Temp aTemp, String aName, double aDuty, double aTime, double p, double i, double k, int GPIO) {
		mode = "off";
		set_point = 175;
		duty_cycle = aDuty;
		hard_duty_cycle = aDuty;
		cycle_time = aTime;
		p_param = p;
		i_param = i;
		d_param = k;
		fName = aName;
		fTemp = aTemp;
		fGPIO = GPIO;
		
	}
	public void updateValues(String m, double duty, double cycle, double setpoint, double p, double i, double k ) {
		mode = m;
		hard_duty_cycle = duty;
		cycle_time = cycle;
		set_point = setpoint;
		BrewServer.log.info(p_param + ": " + i_param + ": " + d_param);
		p_param = p;
		i_param = i;
		d_param = k;
		BrewServer.log.info(p_param + ": " + i_param + ": " + d_param);
		
		return;
	}

	public synchronized String getStatus() {
		if(OC != null) {
			return OC.getStatus();
		}
		return "Duty Cyle: " + duty_cycle + " - Temp: " + getTemp();
	}

	public void run() {
		BrewServer.log.info( "Running " + fName + " PID." );
		// setup the first time
		previousTime = System.currentTimeMillis();
		// create the Output if needed
		if(fGPIO != -1) {
			OC = new OutputControl(fName, fGPIO, cycle_time);	
			Thread outputThread = new Thread(OC);
			outputThread.start();
		}

		
		while (true) {
			try {
				synchronized ( fTemp ) {
					// do the bulk of the work here
					fTemp_C = fTemp.getTempC();
					currentTime = fTemp.getTime();
					fTemp_F = fTemp.getTempF();
					// if the GPIO is -1 we do not need to do any of this;
					if(fGPIO != -1) {
						if(temp_F_list.size() >= 5) {
							temp_F_list.remove(0);
						}	
	
						temp_F_list.add(fTemp_F);
	
						double temp_F_avg = calc_average();
	
	
						// we have the current temperature
						if (mode.equals("auto")) {
							duty_cycle = calcPID_reg4(temp_F_avg, true);
						}
						if (mode.equals("manual")) {
							duty_cycle = hard_duty_cycle;
						}
					
						if (mode.equals("off")) {
							duty_cycle = 0;
						}
						// determine if the heat needs to be on or off
						OC.setDuty(duty_cycle);
						OC.setHTime(cycle_time);

						//BrewServer.log.info(fName + " status: " + fTemp_F + " duty cycle: " + duty_cycle);
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

		duty_cycle = duty;
	}

	public void setTemp(double temp) {
		if (temp < 0) {
			temp = 0;
		}

		set_point = temp;
	}

	public void setK(double k) {
		d_param = k;
	}

	public void setI(double i) {
		i_param = i;
	}

	public void setP(double p) {
		p_param = p;
	}

	public String getMode() {
		return mode;
	}

	public double getTemp() {
		return fTemp_C;
	}

	public int getGPIO() {
		return fGPIO;
	}

	public double getDuty() {
		return duty_cycle;
	}

	public double getCycle() {
		return cycle_time;
	}

	public double getSetPoint() {
		return set_point;
	}

	public double getP() {
		return p_param;
	}

	public double getI() {
		return i_param;
	}

	public double getD() {
		return d_param;
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
	private int fGPIO;
	private List<Double> temp_F_list = new ArrayList<Double>();
	private double duty_cycle;
	private double hard_duty_cycle;
	private double cycle_time;
	private double set_point;
	private double p_param;
	private double i_param;
	private double d_param;
	private String mode;
	private String fName;
	private long currentTime = System.currentTimeMillis();

	// Temp values for PID calculation
	double error = 0.0;
	double previous_error = 0.0;
	double integral = 00.0;
	double derivative = 0.0;
	double output = 0.0;

	double GMA_HLIM = 100.0,
	GMA_LLIM = -100.0;

	private double calcPID_reg4(double avgTemp, boolean enable) {
		currentTime = System.currentTimeMillis();
		if(previousTime == 0.0) {
			previousTime = currentTime;
		}
		double dt = (currentTime - previousTime)/1000;

		error = set_point - avgTemp;
		integral = (integral - error) * dt;
		derivative = (error - previous_error)/dt;
		
	        output = p_param*error + i_param*integral + d_param*derivative;
		previous_error = error;

		// limit y[k] to GMA_HLIM and GMA_LLIM
		if (output > GMA_HLIM) {
			output = GMA_HLIM;
		}
		if (output < GMA_LLIM) {
			output = GMA_LLIM;
		}
		previousTime = currentTime;
		previous_error = error;
		return output;
        
	}

	public void shutdown() {
		if(OC != null) {
			OC.shutdown();
		}
	}

	// set the cool values
	public void setCool(int GPIO, int duty, double delay, double p, double i, double k) {
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
