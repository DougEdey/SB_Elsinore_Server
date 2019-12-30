package com.sb.elsinore;

import com.sb.common.CollectionsUtil;
import com.sb.elsinore.triggers.TriggerInterface;
import org.json.simple.JSONObject;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

/********************
 * This class is for storing the mash steps.
 * It automatically updating the process as it goes.
 *
 * @author Doug Edey
 *
 */

public class TriggerControl implements Runnable, Serializable {

    public static final String NAME = "triggers";
    public static final String PID = "pid";
    /**
     * The list of mash steps, position -> Step.
     */
    private final List<TriggerInterface> triggerList =
            new CopyOnWriteArrayList<>();
    private Logger logger = LoggerFactory.getLogger(TriggerControl.class);

    @Autowired
    private LaunchControl launchControl;
    /**
     * The output PIDModel to be controlled & read from.
     */
    private String outputControl = "";
    /**
     * A flag to tell the thread to shutdown.
     */
    private boolean shutdownFlag = false;

    /**
     * Get the Trigger Class that matches the incoming name.
     *
     * @param name The name of the trigger to get.
     * @return The Class representing the trigger.
     */
    private static Class<? extends TriggerInterface> getTriggerOfName(
            final String name) {
        String seekName = name + "Trigger";
        // Find the trigger.
        return getTriggerList().get(seekName);
    }

    /**
     * Get a map of the triggerInterface classes.
     *
     * @return A Map of className: Class.
     */
    private static Map<String, Class<? extends TriggerInterface>>
    getTriggerList() {
        HashMap<String, Class<? extends TriggerInterface>> interfaceMap = new HashMap<>();

        Set<Class<? extends TriggerInterface>> triggerSet =
                new Reflections("com.sb.elsinore.triggers")
                        .getSubTypesOf(TriggerInterface.class);
        for (Class<? extends TriggerInterface> triggerClass : triggerSet) {
            interfaceMap.put(triggerClass.getSimpleName(), triggerClass);
        }

        return interfaceMap;
    }

    private Map<String, String> getTriggerTypes(final String inType) {
        Map<String, Class<? extends TriggerInterface>> interfaceMap =
                getTriggerList();
        Map<String, String> typeMap = new HashMap<>();

        // Get the String, String array
        for (Entry<String, Class<? extends TriggerInterface>> entry :
                interfaceMap.entrySet()) {
            Constructor<? extends TriggerInterface> tempTrigger;
            try {
                tempTrigger = entry.getValue().getConstructor();
                if (tempTrigger.newInstance()
                        .getTriggerType(inType)) {
                    typeMap.put(entry.getKey().replace("Trigger", ""),
                            tempTrigger.newInstance().getName());
                }
            } catch (NoSuchMethodException | SecurityException |
                    InstantiationException | IllegalAccessException |
                    IllegalArgumentException | InvocationTargetException e) {
                this.logger.warn("Couldn't get the default constructor for: "
                        + entry.getKey() + ". " + e.getLocalizedMessage());
            }
        }

        return typeMap;
    }

    @Autowired
    public void setLaunchControl(LaunchControl launchControl) {
        this.launchControl = launchControl;
    }

