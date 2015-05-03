package com.sb.elsinore;

import com.sb.elsinore.triggers.TriggerInterface;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.reflections.Reflections;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.tools.PrettyWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;

import static org.rendersnake.HtmlAttributesFactory.*;

/********************
 * This class is for storing the mash steps.
 * It automatically updating the process as it goes.
 *
 * @author Doug Edey
 *
 */

public class TriggerControl implements Runnable {

    /**
     * The output PID to be controlled & read from.
     */
    private String outputControl = "";

    /**
     * A flag to tell the thread to shutdown.
     */
    private boolean shutdownFlag = false;

    /**
     * The list of mash steps, position -> Step.
     */
    private final ArrayList<TriggerInterface> triggerList =
            new ArrayList<>();

    /**
     * Add a mashstep at a position, overriding the old one.
     * @param position The position to add the mashstep at
     * @param type The Type of the Trigger to add.
     * @param parameters The Incoming parameter JSONObject
     * to use to setup the trigger
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
            position = triggerList.size();
        }

        // Find the constructor.
        try {
            Constructor<? extends TriggerInterface> triggerConstructor
                = triggerClass.getConstructor(
                        int.class, JSONObject.class);
            triggerStep = triggerConstructor.newInstance(
                    position, parameters);
            triggerList.add(triggerStep);
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }

        return triggerStep;
    }

    /**
     * Get the input trigger form for the specified type.
     * @param position The position to add the new trigger at.
     * @param type The Type of the Trigger interface to get.
     * @return The HtmlCanvas representing the form.
     */
    public static HtmlCanvas getNewTriggerForm(final int position,
            final String type) {
        TriggerInterface triggerStep;
        Class<? extends TriggerInterface> triggerClass = getTriggerOfName(type);
        if (triggerClass == null) {
            LaunchControl.setMessage(
                    "Couldn't find the Trigger Class for " + type);
            return null;
        }

        // Find the constructor.
        try {
            Constructor<? extends TriggerInterface> triggerConstructor
                = triggerClass.getConstructor(
                        int.class);
            if (triggerConstructor == null) {
                LaunchControl.setMessage(
                    "Couldn't find the basic constructor for " + type);
                return null;
            }
            triggerStep = triggerConstructor.newInstance(
                    position);
            return triggerStep.getForm();
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | IOException e) {
            LaunchControl.setMessage(e.getMessage());
            e.printStackTrace();
        }
        LaunchControl.setMessage("Failed to create trigger form for " + type);
        return null;
    }

