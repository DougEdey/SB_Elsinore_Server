package com.sb.elsinore.triggers;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.id;
import static org.rendersnake.HtmlAttributesFactory.name;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.sb.common.SBStringUtils;
import com.sb.elsinore.*;
import com.sb.elsinore.notificiations.Notifications;
import com.sb.elsinore.notificiations.WebNotification;
import org.json.simple.JSONObject;
import org.rendersnake.HtmlCanvas;

import com.sb.util.MathUtil;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;

/**
 * A trigger that waits for a certain period of time before continuing.
 * @author Doug Edey
 */
@SuppressWarnings("unused")
public class WaitTrigger implements TriggerInterface {

    private static final String WAITTIMEMINS = "waitTimeMins";
    private static final String WAITTIMESECS = "waitTimeSecs";
    private static final String NOTES = "notes";
    final Object lck = new Object();
    volatile boolean waitStatus = true;
    private int position = -1;
    private BigDecimal waitTime = BigDecimal.ZERO;
    private Date startDate, endDate;
    private boolean active = false;
    private double minutes = 0.0;
    private double seconds = 0.0;
    private String note;
    private WebNotification webNotification = null;

    public WaitTrigger() {
        BrewServer.LOG.info("Created an empty wait trigger");
    }

    public WaitTrigger(final int newPos) {
        this.position = newPos;
    }

    public WaitTrigger(final int newPos, final JSONObject parameters) {
        this.position = newPos;
        updateParams(parameters);
    }

    public WaitTrigger(final int newPos, final double waitMinutes, final double waitTimeSecs) {
        this.position = newPos;
        this.minutes = waitMinutes;
        this.seconds = waitTimeSecs;
        BigDecimal totalTime = new BigDecimal(
                this.minutes * 60);
        totalTime = totalTime.add(new BigDecimal(
                this.seconds));
        this.waitTime = totalTime;
    }

    /**
     * Set the values of this trigger.
     * @param parameters The updated parameters.
     */
    private void updateParams(final JSONObject parameters) {
        String waitTimeMins = "0";
        if (parameters.get(WAITTIMEMINS) != "") {
                waitTimeMins = (String) parameters.get(WAITTIMEMINS);
                if (waitTimeMins.length() == 0) {
                    waitTimeMins = "0";
                }
        }
        this.minutes = Double.parseDouble(waitTimeMins);
        String waitTimeSecs = "0";
        if (parameters.get(WAITTIMESECS) != "") {
                waitTimeSecs = (String) parameters.get(WAITTIMESECS);
                if (waitTimeSecs.length() == 0) {
                    waitTimeSecs = "0";
                }
        }
        this.seconds = Double.parseDouble(waitTimeSecs);
        BigDecimal totalTime = new BigDecimal(
                this.minutes * 60);
        totalTime = totalTime.add(new BigDecimal(
                this.seconds));
        this.waitTime = totalTime;
    }

    /**
     * Suspend the thread for a certain period of time.
     * @param ms The time in milliseconds to suspend for.
     */
    private void cooldown(final long ms) {
        synchronized (lck) {
            long startTime = System.currentTimeMillis();
            this.startDate = new Date();
            this.endDate = new Date(startTime + ms);

            // Do thread need to wait
            if (waitStatus) {
                while (System.currentTimeMillis() - startTime < ms) {
                    try {
                        lck.wait(1000);
                    } catch (InterruptedException e) {
                        // DO NOTHING!
                    }
                }
                //  Wait over no other thread will wait
                waitStatus = false;
            }
        }
    }

    /**
     * Compare by position.
     * @param o the TriggerInterface to compare to.
     * @return Compare.
     */
    @Override
    public final int compareTo(@Nonnull final TriggerInterface o) {
        return (this.position - o.getPosition());
    }

    /**
     * This is for all inputs.
     * @return pid
     */
    @Override
    public final boolean getTriggerType(final String inType) {
        return true;
    }

    @Override
    public String getName() {
        return "Wait";
    }

