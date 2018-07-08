package com.sb.elsinore.triggers;

import com.sb.elsinore.BrewServer;
import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.Switch;
import org.json.simple.JSONObject;
import org.rendersnake.HtmlCanvas;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Date;

import static org.rendersnake.HtmlAttributesFactory.*;

@SuppressWarnings("unused")
public class SwitchTrigger implements TriggerInterface {

    private static final String SWITCHNAME = "switchname";
    private static final String ACTIVATE = "activate";
    private Integer position = -1;
    private String activate = null;
    private String switchName = null;
    private boolean active = false;
    private Date startDate = null;

    /**
     * Create a blank switch trigger.
     */
    public SwitchTrigger() {
        BrewServer.LOG.info("Creating an empty Switch Trigger");
    }

    /**
     * Create a trigger with a specific position.
     *
     * @param inPosition The position to create.
     */
    public SwitchTrigger(final int inPosition) {
        this.position = inPosition;
    }

    /**
     * Create a trigger with parameters.
     *
     * @param inPos      The position to create the trigger at.
     * @param parameters The parameters.
     */
    public SwitchTrigger(final int inPos, final JSONObject parameters) {
        this.position = inPos;
        this.switchName = (String) parameters.get("switchname");
        this.activate = (String) parameters.get("activate");
    }

    /**
     * Compare by position.
     *
     * @param o the TriggerInterface to compare to.
     * @return Compare.
     */
    @Override
    public final int compareTo(@Nonnull TriggerInterface o) {
        return (this.position - o.getPosition());
    }

    /**
     * @return the name of this trigger.
     */
    @Override
    public final String getName() {
        return "Switch";
    }

    /**
     * Activate or deactivate the switch.
     */
    @Override
    public final void waitForTrigger() {
        this.startDate = new Date();
        if (this.switchName == null && this.activate != null) {
            return;
        }
        triggerSwitch();
    }

    /**
     * Trigger the switch.
     */
    private void triggerSwitch() {
        Switch aSwitch = LaunchControl.getInstance().findSwitch(this.switchName);
        if (aSwitch != null) {
            if (this.activate.equals("on")) {
                aSwitch.turnOn();
            } else if (this.activate.equals("off")) {
                aSwitch.turnOff();
            }
        }
    }

    /**
     * @return True if this step is active.
     */
    @Override
    public final boolean isActive() {
        return this.active;
    }

    /**
     * Set this step active.
     */
    @Override
    public final void setActive() {
        this.active = true;
    }

    /**
     * Deactivate this step.
     *
     * @param fromUI If this call came from the UI
     */
    @Override
    public final void deactivate(boolean fromUI) {
        this.active = false;
    }

    /**
     * @return The position of this step.
     */
    @Override
    public final Integer getPosition() {
        return this.position;
    }

    /**
     * Update this trigger.
     *
     * @param params The parameters to update with.
     */
    @Override
    public final boolean updateTrigger(final JSONObject params) {
        String tName = (String) params.get(SWITCHNAME);
        String tActivate = (String) params.get(ACTIVATE);

        // Update the variables.
        if (tActivate != null) {
            this.activate = tActivate;
        }
        if (tName != null && LaunchControl.getInstance().findSwitch(tName) != null) {
            this.switchName = tName;
            if (this.active) {
                triggerSwitch();
                return true;
            }
        }
        return false;
    }


    /**
     * @param newPos The position to set this step to.
     */
    @Override
    public final void setPosition(final Integer newPos) {
        this.position = newPos;
    }

    /**
     * @param inType The type of the device to check against.
     * @return true if at least one switch is setup.
     */
    @Override
    public final boolean getTriggerType(final String inType) {
        return (LaunchControl.getInstance().switchList.size() > 0);
    }

}
