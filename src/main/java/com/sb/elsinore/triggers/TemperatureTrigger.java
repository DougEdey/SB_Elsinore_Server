package com.sb.elsinore.triggers;

import com.sb.elsinore.*;
import com.sb.elsinore.devices.PID;
import com.sb.elsinore.notificiations.Notifications;
import com.sb.elsinore.notificiations.WebNotification;
import com.sb.elsinore.wrappers.TempRunner;
import com.sb.elsinore.wrappers.TemperatureValue;
import org.json.simple.JSONObject;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.tools.PrettyWriter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import static com.sb.elsinore.devices.PID.PIDMode.OFF;
import static org.rendersnake.HtmlAttributesFactory.*;

/**
 * A TemperatureTrigger will hold until the specified probe hits the target
 * temperature when active.
 * If a PID is associated with the temperature probe, the PID set point will
 * be set to the target temperature associated with this TemperatureTrigger.
 *
 * @author Doug Edey
 */
@SuppressWarnings("unused")
public class TemperatureTrigger implements TriggerInterface {

    public static final String MODE = "mode";
    public static final String METHOD = "method";
    public static final String STEPTYPE = "stepType";
    public static final String EXIT_TEMP = "exitTemperature";
    public static final String POSITION = "position";
    public static final String TARGET_TEMP = "targetTemperature";
    public static final String ACTIVE = "active";
    public static final String TEMPPROBE = "tempprobe";

    private BigDecimal targetTemp = null;
    private TempRunner temperatureProbe = null;
    private String method = null;
    private String type = null;
    private boolean active;
    private Integer position = -1;
    public static String INCREASE = "INCREASE";
    public static String DECREASE = "DECREASE";
    private String mode = null;
    private Date startDate = null;
    private BigDecimal exitTemp;
    private WebNotification webNotification = null;

    public TemperatureTrigger() {
        BrewServer.LOG.info("Created an empty Temperature Trigger");
    }

    public TemperatureTrigger(int newPosition) {
        BrewServer.LOG.info("Created a Temperature Trigger at" + newPosition);
        this.position = newPosition;
    }

    public TemperatureTrigger(int position, String tempProbe, double targetTemp, String stepType, String stepMethod) {
        this.position = position;
        this.targetTemp = new BigDecimal(targetTemp);
        this.temperatureProbe = LaunchControl.getInstance().findTemp(tempProbe);
        this.type = stepType;
        this.method = stepMethod;
    }

    /**
     * Set the {@link java.util.Date} that this step is startRunning.
     *
     * @param inStart The Date that this step is startRunning.
     */
    private void setStart(final Date inStart) {
        this.startDate = inStart;
    }

    /**
     * Set The Target temperature of the PID
     * Associated with this temperatureTrigger.
     */
    public final void setTargetTemperature() {
        PID pid = LaunchControl.getInstance().findPID(this.temperatureProbe.getName());
        if (pid == null) {
            LaunchControl.setMessage(this.temperatureProbe.getName()
                    + " is not associated with a PID. "
                    + "Trigger will wait for it to hit the target temperature.");
        } else {
            pid.setTemp(this.targetTemp);
            pid.setPidMode(PID.PIDMode.AUTO);
        }
    }

    /**
     * Set The Target temperature of the PID
     * Associated with this temperatureTrigger.
     */
    public final void setExitTemperature() {
        PID pid = LaunchControl.getInstance().findPID(this.temperatureProbe.getName());
        if (pid == null) {
            LaunchControl.setMessage(this.temperatureProbe.getName()
                    + " is not associated with a PID. "
                    + "Trigger will wait for it to hit the target temperature.");
        } else {
            pid.setTemp(this.exitTemp);
        }
    }

    /**
     * Set the temperature probe to use for this trigger by name.
     *
     * @param name The name of the probe to lookup.
     */
    public final void setTemperatureProbe(final String name) {
        this.temperatureProbe = LaunchControl.getInstance().findTemp(name);
    }

    /**
     * Creates a new TemperatureTrigger at a set position with parameters.
     *
     * @param inPosition The position this Trigger is at.
     * @param parameters The Parameters to setup this TemperatureTrigger.
     *                   "temp": The TargetTemperature for this step.
     *                   "method": A string used to represent this trigger.
     *                   "type": A String used to represent this trigger.
     *                   "tempprobe": The name of the temperature probe to use.
     */
    public TemperatureTrigger(final int inPosition,
                              final JSONObject parameters) {
        this.position = inPosition;

        String inMethod = parameters.get(METHOD).toString();
        String inType = parameters.get(STEPTYPE).toString();
        String inTempProbe = parameters.get(TEMPPROBE).toString();
        String inMode = parameters.get(MODE).toString();

        this.temperatureProbe = LaunchControl.getInstance().findTemp(inTempProbe);
        this.method = inMethod;
        this.type = inType;
        this.mode = inMode;

        this.targetTemp = new BigDecimal(
                parameters.get(TemperatureTrigger.TARGET_TEMP).toString().replace(",", "."),
                TemperatureValue.context);
        this.exitTemp = new BigDecimal(
                parameters.get(TemperatureTrigger.EXIT_TEMP).toString().replace(",", "."),
                TemperatureValue.context);
    }

