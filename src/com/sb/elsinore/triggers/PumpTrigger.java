package com.sb.elsinore.triggers;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.id;
import static org.rendersnake.HtmlAttributesFactory.name;
import static org.rendersnake.HtmlAttributesFactory.value;

import java.io.IOException;
import java.util.Date;

import org.json.simple.JSONObject;
import org.rendersnake.HtmlCanvas;

import com.sb.elsinore.BrewDay;
import com.sb.elsinore.BrewServer;
import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.Pump;

public class PumpTrigger implements TriggerInterface {

    private int position = -1;
    private static String NAME = "Pump";
    private String activate = null;
    private String pumpName = null;
    private boolean active = false;
    private Date startDate = null;

    /**
     * Create a blank pump trigger.
     */
    public PumpTrigger() {
        BrewServer.LOG.info("Creating an empty Pump Trigger");
    }

    /**
     * Create a trigger with a specific position.
     * @param inPosition The position to create.
     */
    public PumpTrigger(final int inPosition) {
        this.position = inPosition;
    }

    /**
     * Create a trigger with parameters.
     * @param inPos The position to create the trigger at.
     * @param parameters The parameters.
     */
    public PumpTrigger(final int inPos, final JSONObject parameters) {
        this.position = inPos;
        this.pumpName = (String) parameters.get("name");
        this.activate = (String) parameters.get("activate");
    }

    /**
     * Compare by position.
     * @param o the TriggerInterface to compare to.
     * @return Compare.
     */
    @Override
    public final int compareTo(final TriggerInterface o) {
        return (this.position - o.getPosition());
    }

    /**
     * @return the name of this trigger.
     */
    @Override
    public final String getName() {
        return PumpTrigger.NAME;
    }

    /**
     * Activate or deactivate the pump.
     */
    @Override
    public final void waitForTrigger() {
        this.startDate = new Date();
        if (this.pumpName == null && this.activate != null) {
            return;
        }
        Pump pump = LaunchControl.findPump(this.pumpName);
        if (pump != null) {
            if (this.activate.equals("on")) {
                pump.turnOn();
            } else if (this.activate.equals("off")) {
                pump.turnOff();
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
     */
    @Override
    public final void deactivate() {
        this.active = false;
    }

    /**
     * @return The position of this step.
     */
    @Override
    public final int getPosition() {
        return this.position;
    }

    /**
     * @return The HTML elements for this form.
     * @throws IOException if the form could not be created.
     */
    @Override
    public final HtmlCanvas getForm() throws IOException {
        HtmlCanvas html = new HtmlCanvas();
        html.div(id("NewPumpTrigger").class_(""));
        html.form(id("newTriggersForm"));
            html.input(id("type").name("type")
                        .hidden("true").value("Pump"));
            // Add the Pumps as a drop down list.
            html.select(class_("holo-spinner").name("pumpname")
                    .id("pumpName"));
                html.option(value(""))
                        .write(Messages.PUMPS)
                ._option();
                for (Pump tPump: LaunchControl.pumpList) {
                    String tName = tPump.getName();
                    html.option(value(tName))
                        .write(tName)
                    ._option();
                }
            html._select();

            // Add the on/off values
            html.select(class_("holo-spinner").name("activate")
                    .id("activate"));
                html.option(value(""))
                        .write("")
                ._option();
                html.option(value(Messages.PUMP_ON))
                    .write("on")
                ._option();
                html.option(value(Messages.PUMP_OFF))
                    .write("off")
                ._option();
            html._select();

            html.button(name("submitPumpTrigger")
                    .class_("btn col-md-12")
                    .add("data-toggle", "clickover")
                    .onClick("submitNewTriggerStep(this);"))
                .write(Messages.ADD_TRIGGER)
            ._button();
        html._form();
        html._div();
        return html;
    }

    /**
     * @param newPos The position to set this step to.
     */
    @Override
    public final void setPosition(final int newPos) {
        this.position = newPos;
    }

    /**
     * @return The JSON Status.
     */
    @Override
    public final JSONObject getJSONStatus() {
        String description = this.pumpName + ": " + this.activate;
        String startDateStamp = "";
        if (this.startDate  != null) {
            startDateStamp = BrewDay.lFormat.format(this.startDate);
        }

        JSONObject currentStatus = new JSONObject();
        currentStatus.put("position", this.position);
        currentStatus.put("start", startDateStamp);
        currentStatus.put("target", "");
        currentStatus.put("description", description);
        currentStatus.put("active", Boolean.toString(this.active));

        return currentStatus;
    }

    /**
     * @return true if at least one pump is setup.
     * @param inType The type of the device to check against.
     */
    @Override
    public final boolean getTriggerType(final String inType) {
        return (LaunchControl.pumpList.size() > 0);
    }

}
