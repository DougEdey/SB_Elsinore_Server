package com.sb.elsinore.triggers;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.TriggerControl;
import com.sb.elsinore.repositories.TemperatureRepository;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class ProfileTrigger implements TriggerInterface {

    private static final String TARGET_NAME = "targetname";
    private static final String ACTIVATE = "activate";
    private static final String DISABLE = "disable";
    private Logger logger = LoggerFactory.getLogger(ProfileTrigger.class);
    private Integer position = -1;
    private boolean activate = false;
    private boolean active = false;
    private String targetName = null;
    private Date startDate = null;
    private TemperatureRepository temperatureRepository;
    private LaunchControl launchControl;

    public ProfileTrigger() {
    }

    public ProfileTrigger(final int newPos) {
        this.position = newPos;
    }

    public ProfileTrigger(final int newPos, final JSONObject jsonObject) {
        this.position = newPos;
        updateTrigger(jsonObject);
    }

    @Autowired
    public void setLaunchControl(LaunchControl launchControl) {
        this.launchControl = launchControl;
    }

    @Autowired
    public void setTemperatureRepository(TemperatureRepository temperatureRepository) {
        this.temperatureRepository = temperatureRepository;
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

        TriggerControl triggerControl = this.launchControl.findTriggerControl(
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
            this.logger.info("Activated {} step at {}", this.targetName, stepToUse);
        } else {
            triggerControl.deactivateTrigger(stepToUse);
            this.logger.info("Deactivated {} step at {}", this.targetName, stepToUse);
        }

        this.launchControl.startMashControl(this.targetName);
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

        if (target != null && this.temperatureRepository.findByNameIgnoreCase(target) != null) {
            this.targetName = target;
        } else {
            return false;
        }

        this.activate = newAct != null && newAct.equals(ACTIVATE);
        return true;
    }

}
