package com.sb.elsinore;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Provides a representation of a mash step.
 * @author Doug Edey
 *
 */
public class MashStep implements Comparable<MashStep> {
    // Default to Fahrenheit
    /**
     * The Temperature unit.
     */
    private String tempUnit = "F";
    /**
     * Target temperature for the mash step.
     */
    private BigDecimal targetTemp = new BigDecimal(0.0);
    /**
     * The Variance of the mashStep. Default to 2.0
     */
    private BigDecimal variance = new BigDecimal(2.0);
    /**
     * The duration of the mash step.
     */
    private BigDecimal duration = new BigDecimal(0);
    /**
     * The step method string.
     */
    private String method = "";
    /**
     * The step type string.
     */
    private String type = "";
    /**
     * The Time this mash step started.
     */
    private Date startTime = null;
    /**
     * The target time that this mash step should end.
     */
    private Date targetEndTime = null;
    /**
     * The actual end time of this mash step.
     */
    private Date endTime = null;
    /**
     * Flag to determine if this mash step is active.
     */
    private boolean active = false;
    private int position = -1;
    
    public MashStep(int position) {
        this.position = position;
    }

    // GETTERS
    /**
     * Is this step active or not?
     * @return True if active, false, if not.
     */
    public final boolean isActive() {
        return this.active;
    }

    /**
     * @return The current temp unit.
     */
    public final String getTempUnit() {
        return this.tempUnit;
    }

    /**
     * @return The current target temperature
     */
    public final BigDecimal getTargetTemp() {
        return getTargetTempAs(this.tempUnit);
    }

    /**
     * @param unit The Unit to get the current temperature in.
     * @return The current temperature in the specified unit.
     */
    public final BigDecimal getTargetTempAs(final String unit) {
        if (this.tempUnit.equalsIgnoreCase(unit)) {
            return this.targetTemp;
        }

        if (this.tempUnit.equals("F")) {
            // Current Unit is F, user wants C
            return Temp.fToC(this.targetTemp);
        } else {
            // Current unit is C, user wants F
            return Temp.cToF(this.targetTemp);
        }
    }

    /**
     * @param unit The unit to get the upper target in.
     * @return The upper target temperature in the specified unit.
     */
    public final BigDecimal getUpperTargetTempAs(final String unit) {
        BigDecimal maxTemp = this.targetTemp.add(this.variance);
        if (this.tempUnit.equalsIgnoreCase(unit)) {
            return maxTemp;
        }

        if (this.tempUnit.equals("F")) {
            // Current Unit is F, user wants C
            return Temp.fToC(maxTemp);
        } else {
            // Current unit is C, user wants F
            return Temp.cToF(maxTemp);
        }
    }

    /**
     * @param unit The unit to get the lower target in.
     * @return The lower target temperature in the specified unit.
     */
    public final BigDecimal getLowerTargetTempAs(final String unit) {
        BigDecimal lowTemp = this.targetTemp.subtract(this.variance);
        if (this.tempUnit.equalsIgnoreCase(unit)) {
            return lowTemp;
        }

        if (this.tempUnit.equals("F")) {
            // Current Unit is F, user wants C
            return Temp.fToC(lowTemp);
        } else {
            // Current unit is C, user wants F
            return Temp.cToF(lowTemp);
        }
    }

    /**
     * @return The duration of the current step
     */
    public final BigDecimal getDuration() {
        return this.duration;
    }

    /**
     * @return The method string of the current step.
     */
    public final String getMethod() {
        return this.method;
    }

    /**
     * @return The type of the current step.
     */
    public final String getType() {
        return this.type;
    }

    /**
     * @return A date object representing the current step start time.
     */
    public final Date getStart() {
        return this.startTime;
    }

    /**
     * @return A date object representing the current step target time.
     */
    public final Date getTargetEnd() {
        return this.targetEndTime;
    }

    /**
     * @return A date object representing the current step end time.
     */
    public final Date getEnd() {
        return this.endTime;
    }

    // SETTERS

    /**
     * Activate the current step.
     */
    public final void activate() {
        this.active = true;
    }

