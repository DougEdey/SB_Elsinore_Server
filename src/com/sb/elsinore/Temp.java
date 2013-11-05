package com.sb.elsinore;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Temp implements Runnable {

	
	public Temp (String input, String aName ) {
		probeName = aName;
		
		if (input.equalsIgnoreCase("system")) {
			fProbe = "/sys/class/thermal/thermal_zone0/temp";
		} else {
			fProbe = "/sys/bus/w1/devices/" + aName + "/w1_slave";
		}

		name = input;
		BrewServer.log.info(fProbe);
	}

	public void run() {
		
		
		while(true) {
			if(updateTemp() == -999) {
				// Ruhoh no file found, disable output to prevent logging floods
				loggingOn = false;
			} else {
				loggingOn = true;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void setName(String n) {
		name = n;
	}

	public String getName() {
		return name;
	}

	public String getProbe() {
		return probeName;
	}

	// PRIVATE ////
	public String fProbe;
	private String name;
	private String probeName;
	private boolean loggingOn = true;

	private double currentTemp = 0;
	private long currentTime = 0;
	private String scale = "C";
	
	private boolean volumeMeasurement = false;
	private HashMap<Integer, Integer> volumeBase = null;
	private double currentVolume = 0;
	private String volumeUnit = null; 
	private double volumeConstant = 0;
	private double volumeMultiplier = 0.0;
	
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

	public double updateTemp() {
		BufferedReader br = null;
		double result = -1L;
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
				result = currentTemp;
				currentTime = System.currentTimeMillis();
			} else { // the System temp
				currentTemp = Double.parseDouble(line);
				currentTemp = currentTemp/1000;
				if(scale.equals("F")) {
					currentTemp = (9.0/5.0)*currentTemp + 32;
				}
				result = currentTemp;
				currentTime = System.currentTimeMillis();
			}
		} catch (IOException ie) {
			if(loggingOn) {
				System.out.println("Couldn't find the device under: " + fProbe);
			}
			return -999;
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
		return result;
	}

	public void addVolumes(HashMap<Integer, Integer> volumeArray, String unit) {
		// start a volume measurement at the same time
		volumeMeasurement = true;
		volumeBase = volumeArray;
		volumeUnit = unit;
		
		
	}
	
	public void setupVolume() {
		if (volumeBase == null) {
			return;
		}
		
		// Calculate the values of b*value + c = volume
		// get the value of c
		volumeConstant = volumeBase.get(0);
		volumeMultiplier = 0.0;
		
		// for the rest of the values
		Iterator it = volumeBase.entrySet().iterator();
		Map.Entry prevPair = null;
		
		if (volumeConstant != 0.0) {
			volumeConstant = 0.0;
		}
		
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			if (prevPair != null) {
				// diff the pair value and dive by the diff of the key
				Integer keyDiff = (Integer) pairs.getKey() - (Integer) prevPair.getKey();
				Integer valueDiff = ((Integer) pairs.getValue() - (Integer) prevPair.getValue());
				double newMultiplier = ((double)valueDiff)/keyDiff;
				double newConstant = (Integer)pairs.getValue() - ((double)valueDiff*keyDiff);
				
				if (volumeMultiplier != 0.0) {
					if (newMultiplier != volumeMultiplier) {
						System.out.println("The newMultiplier isn't the same as the old one, if this is a big difference, be careful! You may need a quadratic!");
						System.out.println("New: " + newMultiplier + ". Old: " + volumeMultiplier);
					}			
				} else {
					volumeMultiplier = newMultiplier;
				}
				
				if (volumeConstant != 0.0) {
					if (newConstant != volumeConstant) {
						System.out.println("The new constant isn't the same as the old one, if this is a big difference, be careful! You may need a quadratic!");
						System.out.println("New: " + newConstant + ". Old: " + volumeConstant);
					}
				} else {
					volumeConstant = newConstant;
				}
			}
		}
		
		// we should be done now
		
	}
}

