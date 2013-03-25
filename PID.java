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
		k_param = k;
		fName = aName;
		fTemp = aTemp;
		fGPIO = GPIO;
		if(i_param == 0.0){
			k0 = 0.0;
		} else {
			k0 = k_param * cycle_time / i_param;
		}
		k1 = k_param * k_param / cycle_time;
		lpf1 = (2.0 * k_lpf - cycle_time) / (2.0 * k_lpf + cycle_time);
		lpf2 = cycle_time / (2.0 * k_lpf + cycle_time) ;
		
	
	}
	public void updateValues(String m, double duty, double cycle, double setpoint, double p, double i, double k ) {
		mode = m;
		hard_duty_cycle = duty;
		cycle_time = cycle;
		set_point = setpoint;
		System.out.println(p_param + ": " + i_param + ": " + k_param);
		p_param = p;
		i_param = i;
		k_param = k;
		System.out.println(p_param + ": " + i_param + ": " + k_param);
		if(i_param == 0.0){
			k0 = 0.0;
		} else {
			k0 = k_param * cycle_time / i_param;
		}
		k1 = k_param * k_param / cycle_time;
		lpf1 = (2.0 * k_lpf - cycle_time) / (2.0 * k_lpf + cycle_time);
		lpf2 = cycle_time / (2.0 * k_lpf + cycle_time) ;
		return;
	}

	public synchronized String getStatus() {
		if(OC != null) {
			return OC.getStatus();
		}
		return "Duty Cyle: " + duty_cycle + " - Temp: " + getTemp();
	}

	public void run() {
		System.out.println( "Running " + fName + " PID." );
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

						//System.out.println(fName + " status: " + fTemp_F + " duty cycle: " + duty_cycle);
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
		k_param = k;
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

	public double getK() {
		return k_param;
	}

	public Temp getTempProbe() {
		return fTemp;
	}

	public String getName() {
		return fTemp.getName();
	}


  //PRIVATE ///
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
	private double k_param;
	private String mode;
	private String fName;
	private long currentTime = System.currentTimeMillis();

	// Temp values for PID calculation
	double ek_1 = 0.0, //  e[k-1] = SP[k-1] - PV[k-1] = Tset_hlt[k-1] - Thlt[k-1]
		ek_2 = 0.0,  // e[k-2] = SP[k-2] - PV[k-2] = Tset_hlt[k-2] - Thlt[k-2]
		xk_1 = 0.0,  // PV[k-1] = Thlt[k-1]
		xk_2 = 0.0,  // PV[k-2] = Thlt[k-1]
		yk_1 = 0.0,  // y[k-1] = Gamma[k-1]
		yk_2 = 0.0,  // y[k-2] = Gamma[k-1]
		lpf_1 = 0.0, // lpf[k-1] = LPF output[k-1]
		lpf_2 = 0.0, // lpf[k-2] = LPF output[k-2]
		k_lpf = 0.0,
		k0 = 0.0,
		k1 = 0.0,
		lpf1 = 0.0,
		lpf2 = 0.0,
		pp = 0.0,
		pi = 0.0,
 		pd = 0.0;
	// output for the PID
	double yk = 0.0,

	GMA_HLIM = 100.0,
	GMA_LLIM = -100.0;

	private double calcPID_reg4(double avgTemp, boolean enable) {
		double ek = 0.0;
		ek = set_point - avgTemp; // # calculate e[k] = SP[k] - PV[k]
		if (enable) {
			//-----------------------------------------------------------
			// Calculate PID controller:	
			// y[k] = y[k-1] + kc*(PV[k-1] - PV[k] +
			// Ts*e[k]/Ti +
			// Td/Ts*(2*PV[k-1] - PV[k] - PV[k-2]))
			//-----------------------------------------------------------
			pp = k_param * (xk_1 - avgTemp); // # y[k] = y[k-1] + Kc*(PV[k-1] - PV[k])
			//System.out.println("pp: " + pp);
			pi = k0 * ek; // # + Kc*Ts/Ti * e[k]
			//System.out.println("pi: " + pp);
			pd = k1 * (2.0 * xk_1 - avgTemp - xk_2);
			//System.out.println("pd: " + pp);
			yk += pp + pi + pd;
		} else {
			yk = 0.0;
			pp = 0.0;
			pi = 0.0;
			pd = 0.0;
		}

		xk_2 = xk_1;  // PV[k-2] = PV[k-1]
		xk_1 = avgTemp;    // PV[k-1] = PV[k]
		
            	//System.out.println("YK: " + yk);
		// limit y[k] to GMA_HLIM and GMA_LLIM
		if (yk >GMA_HLIM) {
			yk = GMA_HLIM;
		}
		if (yk < GMA_LLIM) {
			yk = GMA_LLIM;
		}
		return yk;
        
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
