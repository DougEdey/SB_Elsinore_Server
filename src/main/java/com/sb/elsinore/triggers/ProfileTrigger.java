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
import com.sb.elsinore.Temp;
import com.sb.elsinore.TriggerControl;

public class ProfileTrigger implements TriggerInterface {

    private int position = -1;
    private boolean activate = false;
    private boolean active = false;
    private String targetName = null;
    private Date startDate = null;
    
    public ProfileTrigger() {
    }

    public ProfileTrigger(final int newPos) {
        this.position = newPos;
    }
    
    public ProfileTrigger(final int newPos, final JSONObject jsonObject) {
        this.position = newPos;
        updateTrigger(jsonObject);
    }
    
    @Override
    public int compareTo(TriggerInterface o) {
        return this.position - o.getPosition();
    }

    @Override
    public String getName() {
        return "Profile";
    }

    @Override
    public void waitForTrigger() {
        startDate = new Date();
        if (this.targetName == null) {
            return;
        }

        TriggerControl triggerControl = LaunchControl.findTriggerControl(
                this.targetName);
        TriggerInterface triggerEntry = triggerControl.getCurrentTrigger();

        int stepToUse = -1;
        // We have a mash step position
        if (position >= 0) {
            stepToUse = position;
            BrewServer.LOG.warning("Using mash step for " + this.targetName
                    + " at position " + position);
        }

        if (!this.activate) {
            // We're de-activating everything
            stepToUse = -1;
        } else {
            if (triggerEntry == null && triggerControl.triggerCount() > 0) {
                stepToUse = 0;
            } else {
                stepToUse = triggerEntry.getPosition();
            }
        }

        if (stepToUse >= 0 && this.activate) {
            triggerControl.activateTrigger(stepToUse);
            BrewServer.LOG.warning(
                    "Activated " + this.targetName + " step at " + stepToUse);
        } else {
            triggerControl.deactivateTrigger(stepToUse);
            BrewServer.LOG.warning("Deactivated " + this.targetName + " step at "
                    + stepToUse);
        }

        LaunchControl.startMashControl(this.targetName);
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public void setActive() {
        this.active = true;
    }

    @Override
    public void deactivate() {
        this.active = true;
    }

    @Override
    public int getPosition() {
        return this.position;
    }

    /**
     * @return The HTML elements for this form.
     * @throws IOException if the form could not be created.
     */
    @Override
    public final HtmlCanvas getForm() throws IOException {
        HtmlCanvas html = new HtmlCanvas();
        html.div(id("NewTriggerTrigger").class_(""));
        html.form(id("newTriggersForm"));
            html.input(id("type").name("type")
                        .hidden("true").value("Profile"));
            // Add the probe as a drop down list.
            html.select(class_("holo-spinner").name("targetname")
                    .id("targetname"));
                html.option(value(""))
                        .write("Target Probe")
                ._option();
                for (Temp tTemp: LaunchControl.tempList) {
                    String tName = tTemp.getName();
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
                html.option(value("activate"))
                    .write(Messages.ACTIVATE)
                ._option();
                html.option(value("disable"))
                    .write(Messages.DISABLE)
                ._option();
            html._select();

            html.button(name("submitTriggerTrigger")
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
     * @return The HTML elements for this form.
     * @throws IOException if the form could not be created.
     */
    @Override
    public final HtmlCanvas getEditForm() throws IOException {
        HtmlCanvas html = new HtmlCanvas();
        html.div(id("NewTriggerTrigger").class_(""));
        html.form(id("newTriggersForm"));
            html.input(id("type").name("type")
                        .hidden("true").value("Profile"));
            html.input(id("type").name("position")
                    .hidden("position").value(this.position));
            // Add the triggers as a drop down list.
            html.select(class_("holo-spinner").name("targetname")
                    .id("targetname"));
                html.option(value(""))
                        .write("Target Probe")
                ._option();
                for (Temp tTemp: LaunchControl.tempList) {
                    String tName = tTemp.getName();
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
                html.option(value("activate")
                        .selected_if(this.activate))
                    .write(Messages.ACTIVATE)
                ._option();
                html.option(value("disable")
                        .selected_if(!this.activate))
                    .write(Messages.DISABLE)
                ._option();
            html._select();

            html.button(name("submitTempTrigger")
                    .class_("btn col-md-12")
                    .add("data-toggle", "clickover")
                    .onClick("updateTriggerStep(this);"))
                .write(Messages.EDIT)
            ._button();
        html._form();
        html._div();
        return html;
    }

    @Override
    public void setPosition(int newPos) {
        this.position = newPos;
    }

    @Override
    public JSONObject getJSONStatus() {
        String description = "Deactivate ";
        if (this.activate) {
            description = "Activate ";
        }
        description = description + this.targetName;
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

    @Override
    public boolean getTriggerType(String inType) {
        return true;
    }

    @Override
    public void updateTrigger(JSONObject params) {
        String temp = (String) params.get("position");
        if (temp != null) {
            int newPos = Integer.parseInt(temp);
            this.position = newPos;
        }
        String target = (String) params.get("targetname");
        String newAct = (String)params.get("activate");
        if (target != null && LaunchControl.findTemp(target) != null) {
            this.targetName = target;
        }

        if (newAct != null && newAct.equals("activate")) {
            this.activate = true;
        } else {
            this.activate = false;
        }
    }

}
