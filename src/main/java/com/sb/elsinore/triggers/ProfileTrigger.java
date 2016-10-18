package com.sb.elsinore.triggers;

import com.sb.elsinore.*;
import org.json.simple.JSONObject;
import org.rendersnake.HtmlCanvas;

import java.io.IOException;
import java.util.Date;

import static org.rendersnake.HtmlAttributesFactory.*;

public class ProfileTrigger implements TriggerInterface {

    private static final String TARGET_NAME = "targetname";
    private static final String ACTIVATE = "activate";
    private static final String DISABLE = "disable";

    private Integer position = -1;
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
        this.startDate = new Date();
        if (this.targetName == null) {
            return;
        }

        TriggerControl triggerControl = LaunchControl.getInstance().findTriggerControl(
                this.targetName);
        TriggerInterface triggerEntry = triggerControl != null ? triggerControl.getCurrentTrigger() : null;
        if (triggerControl == null) {
            return;
        }

        int stepToUse = -1;
        if (!this.activate) {
            // We're de-activating everything
            stepToUse = -1;
        } else {
            if (triggerEntry == null) {
                if (triggerControl.triggerCount() > 0) {
                    stepToUse = 0;
                }
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

        LaunchControl.getInstance().startMashControl(this.targetName);
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
    public void deactivate(boolean fromUI) {
        this.active = true;
    }

    @Override
    public Integer getPosition() {
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
        html.input(id(TYPE).name(TYPE).class_("form-control m-t")
                .hidden("true").value("Profile"));
        // Add the probe as a drop down list.
        html.select(class_("form-control m-t").name(TARGET_NAME)
                .id(TARGET_NAME));
        html.option(value(""))
                .write("Target Probe")
                ._option();
        for (Temp tTemp : LaunchControl.getInstance().tempList) {
            String tName = tTemp.getName();
            html.option(value(tName))
                    .write(tName)
                    ._option();
        }
        html._select();

        // Add the on/off values
        html.select(class_("form-control m-t").name(ACTIVATE)
                .id(ACTIVATE));
        html.option(value(""))
                .write("")
                ._option();
        html.option(value(ACTIVATE))
                .write(Messages.ACTIVATE)
                ._option();
        html.option(value(DISABLE))
                .write(Messages.DISABLE)
                ._option();
        html._select();

        html.button(name("submitTriggerTrigger")
                .class_("btn btn-primary col-md-12")
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
        html.input(id(TYPE).name(TYPE)
                .hidden("true").value("Profile"));
        html.input(id("type").name(POSITION)
                .hidden(POSITION).value(this.position));
        // Add the triggers as a drop down list.
        html.select(class_("holo-spinner").name(TARGET_NAME)
                .id(TARGET_NAME));
        html.option(value(""))
                .write("Target Probe")
                ._option();
        for (Temp tTemp : LaunchControl.getInstance().tempList) {
            String tName = tTemp.getName();
            html.option(value(tName))
                    .write(tName)
                    ._option();
        }
        html._select();

        // Add the on/off values
        html.select(class_("holo-spinner").name(ACTIVATE)
                .id(ACTIVATE));
        html.option(value(""))
                .write("")
                ._option();
        html.option(value(ACTIVATE)
                .selected_if(this.activate))
                .write(Messages.ACTIVATE)
                ._option();
        html.option(value(DISABLE)
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
    public void setPosition(Integer newPos) {
        this.position = newPos;
    }

    @Override
    public String getJSONStatus() {
        return LaunchControl.getInstance().toJsonString(this);
    }

    @Override
    public boolean getTriggerType(String inType) {
        return true;
    }

    @Override
    public boolean updateTrigger(JSONObject params) {
        String temp = (String) params.get(POSITION);
        if (temp != null) {
            this.position = Integer.parseInt(temp);
        }

        String target = (String) params.get(TARGET_NAME);
        String newAct = (String) params.get(ACTIVATE);

        if (target != null && LaunchControl.getInstance().findTemp(target) != null) {
            this.targetName = target;
        } else {
            return false;
        }

        this.activate = newAct != null && newAct.equals(ACTIVATE);
        return true;
    }

}
