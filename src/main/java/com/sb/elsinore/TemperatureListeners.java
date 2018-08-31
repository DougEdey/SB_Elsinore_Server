package com.sb.elsinore;


import com.sb.elsinore.models.TemperatureModel;
import org.springframework.stereotype.Component;

import javax.persistence.PostPersist;
import javax.persistence.PreRemove;

@Component
public class TemperatureListeners {

    @PreRemove
    public void tempRemoved(TemperatureModel tempProbe) {
        LaunchControl.getInstance().deleteTemp(tempProbe);
    }

    @PostPersist
    public void tempAdded(TemperatureModel tempProbe) {
        LaunchControl.getInstance().addTemp(tempProbe);
    }
}
