package com.sb.elsinore;

import jGPIO.InvalidGPIOException;
import jGPIO.OutPin;

public class Pump {
	
	/*
	 * Pump class is designed to control a single GPIO pin with a straight forward on/off functionality 
	 */
	
	public String name;
	public OutPin output = null;
	
	public Pump(String name, String pinName) throws InvalidGPIOException {
		this.name = name;
		try {
			output = new OutPin(pinName);
		} catch (InvalidGPIOException e) {
			// TODO Auto-generated catch block
			throw e;
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean getStatus() {
		return output.getValue().equals("1");
	}
	
	public void turnOn() {
		output.setValue(true);
	}
	
	public void turnOff() {
		output.setValue(false);
	}

	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

	public String getGPIO() {
		// TODO Auto-generated method stub
		return output.getGPIOName();
	}
}