    /**
     * Update the current trigger.
     */
    @Override
    public boolean updateTrigger(final JSONObject parameters) {
        BigDecimal tTemp = new BigDecimal(
                parameters.get(TemperatureTrigger.TARGET_TEMP).toString().replace(",", "."));

        String newMode = parameters.get(TemperatureTrigger.MODE).toString();
        String newMethod = parameters.get(TemperatureTrigger.METHOD).toString();
        String newType = parameters.get(TemperatureTrigger.STEPTYPE).toString();
        String newTempProbe = parameters.get(TEMPPROBE).toString();
        String exitTemp = parameters.get(TemperatureTrigger.EXIT_TEMP).toString();

        this.targetTemp = tTemp;
        if (exitTemp.equals("")) {
            this.exitTemp = this.targetTemp;
        } else {
            this.exitTemp = new BigDecimal(exitTemp.replace(",", "."));
        }
        this.temperatureProbe = LaunchControl.getInstance().findTemp(newTempProbe);
        this.method = newMethod;
        this.type = newType;
        this.mode = newMode;

        if (this.active) {
            setTargetTemperature();
            return true;
        }
        return false;
    }

    /**
     * A blocking call that waits for a trigger to be hit.
     */
    @Override
    public final void waitForTrigger() {

        if (this.targetTemp == null) {
            BrewServer.LOG.warning("No Target Temperature Set");
            return;
        }
        if (this.temperatureProbe == null) {
            BrewServer.LOG.warning("No Temperature Probe Set");
            return;
        }

        setTargetTemperature();
        setStart(new Date());
        BigDecimal probeTempF = this.temperatureProbe.getTempF();

        if (probeTempF == null) {
            return;
        }

        BigDecimal diff = this.targetTemp.subtract(probeTempF);
        if (this.mode == null) {
            // Just get to within 2F of the target TempProbe.
            BrewServer.LOG.info(String.format("Waiting to be within 2F of %.2f", this.targetTemp));

            BrewServer.LOG.info("Probe temp: " + probeTempF.toPlainString());
            while (diff.compareTo(new BigDecimal(2.0)) >= 0) {
                try {
                    BrewServer.LOG.info("" + diff);
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    BrewServer.LOG.warning("Temperature Trigger interrupted.");
                }
            }
        } else if (this.mode.equals(TemperatureTrigger.INCREASE)) {
            while (this.temperatureProbe.getTempF().compareTo(this.targetTemp) <= 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    BrewServer.LOG.warning("Temperature Trigger interrupted");
                    return;
                }
            }
        } else if (this.mode.equals(TemperatureTrigger.DECREASE)) {
            while (this.temperatureProbe.getTempF().compareTo(this.targetTemp) >= 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    BrewServer.LOG.warning("Temperature Trigger interrupted");
                    return;
                }
            }
        } else {
            // Just get to within 2F of the target TempProbe.
            BrewServer.LOG.warning("Waiting to be within 2F of " + this.targetTemp);
            while (diff.compareTo(new BigDecimal(2.0)) >= 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    BrewServer.LOG.warning("Temperature Trigger interrupted.");
                }
            }
        }

        if (this.exitTemp != null && this.exitTemp.compareTo(this.targetTemp) != 0) {
            setExitTemperature();
        }
    }

    /**
     * Return true if this TemperatureTrigger is activated.
     *
     * @return True if active.
     */
    @Override
    public final boolean isActive() {
        return this.active;
    }

    /**
     * Get the current position of this TemperatureTrigger in the overall list.
     *
     * @return The position of this TemperatureTrigger
     */
    @Override
    public final Integer getPosition() {
        return this.position;
    }

    /**
     * Set the position of this step.
     *
     * @param newPosition The new position.
     */
    @Override
    public final void setPosition(Integer newPosition) {
        this.position = newPosition;
    }

    /**
     * Activate this TemperatureTrigger step.
     */
    @Override
    public final void setActive() {
        this.active = true;
        createNotifications(String.format(Messages.TARGET_TEMP_TRIGGER, this.targetTemp,
                this.temperatureProbe.getTempProbe().getScale()));
    }

    /**
     * Deactivate this TemperatureTrigger step.
     *
     * @param fromUI If this update came from the UI
     */
    @Override
    public final void deactivate(boolean fromUI) {
        this.active = false;
        if (fromUI) {
            PID pid = LaunchControl.getInstance().findPID(this.temperatureProbe.getName());
            if (pid == null) {
                LaunchControl.setMessage(this.temperatureProbe.getName()
                        + " is not associated with a PID. "
                        + "Cannot deactivate the PID");
            } else {
                pid.setPidMode(OFF);
            }
        }
        clearNotifications();
    }

    /**
     * Get the Form HTML Canvas representing a temperature trigger.
     *
     * @return {@link org.rendersnake.HtmlCanvas} representing the input form.
     * @throws IOException when the HTMLCanvas could not be created.
     */
    @Override
    public final HtmlCanvas getForm() throws IOException {
        HtmlCanvas html = new HtmlCanvas(new PrettyWriter());
        html.div(id("NewTempTrigger").class_(""));
        html.form(id("newTriggersForm"));
        html.input(id("type").name(TYPE).class_("form-control m-t")
                .hidden("true").value("Temperature"));
        html.input(id("type").name(POSITION).class_("form-control m-t")
                .hidden("position").value("" + this.position));
        html.input(class_("inputBox temperature form-control m-t")
                .type("number").add("step", "any")
                .add("placeholder", Messages.SET_POINT)
                .name(TARGET_TEMP).value(""));
        html.input(class_("inputBox temperature form-control m-t")
                .type("number").add("step", "any")
                .add("placeholder", Messages.END_TEMP)
                .name(EXIT_TEMP).value(""));
        html.input(class_("inputBox form-control m-t")
                .name(METHOD).value("")
                .add("placeholder", Messages.METHOD));
        html.input(class_("inputBox form-control m-t")
                .name(STEPTYPE).value("")
                .add("placeholder", Messages.TYPE));
        // Add the on/off values
        html.select(class_("form-control m-t").name(MODE)
                .id(MODE));
        html.option(value(""))
                .write("")
                ._option();
        html.option(value(TemperatureTrigger.INCREASE))
                .write(TemperatureTrigger.INCREASE)
                ._option();
        html.option(value(TemperatureTrigger.DECREASE))
                .write(TemperatureTrigger.DECREASE)
                ._option();
        html._select();
        html.button(name("submitTemperature")
                .class_("btn btn-primary m-t col-md-12")
                .add("data-toggle", "clickover")
                .onClick("submitNewTriggerStep(this);"))
                .write(Messages.ADD_TRIGGER)
                ._button();
        html._form();
        html._div();
        return html;
    }

    /**
     * Get the Form HTML Canvas representing a temperature trigger.
     *
     * @return {@link org.rendersnake.HtmlCanvas} representing the input form.
     * @throws IOException when the HTMLCanvas could not be created.
     */
    @Override
    public final HtmlCanvas getEditForm() throws IOException {
        HtmlCanvas html = new HtmlCanvas(new PrettyWriter());
        html.div(id("EditTempTrigger").class_(""));
        html.form(id("editTriggersForm"));
        html.input(id(TYPE).name(TYPE)
                .hidden("true").value("Temperature"));
        html.input(id("type").name(POSITION)
                .hidden(POSITION).value("" + this.position));
        html.input(class_("inputBox temperature form-control")
                .type("number").add("step", "any")
                .add("placeholder", Messages.SET_POINT)
                .value(this.targetTemp.toPlainString())
                .name(TARGET_TEMP));
        html.input(class_("inputBox temperature form-control")
                .type("number").add("step", "any")
                .add("placeholder", Messages.END_TEMP)
                .value(this.targetTemp.toPlainString())
                .name(EXIT_TEMP));
        html.input(class_("inputBox form-control")
                .name(METHOD).value("")
                .value(this.method)
                .add("placeholder", Messages.METHOD));
        html.input(class_("inputBox form-control")
                .name(STEPTYPE).value("")
                .value(this.type)
                .add("placeholder", Messages.TYPE));
        // Add the on/off values
        html.select(class_("holo-spinner").name(MODE).id(MODE));
        html.option(value(""))
                .write("")
                ._option();
        html.option(value(TemperatureTrigger.INCREASE)
                .selected_if(
                        this.mode.equals(TemperatureTrigger.INCREASE)))
                .write(TemperatureTrigger.INCREASE)
                ._option();
        html.option(value(TemperatureTrigger.DECREASE)
                .selected_if(
                        this.mode.equals(TemperatureTrigger.DECREASE)))
                .write(TemperatureTrigger.DECREASE)
                ._option();
        html._select();
        html.button(name("submitTemperature")
                .class_("btn col-md-12")
                .add("data-toggle", "clickover")
                .onClick("updateTriggerStep(this);"))
                .write(Messages.ADD_TRIGGER)
                ._button();
        html._form();
        html._div();
        return html;
    }

    @Override
    public String getName() {
        return "Temperature";
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
     * This is for Any device.
     *
     * @return any
     */
    @Override
    public final boolean getTriggerType(String inType) {
        return true;
    }

    public void setTargetTemperature(double stepStartTemp) {
        this.targetTemp = new BigDecimal(stepStartTemp);
    }

    public void setExitTemp(double exitTemp) {
        this.exitTemp = new BigDecimal(exitTemp);
    }

    public BigDecimal getExitTemp() {
        return this.exitTemp;
    }

    private void createNotifications(String s) {
        if (this.webNotification != null) {
            //Clear the existing notifications
            clearNotifications();
        }
        this.webNotification = new WebNotification();
        this.webNotification.setMessage(s);
        this.webNotification.sendNotification();
        Notifications.getInstance().addNotification(this.webNotification);
    }

    private void clearNotifications() {
        if (this.webNotification == null) {
            return;
        }
        Notifications.getInstance().clearNotification(this.webNotification);
    }
}
