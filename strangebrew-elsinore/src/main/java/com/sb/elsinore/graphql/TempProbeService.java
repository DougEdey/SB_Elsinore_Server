package com.sb.elsinore.graphql;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.models.TemperatureModel;
import com.sb.elsinore.repositories.TemperatureRepository;
import com.sb.elsinore.wrappers.TempRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Service
public class TempProbeService {
    private final TemperatureRepository temperatureRepository;
    private final LaunchControl launchControl;

    public TempProbeService(TemperatureRepository temperatureRepository, LaunchControl launchControl) {
        this.temperatureRepository = temperatureRepository;
        this.launchControl = launchControl;
    }

    public List<TempProbe> availableProbes() {
        return this.launchControl.availableProbes();
    }

    public TempRunner findTempRunnerByName(String name) {
        return this.launchControl.findTemp(name);
    }

    public List<TempRunner> tempRunners() {
        return this.launchControl.getTempRunners();
    }

    public List<TemperatureModel> getTemperatures() {
        return this.temperatureRepository.findAll();
    }

    public TemperatureModel getTemperature(Long id) {
        return this.temperatureRepository.findById(id).orElse(null);
    }

    public Collection<TempProbe> getTempProbes() {
        return this.launchControl.getTempProbes();
    }

    public TempRunner addTemperatureProbe(String name, String device) {
        this.temperatureRepository.save(new TemperatureModel(name, device));
        return findTempRunnerByName(name);
    }

    public TempRunner setDefaultTemperature(String name, BigDecimal temperature) {
        TempRunner tempRunner = findTempRunnerByName(name);
        tempRunner.setDefaultTemperature(temperature);
        return tempRunner;
    }

    public Long deleteProbe(Long id) {
        TemperatureModel toDelete = this.temperatureRepository.findById(id).orElse(null);

        if (toDelete == null) {
            throw new RuntimeException("No Probe with Id: " + id);
        }
        this.temperatureRepository.delete(toDelete);
        return id;
    }

    public TempRunner updateProbe(TemperatureModel probe) {
        this.temperatureRepository.saveAndFlush(probe);
        return findTempRunnerByName(probe.getName());
    }

}
