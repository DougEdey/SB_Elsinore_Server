package com.sb.elsinore;

import java.util.Date;

public class MashStep {
    // Default ot Fahrenheit
    private String tempUnit = "F";
    private double targetTemp = 0.0;
    private double variance = 2.0;
    private int duration = 0;
    private String method = "";
    private String type = "";
    private Date startTime = null;
    private Date targetEndTime = null;
    private Date endTime = null;
    private boolean active = false;
    
    
    // GETTERS
    public boolean isActive() {
        return this.active;
    }
    
    public String getTempUnit() {
        return this.tempUnit;
    }
    
    public double getTargetTemp() {
        return getTargetTempAs(this.tempUnit);
    }
    
    public double getTargetTempAs(String unit) {
        if (this.tempUnit.equalsIgnoreCase(unit)) {
            return this.targetTemp;
        }
        
        if (this.tempUnit.equals("F")) {
            // Current Unit is F, user wants C
            return FtoC(this.targetTemp);
        } else {
            // Current unit is C, user wants F
            return CtoF(this.targetTemp);
        }
    }
    
    public double getUpperTargetTempAs(String unit) {
        double maxTemp = this.targetTemp + this.variance;
        if (this.tempUnit.equalsIgnoreCase(unit)) {
            return maxTemp;
        }
        
        if (this.tempUnit.equals("F")) {
            // Current Unit is F, user wants C
            return FtoC(maxTemp);
        } else {
            // Current unit is C, user wants F
            return CtoF(maxTemp);
        }
    }
    
    public double getLowerTargetTempAs(String unit) {
        double lowTemp = this.targetTemp - this.variance;
        if (this.tempUnit.equalsIgnoreCase(unit)) {
            return lowTemp;
        }
        
        if (this.tempUnit.equals("F")) {
            // Current Unit is F, user wants C
            return FtoC(lowTemp);
        } else {
            // Current unit is C, user wants F
            return CtoF(lowTemp);
        }
    }
    
    public int getDuration() {
        return this.duration;
    }
    
    public String getMethod() {
        return this.method;
    }
    
    public String getType() {
        return this.type;
    }
    
    public Date getStart() {
        return this.startTime;
    }
    
    public Date getTargetEnd() {
        return this.targetEndTime;
    }
    
    public Date getEnd() {
        return this.endTime;
    }
    
    
    // SETTERS
    
    public void activate() {
        this.active = true;        
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
        if (this.tempUnit.equalsIgnoreCase(newUnit)) {
            // Nothing to do here
            return;
        }
        
        if (tempUnit.equalsIgnoreCase("F")) {
            // Old if F, new must be C
            this.targetTemp = FtoC(targetTemp);
        } else {
            // Old is C, new must be F
            this.targetTemp = CtoF(targetTemp);
        }
        
        this.tempUnit = newUnit.toUpperCase();
        
        return;
    }
    
    public double FtoC(double currentTemp) {
        return (currentTemp-32) /(9.0*5.0);
    }

    public double CtoF(double currentTemp) {
        return (9.0/5.0)*currentTemp + 32;
    }
    
    public void setTemp(double newTemp) {
        this.targetTemp = newTemp;
    }
    
    public void increaseTemp(double incrValue) {
        setTemp(targetTemp + incrValue);
    }
    
    public void setDuration(int newDuration) {
        if (newDuration <= 0) {
            BrewServer.LOG.warning("Invalid duration " + newDuration + " supplied");
            return;
        }
        
        this.duration = newDuration;
    }
    
    /*********************
     * No Error checking on these,
     * Easier to allow custom mash steps
     * @param newMethod
     */
    public void setMethod(String newMethod) {
        this.method = newMethod;
    }
    
    public void setType(String newType) {
        this.type = newType;
    }
    
    public void setStart(Date newStart) {
        if (newStart != null && endTime != null 
                && newStart.compareTo(endTime) >= 0) {
            BrewServer.LOG.warning("New start date is greater than the end date");
            return;
        }
        
        // If it's null we can reset it
        this.startTime = newStart;
        
    }
    
    public void setEnd(Date newEnd) {
        if (newEnd != null && startTime != null
                && newEnd.compareTo(startTime) <= 0) {
            BrewServer.LOG.warning("New end date is less than than the start date");
            return;
        }
        
        // If it's null we can reset it
        this.endTime = newEnd;
    }
    
    public void setTargetEnd(Date newTarget) {
        if (newTarget != null && startTime != null 
                && newTarget.compareTo(startTime) <= 0) {
            System.out.println(newTarget);
            System.out.println(startTime);
            BrewServer.LOG.warning("New target date is less than than the start date");
            return;
        }
        System.out.println("Setting target time: " + newTarget);
        this.targetEndTime = newTarget;
    }
    
    @Override
    public String toString() {
        return "Step " + this.method + " " + this.type + ", target temp " + this.targetTemp + this.tempUnit + " for " + this.duration;
    }
}
