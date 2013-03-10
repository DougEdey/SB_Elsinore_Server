import framboos.*;

public final class OutputControl implements Runnable {

   public OutputControl ( String aName, int GPIO, double time ){
      fName = aName;
		fGPIO = GPIO;
		setTime(time);
   }

   public void run() {
		double duty;
		double on_time, off_time;
		System.out.println("Using GPIO: " + fGPIO);
		try {
		SSR = new OutPin(fGPIO);
		

      while(true) {
	
			try {

				if(fDuty == 0) {
					fStatus = false;
					SSR.setValue(false);
					Thread.sleep(fTime);
				} else if(fDuty == 100) {
					fStatus = true;
					SSR.setValue(true);
					Thread.sleep(fTime);
				} else {
					// calc the on off time
					duty = fDuty/100;
					on_time = duty * fTime;
					off_time = fTime * (1-duty);
					fStatus = true;
					SSR.setValue(true);
					Thread.sleep((int)on_time);
					fStatus = false;
					SSR.setValue(false);
					Thread.sleep((int)off_time);
				}
			} catch (InterruptedException e) {
				// Sleep interrupted
				System.out.print("Wakeup in " + fName);
			}
		}
		} finally {
			SSR.close();
		}
   }

	public void shutdown() {
			SSR.close();
	}

   public boolean getStatus() {
      return fStatus;
   }
	
	public void setDuty(double duty) {
		fDuty = duty;
	}
	
	public void setTime(double time) {
		// time is coming in in seconds
		fTime = (int)time*1000;
	}
	
   // PRIVATE ////
	private OutPin SSR;
   private String fName;
	private int fGPIO;
	private int fTime;
	private double fDuty;
	private boolean fStatus;

}

