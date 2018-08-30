package com.sb.elsinore;


import com.sb.elsinore.models.Temperature;
import org.springframework.stereotype.Component;

import javax.persistence.PostPersist;
import javax.persistence.PreRemove;

@Component
public class TemperatureListeners {

    @PreRemove
    public void tempRemoved(Temperature tempProbe) {
        LaunchControl.getInstance().deleteTemp(tempProbe);
    }

    @PostPersist
    public void tempAdded(Temperature tempProbe) {
        LaunchControl.getInstance().addTemp(tempProbe);
    }
}