    /**
     * Get the input trigger form for the specified type.
     * @param position The position to add the new trigger at.
     * @param params The incoming params.
     * @return The HtmlCanvas representing the form.
     */
    @SuppressWarnings("unused")
    public final HtmlCanvas getEditTriggerForm(final int position,
            final JSONObject params) {
        TriggerInterface trigger = this.triggerList.get(position);
        // Try to get the form.
        try {
            return trigger.getEditForm();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update the trigger.
     * @param position The position to update the new trigger at.
     * @param params The new parameters
     */
    public final void updateTrigger(final int position,
            final JSONObject params) {
        TriggerInterface trigger = this.triggerList.get(position);
        trigger.updateTrigger(params);
    }


    /**
     * Get the Trigger Class that matches the incoming name.
     * @param name The name of the trigger to get.
     * @return The Class representing the trigger.
     */
    public static Class<? extends TriggerInterface> getTriggerOfName(
            final String name) {
        String seekName = name + "Trigger";
        // Find the trigger.
        return getTriggerList().get(seekName);
    }

    /**
     * Get a map of the triggerInterface classes.
     * @return A Map of className: Class.
     */
    public static Map<String, Class<? extends TriggerInterface>>
        getTriggerList() {
        HashMap<String, Class<? extends TriggerInterface>> interfaceMap = new HashMap<>();

        Set<Class<? extends TriggerInterface>> triggerSet =
                new Reflections("com.sb.elsinore.triggers")
                    .getSubTypesOf(TriggerInterface.class);
        for (Class<? extends TriggerInterface> triggerClass: triggerSet) {
            interfaceMap.put(triggerClass.getSimpleName(), triggerClass);
        }

        return interfaceMap;
    }

    public static Map<String, String> getTriggerTypes(final String inType) {
        Map<String, Class<? extends TriggerInterface>> interfaceMap =
                getTriggerList();
        Map<String, String> typeMap = new HashMap<>();

        // Get the String, String array
        for (Entry<String, Class<? extends TriggerInterface>> entry:
            interfaceMap.entrySet()) {
            Constructor<? extends TriggerInterface> tempTrigger;
            try {
                tempTrigger = entry.getValue().getConstructor();
                if (tempTrigger.newInstance()
                        .getTriggerType(inType)) {
                    typeMap.put(entry.getKey().replace("Trigger",  ""),
                        tempTrigger.newInstance().getName());
                }
            } catch (NoSuchMethodException | SecurityException |
                    InstantiationException | IllegalAccessException |
                    IllegalArgumentException | InvocationTargetException e) {
                BrewServer.LOG.warning("Couldn't get the default constructor for: "
                        + entry.getKey() + ". " + e.getLocalizedMessage());
            }
        }

        return typeMap;
    }

    /**
     * Get the current size of the trigger list.
     * @return The size of the trigger list
     */
    public final int getTriggersSize() {
        return triggerList.size();
    }

    /**
     * Get the mash step at the specified position.
     * @param position The position to get the mash step at
     * @return The mash step at the specified position.
     */
    public final TriggerInterface getTrigger(final Integer position) {
        this.sortTriggerSteps();
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

        while (true) {
            // Is there a step and an output control?
            if (currentTrigger == null) {
                triggerEntry = getCurrentTrigger();

                // active step
                if (triggerEntry != null) {
                    currentTrigger = triggerEntry;
                    currentTriggerPosition = triggerEntry.getPosition();
                    BrewServer.LOG.warning("Found an active mash step: "
                        + currentTriggerPosition);
                }
            }

            if (currentTrigger != null) {
                // Do stuff with the active step
                currentTrigger.waitForTrigger();
                currentTrigger.deactivate();
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
        for (TriggerInterface e : triggerList) {
            masterArray.add(e.getJSONStatus());
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

    public void sortTriggerSteps() {
       Collections.sort(this.triggerList);
    }

    /**
     * Delete the specified trigger step.
     * @param position The step position to delete
     */
    public final void delTriggerStep(final int position) {
        sortTriggerSteps();
        for (int i = triggerList.size(); i > 0; i--) {
            if (triggerList.get(i).getPosition() == position) {
                this.triggerList.remove(i);
            }
            // Drop the rest of the positions down by one.
            if (i > position) {
                triggerList.get(i).setPosition(i - 1);
            }
        }
        sortTriggerSteps();

        // No more steps, turn off the MashControl
        if (triggerList.size() == 0) {
            setShutdownFlag(true);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get the new triggers form for display.
     * @param probe The name of the probe to attach to.
     * @return The new triggers canvas
     * @throws IOException If the form couldn't be created.
     */
    public static HtmlCanvas getNewTriggersForm(final String probe)
            throws IOException {
        String probeType;
        if (LaunchControl.findPID(probe) != null) {
            probeType = "pid";
        } else {
            probeType = "temp";
        }

        HtmlCanvas htmlCanvas = new HtmlCanvas(new PrettyWriter());
        htmlCanvas.div(id("newTriggersForm"))
            .form()
                .select(name("type").class_("holo-spinner")
                        .onClick("newTrigger(this, '" + probe + "');"));
            htmlCanvas.option(value("").selected_if(true))
                .write("Select Trigger Type")
            ._option();
            Map<String, String> triggers = getTriggerTypes(probeType);
            for (Entry<String, String> entry: triggers.entrySet()) {
                htmlCanvas.option(value(entry.getKey()))
                    .write(entry.getValue())
                ._option();
            }
                htmlCanvas._select();
                htmlCanvas.input(id("temp").name("temp")
                        .hidden("true").value(probe));
                htmlCanvas.input(id("position").name("position")
                        .hidden("true").value("-1"))
            ._form()
        ._div()
        .div(id("childInput"))._div();
        return htmlCanvas;
    }

    /**
     * Return the count of how many triggers have been added.
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
}
