package com.sb.elsinore;


import com.sb.elsinore.devices.TempProbe;

import javax.persistence.PostPersist;
import javax.persistence.PreRemove;


public class TemperatureListeners {
    @PreRemove
    public void tempRemoved(TempProbe tempProbe) {
        LaunchControl.getInstance().deleteTemp(tempProbe);
    }

    @PostPersist
    public void tempAdded(TempProbe tempProbe) {
        LaunchControl.getInstance().addTemp(tempProbe);
    }
}