    /**
     * Add a mashstep at a position, overriding the old one.
     *
     * @param position   The position to add the mashstep at
     * @param type       The Type of the Trigger to add.
     * @param parameters The Incoming parameter JSONObject
     *                   to use to setup the trigger
     * @return The new Mash Step
     */
    public final TriggerInterface addTrigger(int position,
                                             final String type, final JSONObject parameters) {
        TriggerInterface triggerStep = null;
        Class<? extends TriggerInterface> triggerClass = getTriggerOfName(type);
        if (triggerClass == null) {
            return null;
        }

        if (position < 0) {
            position = this.triggerList.size();
        }

        // Find the constructor.
        try {
            Constructor<? extends TriggerInterface> triggerConstructor = triggerClass.getConstructor(int.class, JSONObject.class);
            triggerStep = triggerConstructor.newInstance(position, parameters);
            CollectionsUtil.addInOrder(this.triggerList, triggerStep);
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        return triggerStep;
    }

    /**
     * Update the trigger.
     *
     * @param position The position to update the new trigger at.
     * @param params   The new parameters
     */
    final boolean updateTrigger(final int position,
                                final JSONObject params) {
        TriggerInterface trigger = this.triggerList.get(position);
        return trigger.updateTrigger(params);
    }

    /**
     * Get the current size of the trigger list.
     *
     * @return The size of the trigger list
     */
    public final int getTriggersSize() {
        return this.triggerList.size();
    }

    /**
     * Get the mash step at the specified position.
     *
     * @param position The position to get the mash step at
     * @return The mash step at the specified position.
     */
    public final TriggerInterface getTrigger(final Integer position) {
        return this.triggerList.get(position);
    }

    /**
     * Get the current mash step that's activated.
     *
     * @return And entry with the position and the mash step
     */
    public final TriggerInterface getCurrentTrigger() {
        for (TriggerInterface m : this.triggerList) {
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
        if (!this.launchControl.getSystemSettings().isRestoreState()) {
            this.triggerList.forEach(trigger -> trigger.deactivate(true));
        }
        // Run through and update the times based on the currently active step
        TriggerInterface triggerEntry = getCurrentTrigger();

        TriggerInterface currentTrigger = null;
        Integer currentTriggerPosition = -1;

        // active step
        if (triggerEntry != null) {
            currentTrigger = triggerEntry;
            currentTriggerPosition = triggerEntry.getPosition();
        }

        while (true) {
            // Is there a step and an output control?
            if (currentTrigger == null) {
                triggerEntry = getCurrentTrigger();

                // active step
                if (triggerEntry != null) {
                    currentTrigger = triggerEntry;
                    currentTriggerPosition = triggerEntry.getPosition();
                    this.logger.warn("Found an active mash step: "
                            + currentTriggerPosition);
                }
            }

            if (currentTrigger != null) {
                // Do stuff with the active step
                currentTrigger.waitForTrigger();
                currentTrigger.deactivate(false);
                currentTriggerPosition += 1;
                if (currentTriggerPosition >= this.triggerCount()) {
                    return;
                }
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
     *
     * @param position The position to activate the step at
     * @return True if activated OK, false if there's an error
     */
    public final boolean activateTrigger(final Integer position) {
        // deactivate all the steps first
        TriggerInterface triggerEntry = getTrigger(position);

        // Do we have a value
        if (triggerEntry == null) {
            this.logger.warn("Index out of bounds");
            return false;
        }

        // Now we can reset the others
        if (!deactivateTrigger(-1)) {
            this.logger.warn("Couldn't disable all the mash steps");
            return false;
        }

        triggerEntry.setActive();
        return true;
    }

    /**
     * Deactivate the step at the position specified.
     *
     * @param position The step to deactivate. If < 0 deactivate all.
     * @return True if deactivated OK, false if not.
     */
    public boolean deactivateTrigger(int position) {
        return deactivateTrigger(position, false);
    }

    /**
     * Deactivate the step at the position specified.
     *
     * @param position The step to deactivate. If < 0 deactivate all.
     * @param fromUI   True if the trigger is deactivated from the UI.
     * @return True if deactivated OK, false if not.
     */
    boolean deactivateTrigger(int position, boolean fromUI) {
        // deactivate all the steps first

        if (position >= 0) {
            TriggerInterface triggerEntry = getTrigger(position);

            // Do we have a value
            if (triggerEntry == null) {
                this.logger.warn("Index out of bounds");
                return false;
            }
            triggerEntry.deactivate(fromUI);
        } else {
            // Otherwise deactivate all the steps
            for (TriggerInterface mEntry : this.triggerList) {
                mEntry.deactivate(fromUI);
            }
        }
        return true;
    }

    /**
     * @return Is the shutdown flag set?
     */
    private boolean isShutdownFlag() {
        return this.shutdownFlag;
    }

    /**
     * @param newFlag Value to set the shutdown flag to
     */
    void setShutdownFlag(final boolean newFlag) {
        this.shutdownFlag = newFlag;
    }

    /**
     * @return the outputControl
     */
    String getOutputControl() {
        return this.outputControl;
    }

    /**
     * @param newControl the outputControl to set
     */
    public void setOutputControl(final String newControl) {
        this.outputControl = newControl;
    }

    /**
     * Delete the specified trigger step.
     *
     * @param position The step position to delete
     */
    public final void delTriggerStep(final int position) {
        int i = -1;
        for (TriggerInterface ti : this.triggerList) {
            if (ti.getPosition() == position) {
                i = this.triggerList.indexOf(ti);
                break;
            }
        }
        if (i != -1) {
            this.triggerList.remove(i);
            // Drop the rest of the positions down by one.
            TriggerInterface ti;
            for (; i < this.triggerList.size(); i++) {
                ti = this.triggerList.get(i);
                if (ti != null) {
                    ti.setPosition(ti.getPosition() - 1);
                }
            }

        }

        // No more steps, turn off the MashControl
        if (this.triggerList.size() == 0) {
            setShutdownFlag(true);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Return the count of how many triggers have been added.
     *
     * @return The size of the trigger list.
     */
    public final int triggerCount() {
        return this.triggerList.size();
    }

    public void clear() {
        this.triggerList.clear();
    }

    public void addTrigger(TriggerInterface newTrigger) {
        this.triggerList.add(newTrigger);
    }

    public boolean isActive() {
        for (TriggerInterface triggerInterface : this.triggerList) {
            if (triggerInterface.isActive()) {
                return true;
            }
        }
        return false;
    }

    void deactivate() {
        for (TriggerInterface triggerInterface : this.triggerList) {
            if (triggerInterface.isActive()) {
                triggerInterface.deactivate(false);
            }
        }
    }

    public void sortTriggers() {
        this.triggerList.sort(Comparator.comparing(TriggerInterface::getPosition));
    }
}
