package com.sb.elsinore;
import jGPIO.*;

public final class OutputControl implements Runnable {

   public OutputControl ( String aName, String fGPIO, double heat_time ){
	   	// just for heating
		fName = aName;
		fGPIOh = fGPIO.toLowerCase();
		setHTime(heat_time);
   }


   public OutputControl ( String aName, String GPIO_heat, double heat_time, String GPIO_cool, double cool_time, double cool_delay ){
	   	// just for heating and cooling
		fName = aName;
		fGPIOc = GPIO_cool.toLowerCase();
		fGPIOh = GPIO_heat.toLowerCase();
		setHTime(heat_time);
		setCTime(cool_time);
		setCDelay(cool_delay);
   }

   public void setCool(String GPIO, double time, double delay) { 	
		fGPIOc = GPIO;
		setCTime(time);
		setCDelay(delay);
   }


   public void run() {
		double duty;
		double on_time, off_time;
		
		try {
			BrewServer.log.info("Starting the ("+fGPIOh+") heating output: " + GPIO.getPinNumber(fGPIOh));	
			try {
				
				if(fGPIOc != null && !fGPIOh.equals("")) {
					Heat_SSR = new OutPin(fGPIOh);
					BrewServer.log.info("Started the heating output: " + GPIO.getPinNumber(fGPIOh));
				}
		
				if(fGPIOc != null && !fGPIOc.equals("")) {
					Cool_SSR = new OutPin(fGPIOc);
				}
			} catch (RuntimeException e) {
				
				BrewServer.log.warning("Could not control the GPIO Pin. Did you start as root?");
				e.printStackTrace();
				return;
			}

			try {
				while(true) {
				
					if(fGPIOc != null && !fGPIOc.equals("") && Cool_SSR == null) {
						Cool_SSR = new OutPin(fGPIOc);
					}
					if(fGPIOh != null && !fGPIOh.equals("") && Heat_SSR == null) {
						Heat_SSR = new OutPin(fGPIOh);
					}
					BrewServer.log.info("Fduty: "+fDuty);
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
						BrewServer.log.info("On: " + on_time + " Off; " + off_time);
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
				} // end the while loop
			
			} catch (RuntimeException e) {
				BrewServer.log.warning("Could not control the GPIO Pin during loop. Did you start as root?");
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
			BrewServer.log.warning(fName + " turning off outputs" );
			allOff();
		}
   }

	public void shutdown() {
		BrewServer.log.info("Shutting down OC");
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
	
	public synchronized void setDuty(double duty) {
		fDuty = duty;
		BrewServer.log.info("IN: " + duty + " OUT: " + fDuty);
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
		
		if(Cool_SSR == null && Heat_SSR == null) {
			BrewServer.log.info("No SSRs to turn on");
		}
		
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
			synchronized(Heat_SSR) {
				Heat_SSR.setValue(false);
			}
		}
		if(Cool_SSR != null) {
			synchronized(Cool_SSR) {
				Cool_SSR.setValue(false);
			}
		}
	}

	private void allDisable() {
		if(Heat_SSR != null) {
			synchronized(Heat_SSR) {
				Heat_SSR.close();
				Heat_SSR = null;
			}
		}
		if(Cool_SSR != null) {
			synchronized(Cool_SSR) {
				Cool_SSR.close();
				Cool_SSR = null;
			}
		}
	}
	
	// PRIVATE ////
	private OutPin Heat_SSR = null;
	private OutPin Cool_SSR = null;
	private String fName;
	private String fGPIOh = "";
	private String fGPIOc = "";
	private int fTimeh;
	private int fTimec;
	private int cool_delay;
	private double fDuty;
	private boolean cool_status = false;
	private boolean heat_status = false;
	private long coolStopTime = -1;
	
	public synchronized double getDuty() {
		// TODO Auto-generated method stub
		return fDuty;
	}
}

