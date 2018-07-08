package com.sb.elsinore.triggers;

import com.sb.elsinore.*;
import com.sb.elsinore.controller.DeviceRepository;
import com.sb.elsinore.controller.TemperatureRepository;
import com.sb.elsinore.devices.TempProbe;
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
    private DeviceRepository deviceRepository;
    private TemperatureRepository temperatureRepository;

    public void setDeviceRepository(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public void setTemperatureRepository(TemperatureRepository temperatureRepository) {
        this.temperatureRepository = temperatureRepository;
    }

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

    @Override
    public void setPosition(Integer newPos) {
        this.position = newPos;
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

        if (target != null && this.temperatureRepository.findByName(target) != null) {
            this.targetName = target;
        } else {
            return false;
        }

        this.activate = newAct != null && newAct.equals(ACTIVATE);
        return true;
    }

}
