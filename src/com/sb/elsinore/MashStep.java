package com.sb.elsinore;

import java.util.Date;

public class MashStep {
	// Default ot Fahrenheit
	private String tempUnit = "F";
	private double targetTemp = 0.0;
	private int duration = 0;
	private String method = "";
	private String type = "";
	private Date startTime = null;
	private Date targetEndTime = null;
	private Date endTime = null;
	private boolean active = false;
	
	
	// GETTERS
	public boolean isActive() {
		return active;
	}
	
	public String getTempUnit() {
		return tempUnit;
	}
	
	public double getTargetTemp() {
		return getTargetTempAs(tempUnit);
	}
	
	public double getTargetTempAs(String unit) {
		if (tempUnit.equalsIgnoreCase(unit)) {
			return targetTemp;
		}
		
		if (tempUnit.equals("F")) {
			// Current Unit is F, user wants C
			return FtoC(targetTemp);
		} else {
			// Current unit is C, user wants F
			return CtoF(targetTemp);
		}
	}
	
	public int getDuration() {
		return duration;
	}
	
	public String getMethod() {
		return method;
	}
	
	public String getType() {
		return type;
	}
	
	public Date getStart() {
		return startTime;
	}
	
	public Date getTargetEnd() {
		return targetEndTime;
	}
	
	public Date getEnd() {
		return endTime;
	}
	
	
	// SETTERS
	
	public void activate() {
		active = true;		
	}
	
	public void deactivate(boolean clear) {
		active = false;
		if (clear && endTime == null) {
			setEnd(new Date());	
		}
	}
	
	public void deactivate() {
		deactivate(false);
	}
	
	public void setTempUnit(String newUnit) {
		setTempUnit(newUnit, false);
	}
	
	// Set the new temperature unit and convert if need be
	public void setTempUnit(String newUnit, boolean convert) {
		if (tempUnit.equalsIgnoreCase(newUnit)) {
			// Nothing to do here
			return;
		}
		
		if (tempUnit.equalsIgnoreCase("F")) {
			// Old if F, new must be C
			targetTemp = FtoC(targetTemp);
		} else {
			// Old is C, new must be F
			targetTemp = CtoF(targetTemp);
		}
		
		tempUnit = newUnit.toUpperCase();
		
		return;
	}
	
	public double FtoC(double currentTemp) {
		return (currentTemp-32) /(9.0*5.0);
	}

	public double CtoF(double currentTemp) {
		return (9.0/5.0)*currentTemp + 32;
	}
	
	public void setTemp(double newTemp) {
		targetTemp = newTemp;
	}
	
	public void increaseTemp(double incrValue) {
		setTemp(targetTemp + incrValue);
	}
	
	public void setDuration(int newDuration) {
		if (newDuration <= 0) {
			BrewServer.log.warning("Invalid duration " + newDuration + " supplied");
			return;
		}
		
		duration = newDuration;
	}
	
	/*********************
	 * No Error checking on these,
	 * Easier to allow custom mash steps
	 * @param newMethod
	 */
	public void setMethod(String newMethod) {
		method = newMethod;
	}
	
	public void setType(String newType) {
		type = newType;
	}
	
	public void setStart(Date newStart) {
		if (newStart != null && endTime != null 
				&& newStart.compareTo(endTime) >= 0) {
			BrewServer.log.warning("New start date is greater than the end date");
			return;
		}
		
		// If it's null we can reset it
		startTime = newStart;
		
	}
	
	public void setEnd(Date newEnd) {
		if (newEnd != null && startTime != null
				&& newEnd.compareTo(startTime) <= 0) {
			BrewServer.log.warning("New end date is less than than the start date");
			return;
		}
		
		// If it's null we can reset it
		endTime = newEnd;
	}
	
	public void setTargetEnd(Date newTarget) {
		if (newTarget != null && startTime != null 
				&& newTarget.compareTo(startTime) <= 0) {
			BrewServer.log.warning("New target date is less than than the start date");
			return;
		}
	}
	
	@Override
	public String toString() {
		return "Step " + this.method + " " + this.type + ", target temp " + this.targetTemp + this.tempUnit + " for " + this.duration;
	}
}
