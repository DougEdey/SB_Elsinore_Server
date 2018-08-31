package com.sb.elsinore.models;

public interface SwitchInterface {

    /**
     * @return The unique DB Id of this object
     */
    Long getId();

    /**
     * @return The name of this switch, must be unique
     */
    String getName();

    /**
     * @param name The new name for this switch, must be unique
     */
    void setName(String name);

    /**
     * @return The GPIO Name of this switch, must be unique
     */
    String getGPIOName();

    /**
     * @param gpioName The new GPIO name for this switch, must be unique
     */
    void setGPIOName(String gpioName);

    /**
     * @return True if the output is HIGH for off, LOW for on
     */
    boolean isOutputInverted();

    /**
     * @param inverted True to make the output HIGH for off, LOW for on
     */
    void setOutputInverted(boolean inverted);

    /**
     * @return The position of this Switch in the UI
     */
    int getPosition();

    /**
     * @param position The new position for this Switch in the UI
     */
    void setPosition(int position);
}
