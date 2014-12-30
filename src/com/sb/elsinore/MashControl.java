package com.sb.elsinore;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.reflections.Reflections;

import com.sb.elsinore.triggers.TriggerInterface;

import sun.reflect.Reflection;


/********************
 * This class is for storing the mash steps.
 * It automatically updating the process as it goes.
 *
 * @author Doug Edey
 *
 */

public class MashControl implements Runnable {

    /**
     * The output PID to be controlled & read from.
     */
    private String outputControl = "";

    /**
     * The Pump to be controlled (not in use yet).
     */
    private String pumpControl = "";

    /**
     * A flag to tell the thread to shutdown.
     */
    private boolean shutdownFlag = false;

    /**
     * The default variance.
     */
    private double varianceF = 2.0;

    /**
     * The list of mash steps, position -> Step.
     */
    private ArrayList<TriggerInterface> triggerList =
            new ArrayList<TriggerInterface>();

    /**
     * Add a mashstep at a position, overriding the old one.
     * @param position The position to add the mashstep at
     * @return The new Mash Step
     */
    public final TriggerInterface addMashStep(final int position, String type, JSONObject parameters) {
        Set<Class<? extends TriggerInterface>> triggerSet = new Reflections("com.sb.elsinore.triggers")
            .getSubTypesOf(TriggerInterface.class);
        String seekName = type + "Trigger";
        TriggerInterface triggerStep = null;
        for (Class<? extends TriggerInterface> triggerClass: triggerSet) {
            if (triggerClass.getName().equals(seekName)) {
                try {
                    Constructor<? extends TriggerInterface> triggerConstructor
                        = triggerClass.getConstructor(int.class, JSONObject.class);
                    triggerStep = triggerConstructor.newInstance(position, parameters);
                    triggerList.add(triggerStep);
                } catch (InstantiationException | IllegalAccessException
                        | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
        
        return triggerStep;
    }

    /**
     * Get the current size of the mash step list.
     * @return The size of the mash step list
     */
    public final int getMashStepSize() {
        return triggerList.size();
    }

    /**
     * Get the mash step at the specified position.
     * @param position The position to get the mash step at
     * @return The mash step at the specified position.
     */
    public final TriggerInterface getTrigger(final Integer position) {
        this.sortMashSteps();
        return this.triggerList.get(position);
    }

    /**
     * Get the current mash step that's activated.
     * @return And entry with the position and the mash step
     */
    public final TriggerInterface getCurrentTrigger() {
        for (TriggerInterface m: triggerList) {
            if (m.isActive()) {
                return m;
            }
        }
        return null;
    }

    /**
     * A loop to run for the thread that checks the states.
     */
    @Override
    public final void run() {
        // Run through and update the times based on the currently active step
        TriggerInterface triggerEntry = getCurrentTrigger();

        TriggerInterface currentTrigger = null;
        Integer currentTriggerPosition = -1;

        // active step
        if (triggerEntry != null) {
            currentTrigger = triggerEntry;
            currentTriggerPosition = triggerEntry.getPosition();
        }

        PID currentPID = LaunchControl.findPID(getOutputControl());

        while (true) {
            // Is there a step and an output control?
            if (currentTrigger == null && currentPID != null) {
                triggerEntry = getCurrentTrigger();

                // active step
                if (triggerEntry != null) {
                    currentTrigger = triggerEntry;
                    currentTriggerPosition = triggerEntry.getPosition();
                    BrewServer.LOG.warning("Found an active mash step: "
                        + currentTriggerPosition);
                }
            }

            if (currentTrigger != null && currentPID != null) {
                // Do stuff with the active step
                currentTrigger.waitForTrigger();
                currentTriggerPosition += 1;
                currentTrigger = getTrigger(currentTriggerPosition);
                currentTrigger.setActive();
            }
            try {
                // Sleep for 10 seconds
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // We got woken up.
            }
            if (isShutdownFlag()) {
                return;
            }
        }
    }

    /**
     * Activate the step at the selected position.
     * Deactivates the other mash steps.
     * @param position The position to activate the step at
     * @return True if activated OK, false if there's an error
     */
    public final boolean activateTrigger(final Integer position) {
        // deactivate all the steps first
        TriggerInterface triggerEntry = getTrigger(position);

        // Do we have a value
        if (triggerEntry == null) {
            BrewServer.LOG.warning("Index out of bounds");
            return false;
        }

        // Now we can reset the others
        if (!deactivateTrigger(-1)) {
            BrewServer.LOG.warning("Couldn't disable all the mash steps");
            return false;
        }

        triggerEntry.setActive();
        return true;
    }

    /**
     * Deactivate the step at the position specified.
     * @param position The step to deactivate. If < 0 deactivate all.
     * @return True if deactivated OK, false if not.
     */
    public final boolean deactivateTrigger(final Integer position) {
        // deactivate all the steps first

        if (position >= 0) {
            TriggerInterface triggerEntry = getTrigger(position);

            // Do we have a value
            if (triggerEntry == null) {
                BrewServer.LOG.warning("Index out of bounds");
                return false;
            }
            triggerEntry.deactivate();
        } else {
            // Otherwise deactivate all the steps
            for (TriggerInterface mEntry : triggerList) {
                mEntry.deactivate();
            }
        }

        return true;
    }

    /**
     * Return the current state of this MashControl as a JSON string.
     * @return The String representing the current state.
     */
    public final String getJSONDataString() {
        StringWriter out = new StringWriter();
        try {
            getJSONData().writeJSONString(out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toString();
    }

    /**
     * Get the current status as a JSONObject.
     * @return The JSONObject representing the current state.
     */
    @SuppressWarnings("unchecked")
    public final JSONArray getJSONData() {
        JSONArray masterArray = new JSONArray();
        DateFormat lFormat = new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ssZ");
        //masterArray.put("pid", this.getOutputControl());
        synchronized (triggerList) {
            for (TriggerInterface e : triggerList) {
                masterArray.add(e.getJsonStatus());
            }
        }
        return masterArray;
    }

    /**
     * @return Is the shutdown flag set?
     */
    public final boolean isShutdownFlag() {
        return shutdownFlag;
    }

    /**
     * @param newFlag Value to set the shutdown flag to
     */
    public final void setShutdownFlag(final boolean newFlag) {
        this.shutdownFlag = newFlag;
    }

    /**
     * @return the outputControl
     */
    public final String getOutputControl() {
        return outputControl;
    }

    /**
     * @param newControl the outputControl to set
     */
    public final void setOutputControl(final String newControl) {
        this.outputControl = newControl;
    }

    public void sortMashSteps() {
       // Collections.sort(this.triggerList);
    }

    /**
     * Delete the specified mash step.
     * @param position The step position to delete
     */
    public final void delMashStep(final int position) {
        sortMashSteps();
        for (int i = 0; i < triggerList.size(); i++) {
            if (triggerList.get(i).getPosition() == position) {
                this.triggerList.remove(i);
            }
        }

        // No more steps, turn off the MashControl
        if (triggerList.size() == 0) {
            setShutdownFlag(true);
            Thread.currentThread().interrupt();
        }
    }
}
