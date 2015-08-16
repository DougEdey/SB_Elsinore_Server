package com.sb.elsinore.triggers;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.id;
import static org.rendersnake.HtmlAttributesFactory.name;
import static org.rendersnake.HtmlAttributesFactory.value;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.sb.elsinore.*;
import org.json.simple.JSONObject;
import org.rendersnake.HtmlCanvas;

import org.w3c.dom.Element;

import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public class SwitchTrigger implements TriggerInterface {

    private static final String SWITCHNAME = "switchname";
    private static final String ACTIVATE = "activate";
    private int position = -1;
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
     * @param inPosition The position to create.
     */
    public SwitchTrigger(final int inPosition) {
        this.position = inPosition;
    }

    /**
     * Create a trigger with parameters.
     * @param inPos The position to create the trigger at.
     * @param parameters The parameters.
     */
    public SwitchTrigger(final int inPos, final JSONObject parameters) {
        this.position = inPos;
        this.switchName = (String) parameters.get("switchname");
        this.activate = (String) parameters.get("activate");
    }

    /**
     * Compare by position.
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
        Switch aSwitch = LaunchControl.findSwitch(this.switchName);
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
     * @param fromUI
     */
    @Override
    public final void deactivate(boolean fromUI) {
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
        html.div(id("NewSwitchTrigger").class_(""));
        html.form(id("newTriggersForm"));
            html.input(id("type").name("type")
                        .hidden("true").value("Switch"));
            // Add the Switches as a drop down list.
            html.select(class_("holo-spinner").name("switchname")
                    .id("switchName"));
                html.option(value(""))
                        .write(Messages.SWITCHES)
                ._option();
                for (Switch tSwitch : LaunchControl.switchList) {
                    String tName = tSwitch.getName();
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
                html.option(value("on"))
                    .write(Messages.SWITCH_ON)
                ._option();
                html.option(value("off"))
                    .write(Messages.SWITCH_OFF)
                ._option();
            html._select();

            html.button(name("submitSwitchTrigger")
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
        html.div(id("EditSwitchTrigger").class_(""));
        html.form(id("editTriggersForm"));
            html.input(id("type").name("type")
                        .hidden("true").value("switch"));
            html.input(id("type").name("position")
                    .hidden("position").value("" + this.position));
            // Add the Switches as a drop down list.
            html.select(class_("holo-spinner").name(SWITCHNAME)
                    .id("switchName"));
                html.option(value(""))
                        .write(Messages.SWITCHES)
                ._option();
                for (Switch tSwitch : LaunchControl.switchList) {
                    String tName = tSwitch.getName();
                    html.option(value(tName)
                            .selected_if(this.switchName.equals(tName)))
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
                html.option(value(Messages.SWITCH_ON)
                        .selected_if(this.activate.equals("on")))
                    .write("On")
                ._option();
                html.option(value(Messages.SWITCH_OFF)
                        .selected_if(this.activate.equals("off")))
                    .write("Off")
                ._option();
            html._select();

            html.button(name("submitSwitchTrigger")
                    .class_("btn col-md-12")
                    .add("data-toggle", "clickover")
                    .onClick("updateTriggerStep(this);"))
                .write(Messages.EDIT)
            ._button();
        html._form();
        html._div();
        return html;
    }

    /**
     * Update this trigger.
     * @param params The parameters to update with.
     */
    @Override
    public final void updateTrigger(final JSONObject params) {
        String tName = (String) params.get(SWITCHNAME);
        String tActivate = (String) params.get(ACTIVATE);

        // Update the variables.
        if (tActivate != null) {
            this.activate = tActivate;
        }
        if (tName != null && LaunchControl.findSwitch(tName) != null) {
            this.switchName = tName;
            if (this.active) {
                triggerSwitch();
            }
        }
    }

    @Override
    public boolean readTrigger(Element rootElement) {
        if (getName().equals(rootElement.getAttribute(TYPE)))
        {
            return false;
        }
        this.position = Integer.parseInt(rootElement.getAttribute(POSITION));
        if (LaunchControl.shouldRestore()) {
            this.activate = LaunchControl.getTextForElement(rootElement, ACTIVATE, ACTIVATE);
        }
        this.switchName = LaunchControl.getTextForElement(rootElement, SWITCHNAME, "");
        return true;
    }

    @Override
    public void updateElement(Element rootElement) {
        Element triggerElement;
        if (rootElement.getNodeName().equals(TriggerControl.NAME))
        {
            triggerElement = LaunchControl.addNewElement(rootElement, TriggerInterface.NAME);
        }
        else if (rootElement.getNodeName().equals(TriggerInterface.NAME))
        {
            triggerElement = rootElement;
        }
        else
        {
            return;
        }

        triggerElement.setAttribute(TriggerInterface.POSITION, Integer.toString(this.position));
        triggerElement.setAttribute(TriggerInterface.TYPE, getName());

        if (triggerElement.hasChildNodes()) {
            Set<Element> delElements = new HashSet<>();
            // Can't delete directly from the nodelist, concurrency issues.
            for (int i = 0; i < triggerElement.getChildNodes().getLength(); i++) {
                delElements.add((Element) triggerElement.getChildNodes().item(i));
            }
            // now we can delete them.
            for (Element e : delElements) {
                e.getParentNode().removeChild(e);
            }
        }

        LaunchControl.addNewElement(rootElement, SWITCHNAME).setTextContent(this.switchName);
        LaunchControl.addNewElement(rootElement, ACTIVATE).setTextContent(this.activate);
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
        String description = this.switchName + ": " + this.activate;
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
     * @return true if at least one switch is setup.
     * @param inType The type of the device to check against.
     */
    @Override
    public final boolean getTriggerType(final String inType) {
        return (LaunchControl.switchList.size() > 0);
    }

}
