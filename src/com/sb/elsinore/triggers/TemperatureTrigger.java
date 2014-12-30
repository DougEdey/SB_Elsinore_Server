package com.sb.elsinore.triggers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.joda.time.DateTime;
import org.json.simple.JSONObject;

import com.sb.elsinore.BrewServer;
import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Temp;

public class TemperatureTrigger implements TriggerInterface {

    private BigDecimal targetTemp = null;
    private BigDecimal duration = null;
    private Temp temperatureProbe = null;
    private String mode = null;
    private ActionInterface nextAction = null;
    private boolean active;
    private int position = -1;
    public static String INCREASE = "INCREASE";
    public static String DECREASE = "DECREASE";

    public void setTargetTemperature(BigDecimal targetTemp) {
        this.targetTemp = targetTemp;
    }

    public void setTemperatureProbe(String name) {
        temperatureProbe = LaunchControl.findTemp(name);
    }

    public TemperatureTrigger(int position, JSONObject parameters) {
        this.position = position;
        BigDecimal duration = new BigDecimal(
              parameters.get("duration").toString().replace(",", "."));
        BigDecimal temp = new BigDecimal(
              parameters.get("temp").toString().replace(",", "."));

        String method = parameters.get("method").toString();
        String type = parameters.get("type").toString();
        String tempProbe = parameters.get("tempProbe").toString();

        this.targetTemp = temp;
        setTemperatureProbe(tempProbe);
        this.mode = method + ": " + type;
        this.duration = duration;
    }

    @Override
    public void waitForTrigger() {
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

        BrewServer.LOG.info ("Setting start date");
        // We've hit the target step temp, set the start date
        DateTime tDate = new DateTime();
        setStart(tDate.toDate());
        Date cDate = new Date();
        // set the target End Date stamp
        BigInteger minutes = getDuration().toBigInteger();
        tDate = tDate.plusMinutes(minutes.intValue());
        setTargetEnd(tDate.toDate());
        // Have we gone past this mashStep Duration?
        if (getTargetEnd() != null
            && cDate.compareTo(getTargetEnd()) >= 0) {
            BrewServer.LOG.warning("End Temp hit");
            // Over or equal to the target end time, deactivate
            this.active = false;
        }

    }

    @Override
    public void setAction(ActionInterface action) {
        this.nextAction = action;
    }

    @Override
    public ActionInterface getAction() {
        return this.nextAction;
    }

    @Override
    public JSONObject getJsonStatus() {
        JSONObject currentStatus = new JSONObject();
        currentStatus.put("type", "temperature");
        currentStatus.put("mode", this.mode);
        currentStatus.put("targetTemperature",
                this.targetTemp.toString());
        currentStatus.put("temperatureProbe",
                this.temperatureProbe.getMapStatus());
        return currentStatus;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }
    
    @Override
    public int getPosition() {
        return this.position;
    }

    @Override
    public void setActive() {
        this.active = true;
    }

    @Override
    public void deactivate() {
        this.active = false;
    }
}
