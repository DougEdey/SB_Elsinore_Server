package com.sb.elsinore.triggers;

import org.json.simple.JSONObject;

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
}
