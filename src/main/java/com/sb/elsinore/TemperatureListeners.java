package com.sb.elsinore;


import com.sb.elsinore.models.TemperatureModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.PostPersist;
import javax.persistence.PreRemove;

@Component
public class TemperatureListeners {

    @Autowired
    private LaunchControl launchControl;

    public TemperatureListeners() {
    }

    @Autowired
    public TemperatureListeners(LaunchControl launchControl) {
        this.launchControl = launchControl;
    }

    @PreRemove
    public void tempRemoved(TemperatureModel tempProbe) {
        this.launchControl.deleteTemp(tempProbe);
    }

    @PostPersist
    public void tempAdded(TemperatureModel tempProbe) {
        this.launchControl.addTemp(tempProbe);
    }
}
