package com.sb.elsinore;

import framboos.GpioPin;
import framboos.OutPin;

public class Pump {
	
	/*
	 * Pump class is designed to control a single GPIO pin with a straight forward on/off functionality 
	 */
	
	public String name;
	public OutPin output = null;
	
	public Pump(String name, String pinName) {
		this.name = name;
		output = new OutPin(pinName);
	}
	
	public boolean getStatus() {
		return output.getValue();
	}
	
	public void turnOn() {
		output.setValue(true);
	}
	
	public void turnOff() {
		output.setValue(false);
	}
}
