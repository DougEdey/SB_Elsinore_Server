package com.sb.elsinore.graphql;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.models.TemperatureModel;
import com.sb.elsinore.repositories.TemperatureRepository;
import com.sb.elsinore.wrappers.TempRunner;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import org.springframework.stereotype.Service;

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

    @GraphQLQuery(name="availableProbes")
    public List<TempProbe> availableProbes() {
        return this.launchControl.availableProbes();
    }

    @GraphQLQuery(name = "findTempRunnerByName")
    public TempRunner findTempRunnerByName(String name) {
        return this.launchControl.findTemp(name);
    }

    @GraphQLQuery(name = "tempRunners")
    public List<TempRunner> tempRunners() {
        return this.launchControl.getTempRunners();
    }

    @GraphQLQuery(name = "temperatureModels")
    public List<TemperatureModel> getTemperatures() {
        return this.temperatureRepository.findAll();
    }

    @GraphQLQuery(name = "tempProbes")
    public Collection<TempProbe> getTempProbes() {
        return this.launchControl.getTempProbes();
    }

    @GraphQLMutation(name = "addTemperatureProbe")
    public TempRunner addTemperatureProbe(String name, String device) {
        this.temperatureRepository.save(new TemperatureModel(name, device));
        return findTempRunnerByName(name);

    }

    @GraphQLMutation(name = "deleteProbe")
    public Long deleteProbe(Long id) {
        TemperatureModel toDelete = this.temperatureRepository.findById(id).orElse(null);

        if (toDelete == null) {
            throw new RuntimeException("No Probe with Id: " + id);
        }
        this.temperatureRepository.delete(toDelete);
        return id;
    }

    @GraphQLMutation(name= "updateProbe")
    public TempRunner updateProbe(TemperatureModel probe) {
        this.temperatureRepository.saveAndFlush(probe);
        return findTempRunnerByName(probe.getName());
    }

}