    @Override
    public void waitForTrigger() {
        // Time is in seconds, multiply by 1000 and wait.
        cooldown(MathUtil.multiply(this.waitTime, 1000).longValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject getJSONStatus() {
        String startDateStamp = "";
        if (this.startDate != null) {
            startDateStamp = BrewDay.readableFormat.format(this.startDate);
        }
        String endDateStamp;
        if (this.endDate != null) {
            endDateStamp = "End: " + BrewDay.readableFormat.format(this.endDate) + "<br />";
        } else {
            endDateStamp = "";
        }

        String targetStr = SBStringUtils.formatTime(this.minutes);
        JSONObject currentStatus = new JSONObject();
        currentStatus.put("position", this.position);
        currentStatus.put("start", startDateStamp);
        currentStatus.put("target", targetStr);
        currentStatus.put("description", endDateStamp + "<br />\n" + this.note);
        currentStatus.put("active", Boolean.toString(this.active));
        return currentStatus;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public void setActive() {
        this.active = true;
        String message = "";
        if (endDate != null) {
            message = SBStringUtils.dateFormatShort.format(endDate);

        } else {
            message = SBStringUtils.formatTime(this.minutes);
        }
        message = message + "\n" + this.note;
        createNotifications(String.format(Messages.WAIT_TRIGGER_ACTIVE, message));

    }

    @Override
    public void deactivate(boolean fromUI) {
        this.active = false;
    }

    @Override
    public int getPosition() {
        return this.position;
    }

    @Override
    public HtmlCanvas getForm() throws IOException {
        HtmlCanvas html = new HtmlCanvas();
        html.div(id("NewWaitTrigger").class_(""));
            html.form(id("newTriggersForm"));
                html.input(id("type").name("type")
                            .hidden("true").value("Wait"));
                html.input(class_("inputBox temperature form-control")
                        .type("number").add("step", "any")
                        .add("placeholder", Messages.MINUTES)
                        .name(WAITTIMEMINS).value(""));
                html.input(class_("inputBox temperature form-control")
                        .type("number").add("step", "any")
                        .add("placeholder", Messages.SECS)
                        .name(WAITTIMESECS).value(""));
                html.input(class_("inputBox form-control")
                    .name(NOTES).value("")
                    .add("placeholder", Messages.NOTES)
                    .value(this.note));
                html.button(name("submitWait")
                        .class_("btn col-md-12")
                        .add("data-toggle", "clickover")
                        .onClick("submitNewTriggerStep(this);"))
                    .write(Messages.ADD_TRIGGER)
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
    public HtmlCanvas getEditForm() throws IOException {
        HtmlCanvas html = new HtmlCanvas();
        html.div(id("EditWaitTrigger").class_(""));
            html.form(id("editTriggersForm"));
                html.input(id("type").name("type")
                        .hidden("true").value("Wait"));
                html.input(id("type").name("position")
                        .hidden("position").value("" + this.position));
                html.input(class_("inputBox temperature form-control")
                        .type("number").add("step", "any")
                        .add("placeholder", Messages.MINUTES)
                        .name(WAITTIMEMINS)
                        .value(Double.toString(this.minutes)));
                html.input(class_("inputBox temperature form-control")
                        .type("number").add("step", "any")
                        .add("placeholder", Messages.SECS)
                        .name(WAITTIMESECS)
                        .value(Double.toString(this.seconds)));
                html.input(class_("inputBox form-control")
                    .name(NOTES).value("")
                    .add("placeholder", Messages.NOTES)
                    .value(this.note));
                html.button(name("submitWait")
                        .class_("btn col-md-12")
                        .add("data-toggle", "clickover")
                        .onClick("updateTriggerStep(this);"))
                    .write(Messages.ADD_TRIGGER)
                ._button();
            html._form();
        html._div();
        return html;
    }

    /**
     * Update the wait trigger with new timings.
     * @param params The new parameters.
     */
    @Override
    public final void updateTrigger(final JSONObject params) {
        updateParams(params);
    }

    @Override
    public boolean readTrigger(Element rootElement) {
        if (!getName().equals(rootElement.getAttribute(TYPE)))
        {
            BrewServer.LOG.warning(rootElement.getAttribute(TYPE) + " is not a "  + getName());
            return false;
        }
        if (LaunchControl.shouldRestore()) {
            this.active = Boolean.parseBoolean(LaunchControl.getTextForElement(rootElement, ACTIVE, "false"));
        }
        this.position = Integer.parseInt(rootElement.getAttribute(POSITION));
        this.minutes = Double.parseDouble(LaunchControl.getTextForElement(rootElement, WAITTIMEMINS, "0"));
        this.seconds = Double.parseDouble(LaunchControl.getTextForElement(rootElement, WAITTIMESECS, "0"));
        this.note = LaunchControl.getTextForElement(rootElement, NOTES, "");
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
        LaunchControl.addNewElement(triggerElement, WAITTIMEMINS).setTextContent(Double.toString(this.minutes));
        LaunchControl.addNewElement(triggerElement, WAITTIMESECS).setTextContent(Double.toString(this.seconds));
        LaunchControl.addNewElement(triggerElement, NOTES).setTextContent(this.note);
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public void createNotifications(String s) {
        webNotification = new WebNotification();
        webNotification.setMessage(s);
        webNotification.sendNotification();
        Notifications.getInstance().addNotification(webNotification);
    }

    public void clearNotifications() {
        if (webNotification == null) {
            return;
        }
        Notifications.getInstance().clearNotification(webNotification);
    }
}
