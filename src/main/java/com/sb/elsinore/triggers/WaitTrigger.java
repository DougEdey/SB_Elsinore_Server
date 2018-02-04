package com.sb.elsinore.triggers;

import com.sb.common.SBStringUtils;
import com.sb.elsinore.BrewServer;
import com.sb.elsinore.Messages;
import com.sb.elsinore.notificiations.Notifications;
import com.sb.elsinore.notificiations.WebNotification;
import com.sb.util.MathUtil;
import org.json.simple.JSONObject;
import org.rendersnake.HtmlCanvas;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import static org.rendersnake.HtmlAttributesFactory.*;

/**
 * A trigger that waits for a certain period of time before continuing.
 *
 * @author Doug Edey
 */
@SuppressWarnings("unused")
public class WaitTrigger implements TriggerInterface {

    public static final String WAITTIMEMINS = "waitTimeMins";
    public static final String WAITTIMESECS = "waitTimeSecs";
    public static final String NOTES = "notes";
    final Object lck = new Object();
    volatile boolean waitStatus = true;
    private Integer position = -1;
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
     *
     * @param parameters The updated parameters.
     */
    private boolean updateParams(final JSONObject parameters) {
        String waitTimeMins = "0";
        if (parameters.get(WAITTIMEMINS) != null) {
            waitTimeMins = (String) parameters.get(WAITTIMEMINS);
            if (waitTimeMins.length() == 0) {
                waitTimeMins = "0";
            }
        }
        this.minutes = Double.parseDouble(waitTimeMins);
        String waitTimeSecs = "0";
        if (parameters.get(WAITTIMESECS) != null) {
            waitTimeSecs = (String) parameters.get(WAITTIMESECS);
            if (waitTimeSecs.length() == 0) {
                waitTimeSecs = "0";
            }
        }
        this.seconds = Double.parseDouble(waitTimeSecs);

        this.note = (String) parameters.get(NOTES);
        BigDecimal totalTime = new BigDecimal(
                this.minutes * 60);
        totalTime = totalTime.add(new BigDecimal(
                this.seconds));
        this.waitTime = totalTime;
        return true;
    }

    /**
     * Suspend the thread for a certain period of time.
     *
     * @param ms The time in milliseconds to suspend for.
     */
    private void cooldown(final long ms) {
        synchronized (this.lck) {
            long startTime = System.currentTimeMillis();
            this.startDate = new Date();
            this.endDate = new Date(startTime + ms);

            // Do thread need to wait
            if (this.waitStatus) {
                while (System.currentTimeMillis() - startTime < ms) {
                    try {
                        this.lck.wait(1000);
                    } catch (InterruptedException e) {
                        // DO NOTHING!
                    }
                }
                //  Wait over no other thread will wait
                this.waitStatus = false;
            }
        }
    }

    /**
     * Compare by position.
     *
     * @param o the TriggerInterface to compare to.
     * @return Compare.
     */
    @Override
    public final int compareTo(@Nonnull final TriggerInterface o) {
        return (this.position - o.getPosition());
    }

    /**
     * This is for all inputs.
     *
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
    public boolean isActive() {
        return this.active;
    }

    @Override
    public void setActive() {
        this.active = true;
        String message;
        if (this.endDate != null) {
            message = SBStringUtils.dateFormatShort.format(this.endDate);

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
    public Integer getPosition() {
        return this.position;
    }

    @Override
    public HtmlCanvas getForm() throws IOException {
        HtmlCanvas html = new HtmlCanvas();
        html.div(id("NewWaitTrigger").class_(""));
        html.form(id("newTriggersForm"));
        html.input(id(TYPE).name(TYPE).class_("form-control m-t")
                .hidden("true").value("Wait"));
        html.input(class_("inputBox temperature form-control m-t")
                .type("number").add("step", "any")
                .add("placeholder", Messages.MINUTES)
                .name(WAITTIMEMINS).value(""));
        html.input(class_("inputBox temperature form-control m-t")
                .type("number").add("step", "any")
                .add("placeholder", Messages.SECS)
                .name(WAITTIMESECS).value(""));
        html.input(class_("inputBox form-control m-t")
                .name(NOTES).value("")
                .add("placeholder", Messages.NOTES)
                .value(this.note));
        html.button(name("submitWait")
                .class_("btn btn-primary col-md-12")
                .add("data-toggle", "clickover")
                .onClick("submitNewTriggerStep(this);"))
                .write(Messages.ADD_TRIGGER)
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
    public HtmlCanvas getEditForm() throws IOException {
        HtmlCanvas html = new HtmlCanvas();
        html.div(id("EditWaitTrigger").class_(""));
        html.form(id("editTriggersForm"));
        html.input(id(TYPE).name(TYPE)
                .hidden("true").value("Wait"));
        html.input(id(POSITION).name(POSITION)
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
     *
     * @param params The new parameters.
     */
    @Override
    public final boolean updateTrigger(final JSONObject params) {
        return updateParams(params);
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return this.note;
    }

    private void createNotifications(String s) {
        this.webNotification = new WebNotification();
        this.webNotification.setMessage(s);
        this.webNotification.sendNotification();
        Notifications.getInstance().addNotification(this.webNotification);
    }

    public void clearNotifications() {
        if (this.webNotification == null) {
            return;
        }
        Notifications.getInstance().clearNotification(this.webNotification);
    }
}
