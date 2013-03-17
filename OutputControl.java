import framboos.*;

public final class OutputControl implements Runnable {

   public OutputControl ( String aName, int GPIO_heat, double heat_time ){
   	// just for heating
	fName = aName;
	fGPIOh = GPIO_heat;
	setHTime(heat_time);
   }


   public OutputControl ( String aName, int GPIO_heat, double heat_time, int GPIO_cool, double cool_time, double cool_delay ){
   	// just for heating and cooling
	fName = aName;
	fGPIOc = GPIO_cool;
	fGPIOh = GPIO_heat;
	setHTime(heat_time);
	setCTime(cool_time);
	setCDelay(cool_delay);
   }

   public void setCool(int GPIO, double time, double delay) {
   	
	fGPIOc = GPIO;
	setCTime(time);
	setCDelay(delay);
   }


   public void run() {
		double duty;
		double on_time, off_time;
		System.out.println("Using GPIO: " + fGPIOc + " and " + fGPIOh);
		try {
			if(fGPIOh > 0) {
				Heat_SSR = new OutPin(fGPIOh);
			}

			if(fGPIOc > 0) {
				Cool_SSR = new OutPin(fGPIOc);
			}

			while(true) {
	
			try {
				if(fGPIOc <= 0 && Cool_SSR == null) {
					Cool_SSR = new OutPin(fGPIOc);
				}
				if(fGPIOh <= 0 && Heat_SSR == null) {
					Heat_SSR = new OutPin(fGPIOh);
				}

				if(fDuty == 0) {
					allOff();
					Thread.sleep(fTimeh);
				} else if(fDuty == 100) {
					heatOn();
					Thread.sleep(fTimeh);
				} else if(fDuty == -100) {
					// check to see if we've slept long enough
					Long lTime = System.currentTimeMillis() - coolStopTime;
					
					if ((lTime /1000) > cool_delay) {
						// not slept enough
						break;
					}

					coolOn();
					Thread.sleep(fTimec);
					allOff();

					coolStopTime = System.currentTimeMillis();
				} else if (fDuty > 0) {
					// calc the on off time
					duty = fDuty/100;
					on_time = duty * fTimeh;
					off_time = fTimeh * (1-duty);

					heatOn();
					Thread.sleep((int)on_time);

					allOff();
					Thread.sleep((int)off_time);
				} else if (fDuty < 0) {
					// calc the on off time
					duty = Math.abs(fDuty)/100;
					on_time = duty * fTimec;
					on_time = duty * fTimec;
					off_time = fTimec * (1-duty);

					coolOn();
					Thread.sleep((int)on_time);

					allOff();
					Thread.sleep((int)off_time);
					coolStopTime = System.currentTimeMillis();
				}
			} catch (InterruptedException e) {
				// Sleep interrupted
				// disable the outputs
				allOff();
				coolStopTime = System.currentTimeMillis();
				System.out.print("Wakeup in " + fName);
			}
		}
		} finally {
			allOff();
		}
   }

	public void shutdown() {
			allOff();
			allDisable();
	}

   public String getStatus() {
		if (cool_status) {
			return "cooling";
		}

		if (heat_status) {
			return "heating";
		}
      return "off";
   }
	
	public void setDuty(double duty) {
		fDuty = duty;
	}
	
	public void setHTime(double time) {
		// time is coming in in seconds
		fTimeh = (int)time*1000;
	}

	public void setCTime(double time) {
		// time is coming in in seconds
		fTimec = (int)time*1000;
	}

	public void setCDelay(double time) {
		// delay is coming in in minutes
		cool_delay = (int)time*1000*60;
	}

	private void heatOn() {
		cool_status = false;
		heat_status = true;
		if(Cool_SSR != null) {
			Cool_SSR.setValue(false);
		}
		if(Heat_SSR != null) {
			Heat_SSR.setValue(true);
		}
	}

	private void coolOn() {
		cool_status = true;
		heat_status = false;
		if(Heat_SSR != null) {
			Heat_SSR.setValue(false);
		}
		if(Cool_SSR != null) {
			Cool_SSR.setValue(true);
		}
	}
	
	private void allOff() {
		cool_status = false;
		heat_status = false;
		if(Heat_SSR != null) {
			Heat_SSR.setValue(false);
		}
		if(Cool_SSR != null) {
			Cool_SSR.setValue(false);
		}
	}

	private void allDisable() {
		if(Heat_SSR != null) {
			Heat_SSR.close();
		}
		if(Cool_SSR != null) {
			Cool_SSR.close();
		}
	}
	
	// PRIVATE ////
	private OutPin Heat_SSR = null;
	private OutPin Cool_SSR = null;
	private String fName;
	private int fGPIOh;
	private int fGPIOc;
	private int fTimeh;
	private int fTimec;
	private int cool_delay;
	private double fDuty;
	private boolean cool_status = false;
	private boolean heat_status = false;
	private long coolStopTime = -1;
}

