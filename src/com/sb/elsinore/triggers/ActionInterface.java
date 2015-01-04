package com.sb.elsinore.triggers;

import org.json.simple.JSONObject;
import org.rendersnake.HtmlCanvas;

/**
 * Base Interface for actions.
 * @author doug
 *
 */
public interface ActionInterface {

    /**
     * Get the current status as a JSON Representation.
     * @return The JSONObject representing the current status
     */
    JSONObject getJsonStatus();

    /**
     * Perform the action in the object.
     */
    void performAction();

    /**
     * Return the Form HTML Canvas representing this interface for input.
     * @param stepCount The position to add to any details.
     * @return The HTML Canvas representing the input form for this Trigger.
     */
    HtmlCanvas getForm(int stepCount);
}
