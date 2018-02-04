package com.sb.elsinore;


import javax.persistence.PostPersist;
import javax.persistence.PreRemove;


public class TemperatureListeners {
    @PreRemove
    public void tempRemoved(Temp temp) {
        LaunchControl.getInstance().deleteTemp(temp);
    }

    @PostPersist
    public void tempAdded(Temp temp) {
        LaunchControl.getInstance().addTemp(temp);
    }
}