    /**
     * Deactivate the current step.
     * @param clear Clear the current step times.
     */
    public final void deactivate(final boolean clear) {
        active = false;
        if (clear && endTime == null) {
            setEnd(new Date());
        }
    }

    /**
     * Deactivate the current step without clearing the times.
     */
    public final void deactivate() {
        deactivate(false);
    }

    /**
     * Set the temp unit on the current step without converting the temps.
     * @param newUnit The unit to set the step to.
     */
    public final void setTempUnit(final String newUnit) {
        setTempUnit(newUnit, false);
    }

    /**
     * Set the new temperature unit and convert if need be.
     * @param newUnit The new unit to convert to.
     * @param convert True to convert existing temperatures to the new unit.
     */
    public final void setTempUnit(final String newUnit, final boolean convert) {
        if (this.tempUnit.equalsIgnoreCase(newUnit)) {
            // Nothing to do here
            return;
        }

        if (tempUnit.equalsIgnoreCase("F")) {
            // Old if F, new must be C
            this.targetTemp = Temp.fToC(targetTemp);
        } else {
            // Old is C, new must be F
            this.targetTemp = Temp.cToF(targetTemp);
        }

        this.tempUnit = newUnit.toUpperCase();

        return;
    }

    /**
     * Set the current target temp.
     * @param newTemp The target temp to set this step to.
     */
    public final void setTemp(final BigDecimal newTemp) {
        this.targetTemp = newTemp;
    }

    /**
     * Increase the target temperature by a value.
     * @param incrValue The value to increase the target temp by.
     */
    public final void increaseTemp(final BigDecimal incrValue) {
        setTemp(targetTemp.add(incrValue));
    }

    /**
     * Set the step duration.
     * @param duration2 The duration to set this step to in minutes.
     */
    public final void setDuration(final BigDecimal duration2) {
        if (duration2.compareTo(BigDecimal.ZERO) <= 0) {
            BrewServer.LOG.warning("Invalid duration "
                + duration2 + " supplied");
            return;
        }
        this.duration = duration2;
    }

    /*********************
     * No Error checking on these.
     * Easier to allow custom mash steps
     * @param newMethod the new method string
     */
    public final void setMethod(final String newMethod) {
        this.method = newMethod;
    }

    /**
     * Set the step type.
     * @param newType The new mash step type string.
     */
    public final void setType(final String newType) {
        this.type = newType;
    }

    /**
     * Set the start datetime of this step.
     * @param newStart The new start date.
     */
    public final void setStart(final Date newStart) {
        if (newStart != null && endTime != null
                && newStart.compareTo(endTime) >= 0) {
            BrewServer.LOG.warning(
                "New start date is greater than the end date");
            return;
        }

        // If it's null we can reset it
        this.startTime = newStart;

    }

    /**
     * Set the end datetime of this step.
     * @param newEnd The new end date.
     */
    public final void setEnd(final Date newEnd) {
        if (newEnd != null && startTime != null
                && newEnd.compareTo(startTime) <= 0) {
            BrewServer.LOG.warning(
                "New end date is less than than the start date");
            return;
        }

        // If it's null we can reset it
        this.endTime = newEnd;
    }

    /**
     * Set the target datetime of this step.
     * @param newTarget The new target time
     */
    public final void setTargetEnd(final Date newTarget) {
        if (newTarget != null && startTime != null
                && newTarget.compareTo(startTime) <= 0) {
            System.out.println(newTarget);
            System.out.println(startTime);
            BrewServer.LOG.warning(
                "New target date is less than than the start date");
            return;
        }
        System.out.println("Setting target time: " + newTarget);
        this.targetEndTime = newTarget;
    }

    /**
     * Convert this object to a string.
     * @return This object as a string.
     */
    @Override
    public final String toString() {
        return "Step " + this.method + " " + this.type + ", target temp "
            + this.targetTemp + this.tempUnit + " for " + this.duration;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int compareTo(MashStep o) {
        return Integer.compare(this.position, o.getPosition());
    }
}
