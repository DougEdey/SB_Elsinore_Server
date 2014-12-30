package com.sb.elsinore.triggers;

import org.json.simple.JSONObject;

/**
 * This is the base Trigger interface, a trigger should cause an action.
 * @author doug
 *
 */
public interface TriggerInterface {

    /**
     * Causes a wait until the trigger condition is met.
     */
    void waitForTrigger();

    /**
     * Set the action to be performed when this trigger is met.
     * @param action The Action object to associate with this Trigger.
     */
    void setAction(ActionInterface action);
    /**
     * Get the action to perform next.
     * @return The Action Object that's being returned
     */
    ActionInterface getAction();

    /**
     * Get a JSON Object representing the current status of the object.
     * @return The current status as a JSONObject
     */
    JSONObject getJsonStatus();

    /**
     * Return true is this is the current trigger that is waiting.
     * @return true if this is active.
     */
    boolean isActive();
    void setActive();
    void deactivate();
    /**
     * Returns the position of this trigger step.
     * @return The trigger position.
     */
    int getPosition();
}
