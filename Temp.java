import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class Temp implements Runnable {

	public Temp (String input, String aName ) {
		fProbe = "/sys/bus/w1/devices/" + aName + "/w1_slave";
		name = input;
		System.out.println(fProbe);
	}

	public void run() {
		
		
		while(true) {

			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(fProbe));
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();
				if(line.contains("NO")) {
					// bad CRC, do nothing
				} else if(line.contains("YES")) {
					// good CRC
					line = br.readLine();
					// last value should be t=
					int t = line.indexOf("t=");
					String temp = line.substring(t+2);
					double tTemp = Double.parseDouble(temp);
					currentTemp = tTemp/1000;
					if(scale.equals("F")) {
						currentTemp = (9.0/5.0)*currentTemp + 32;
					}
					currentTime = System.currentTimeMillis();
				}
			} catch (IOException ie) {
				ie.printStackTrace();
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			} finally {
				if(br != null){
					try {
						br.close();
					} catch (IOException ie) {
					}
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public String getName() {
		return name;
	}

	// PRIVATE ////
	private String fProbe;
	private String name;
	private Path probe;
	private double currentTemp = 0;
	private long currentTime = 0;
	private String scale = "C";
	private WatchService  watcher;

	public double getTemp() {
		// set up the reader
		if(scale.equals("F")) {
			return getTempF();
		}
		return getTempC();
	}

	public String getScale() {
		return scale;
	}

	public void setScale(String s) {
		if(s.equalsIgnoreCase("F") || s.equalsIgnoreCase("C")) {
			scale = s;
		}
		
	}

	public double getTempF() {
		if(scale.equals("F")) {
			return currentTemp;
		}
		return (currentTemp-32) /(9.0*5.0);
	}

	public double getTempC() {
		if(scale.equals("C")) {
			return currentTemp;
		}
		return (9.0/5.0)*currentTemp + 32;
	}

	public long getTime() {
		return currentTime;
	}
}

