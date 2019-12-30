package com.sb.elsinore;

import com.sb.elsinore.models.TemperatureModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.persistence.PostPersist;
import javax.persistence.PreRemove;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class TemperatureListeners {

    @Autowired
    static private LaunchControl launchControl;

    public TemperatureListeners() {
    }

    @Autowired
    public TemperatureListeners(LaunchControl launchControl) {
        TemperatureListeners.launchControl = launchControl;
    }

    @PreRemove
    public void tempRemoved(TemperatureModel tempProbe) {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        TemperatureListeners.launchControl.deleteTemp(tempProbe);
    }

    @PostPersist
    public void tempAdded(TemperatureModel tempProbe) {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        TemperatureListeners.launchControl.addTemp(tempProbe);
    }

    @Autowired
    public void init(LaunchControl launchControl) 
    {
        TemperatureListeners.launchControl = launchControl;
    }
}
