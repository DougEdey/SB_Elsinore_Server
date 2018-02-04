package com.sb.elsinore.triggers;

import org.json.simple.JSONObject;
import org.rendersnake.HtmlCanvas;

import java.io.IOException;

/**
 * This is the base Trigger interface, a trigger should cause an action.
 *
 * @author doug
 */
public interface TriggerInterface extends Comparable<TriggerInterface> {

    String NAME = "trigger";
    String POSITION = "position";
    String TYPE = "type";
    String ACTIVE = "active";

    /**
     * Get the name of this Trigger.
     *
     * @return The name of the trigger.
     */
    String getName();

    /**
     * Causes a wait until the trigger condition is met.
     */
    void waitForTrigger();

    /**
     * Return true is this is the current trigger that is waiting.
     *
     * @return true if this is active.
     */
    boolean isActive();

    void setActive();

    void deactivate(boolean fromUI);

    /**
     * Returns the position of this trigger step.
     *
     * @return The trigger position.
     */
    Integer getPosition();

    /**
     * Get the input form for this trigger.
     *
     * @return The {@link org.rendersnake.HtmlCanvas} object that
     * represents this trigger.
     * @throws IOException If the HtmlCanvas creation causes an exception.
     */
    HtmlCanvas getForm() throws IOException;

    /**
     * Get the Edit form for this trigger.
     *
     * @return The Edit Form as a HTML Canvas
     * @throws IOException If the form could not be created.
     */
    HtmlCanvas getEditForm() throws IOException;

    /**
     * Set the position of this Trigger.
     *
     * @param newPos The new position.
     */
    void setPosition(Integer newPos);

    /**
     * Get the type of device this trigger is intended for.
     * pid, temp, any.
     *
     * @param inType The type of the trigger to check against.
     * @return True if this is a valid trigger.
     */
    boolean getTriggerType(String inType);

    /**
     * Method to update this trigger.
     *
     * @param params The Parameters to update with.
     */
    boolean updateTrigger(JSONObject params);

}
