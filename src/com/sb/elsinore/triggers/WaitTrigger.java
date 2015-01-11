package com.sb.elsinore.triggers;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.id;
import static org.rendersnake.HtmlAttributesFactory.name;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.json.simple.JSONObject;
import org.rendersnake.HtmlCanvas;

import com.sb.elsinore.BrewDay;
import com.sb.elsinore.BrewServer;
import com.sb.elsinore.Messages;
import com.sb.util.MathUtil;

/**
 * A trigger that waits for a certain period of time before continuing.
 * @author Doug Edey
 */
public class WaitTrigger implements TriggerInterface {

    volatile Object lck = new Object();
    volatile boolean waitStatus = true;
    private int position = -1;
    private String TRIGGER_TYPE = "any";
    private BigDecimal waitTime = BigDecimal.ZERO;
    private Date startDate, endDate;
    private boolean active = false;

    public WaitTrigger() {
        BrewServer.LOG.info("Created an empty wait trigger");
    }

    public WaitTrigger(final int newPos) {
        this.position = newPos;
    }

    public WaitTrigger(final int newPos, final JSONObject parameters) {
        this.position = newPos;
        String waitTimeMins = "0";
        if (parameters.get("waitTimeMins") != "") {
                waitTimeMins = (String) parameters.get("waitTimeMins");
        }

        String waitTimeSecs = "0";
        if (parameters.get("waitTimeSecs") != "") {
                waitTimeSecs = (String) parameters.get("waitTimeSecs");
        }
        BigDecimal totalTime = new BigDecimal(
                Double.parseDouble(waitTimeMins) * 60);
        totalTime = totalTime.add(new BigDecimal(
                Double.parseDouble(waitTimeSecs)));
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
    public final int compareTo(final TriggerInterface o) {
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
        cooldown(MathUtil.multiply(this.waitTime , 1000).longValue());
    }

    @Override
    public JSONObject getJSONStatus() {
        String startDateStamp = "";
        if (this.startDate != null) {
            startDateStamp = BrewDay.lFormat.format(this.startDate);
        }
        String endDateStamp = "";
        if (this.endDate != null) {
            endDateStamp = BrewDay.lFormat.format(this.endDate);
        } else {
            endDateStamp = this.waitTime.toPlainString();
        }

        JSONObject currentStatus = new JSONObject();
        currentStatus.put("position", this.position);
        currentStatus.put("start", startDateStamp);
        currentStatus.put("target", this.waitTime.toPlainString());
        currentStatus.put("description", endDateStamp);
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

    }

    @Override
    public void deactivate() {
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
                        .name("waitTimeMins").value(""));
                html.input(class_("inputBox temperature form-control")
                        .type("number").add("step", "any")
                        .name("waitTimeSecs").value(""));
                html.button(name("submitWait")
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

}
