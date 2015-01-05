package com.sb.elsinore.triggers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.json.simple.JSONObject;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.tools.PrettyWriter;

import static org.rendersnake.HtmlAttributesFactory.*;

import com.sb.elsinore.BrewServer;
import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.Temp;

/**
 * A TemperatureTrigger will hold until the specified probe hits the target
 * temperature when active.
 * If a PID is associated with the temperature probe, the PID set point will
 * be set to the target temperature associated with this TemperatureTrigger.
 * @author Doug Edey
 *
 */
public class TemperatureTrigger implements TriggerInterface {

    private BigDecimal targetTemp = null;
    private BigDecimal duration = null;
    private Temp temperatureProbe = null;
    private String mode = null;
    private boolean active;
    private int position = -1;
    public static String INCREASE = "INCREASE";
    public static String DECREASE = "DECREASE";
    private Date startDate = null;
    private String TRIGGER_NAME =  "Temperature";

    public TemperatureTrigger() {
        BrewServer.LOG.info("Created an empty Temperature Trigger");
    }

    public TemperatureTrigger(int newPosition) {
        BrewServer.LOG.info("Created a Temperature Trigger at" + newPosition);
        this.position = newPosition;
    }
    /**
     * Set the {@link java.util.Date} that this step is started.
     * @param inStart The Date that this step is started.
     */
    private void setStart(final Date inStart) {
        this.startDate = inStart;
    }

    /**
     * The Target temperature of this TemperatureTrigger.
     * @param inTemp The {@link java.math.BigDecimal} temperature.
     */
    public final void setTargetTemperature(final BigDecimal inTemp) {
        this.targetTemp = inTemp;
    }

    /**
     * Set the temperature probe to use for this trigger by name.
     * @param name The name of the probe to lookup.
     */
    public final void setTemperatureProbe(final String name) {
        temperatureProbe = LaunchControl.findTemp(name);
    }

    /**
     * Creates a new TemperatureTrigger at a set position with parameters.
     * @param inPosition The position this Trigger is at.
     * @param parameters The Parameters to setup this TemperatureTrigger.
     * "temp": The TargetTemperature for this step.
     * "method": A string used to represent this trigger.
     * "type": A String used to represent this trigger.
     * "tempProbe": The name of the temperature probe to use.
     */
    public TemperatureTrigger(final int inPosition,
            final JSONObject parameters) {
        this.position = inPosition;
        BigDecimal tTemp = new BigDecimal(
              parameters.get("targetTemp").toString().replace(",", "."));

        String method = parameters.get("method").toString();
        String type = parameters.get("stepType").toString();
        String tempProbe = parameters.get("tempProbe").toString();

        this.targetTemp = tTemp;
        setTemperatureProbe(tempProbe);
        this.mode = method + ": " + type;
    }

    /**
     * A blocking call that waits for a trigger to be hit.
     */
    @Override
    public final void waitForTrigger() {
        if (targetTemp == null) {
            BrewServer.LOG.warning("No Target Temperature Set");
            return;
        }
        if (temperatureProbe == null) {
            BrewServer.LOG.warning("No Temperature Probe Set");
            return;
        }
        if (mode == null) {
            BrewServer.LOG.warning("No Mode Set");
            return;
        }
        if (duration == null) {
            BrewServer.LOG.warning("No Duration Set");
            return;
        }

        setStart(new Date());
        if (this.mode.equals(TemperatureTrigger.INCREASE)) {
            while (this.temperatureProbe.getTemp().compareTo(
                    this.targetTemp) <= 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    BrewServer.LOG.warning("Temperature Trigger interrupted");
                    return;
                }
            }
        } else if (this.mode.equals(TemperatureTrigger.DECREASE)) {
            while (this.temperatureProbe.getTemp().compareTo(
                    this.targetTemp) >= 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    BrewServer.LOG.warning("Temperature Trigger interrupted");
                    return;
                }
            }
        }
    }

    /**
     * @return a {@link org.json.simple.JSONObject}
     *  representing this TemperatureTrigger.
     */
    @SuppressWarnings("unchecked")
    @Override
    public final JSONObject getJsonStatus() {
        JSONObject currentStatus = new JSONObject();
        currentStatus.put("type", "temperature");
        currentStatus.put("mode", this.mode);
        currentStatus.put("targetTemperature",
                this.targetTemp.toString());
        currentStatus.put("temperatureProbe",
                this.temperatureProbe.getName());
        if (this.startDate != null) {
            currentStatus.put("startDate", this.startDate.toString());
        }
        return currentStatus;
    }

    /**
     * Return true if this TemperatureTrigger is activated.
     * @return True if active.
     */
    @Override
    public final boolean isActive() {
        return this.active;
    }

    /**
     * Get the current position of this TemperatureTrigger in the overall list.
     * @return The position of this TemperatureTrigger
     */
    @Override
    public final int getPosition() {
        return this.position;
    }

    /**
     * Set the position of this step.
     * @param newPosition The new position.
     */
    @Override
    public final void setPosition(final int newPosition) {
        this.position = newPosition;
    }

    /**
     * Activate this TemperatureTrigger step.
     */
    @Override
    public final void setActive() {
        this.active = true;
    }

    /**
     * Deactivate this TemperatureTrigger step.
     */
    @Override
    public final void deactivate() {
        this.active = false;
    }

    /**
     * Get the Form HTML Canvas representing a temperature trigger.
     * @return {@link org.rendersnake.HtmlCanvas} representing the input form.
     * @throws IOException when the HTMLCanvas could not be created.
     */
    @Override
    public final HtmlCanvas getForm() throws IOException {
        HtmlCanvas html = new HtmlCanvas(new PrettyWriter());
        html.div(id("NewTempTrigger").class_(""));
            html.input(id("type").name("type")
                        .hidden("true").value("Temperature"));
            html.input(class_("inputBox temperature")
                    .type("number").add("step", "any")
                    .name("temperature").value(""));
            html.input(class_("inputBox").name("method").value("")
                    .add("placeholder", Messages.METHOD));
            html.input(class_("inputBox").name("stepType").value("")
                    .add("placeholder", Messages.TYPE));
            html.button(name("submitTemperature")
                    .add("data-toggle", "clickover")
                    .onClick("submitNewTriggerStep(this);"))
                .write(Messages.ADD_TRIGGER)
            ._button()
        ._div();
        return html;
    }

    @Override
    public String getName() {
        return TRIGGER_NAME;
    }
}
