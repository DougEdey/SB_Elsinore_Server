package com.sb.elsinore.graphql;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.models.TemperatureModel;
import com.sb.elsinore.repositories.TemperatureRepository;
import io.leangen.graphql.annotations.GraphQLQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TempProbeService {
    private final TemperatureRepository temperatureRepository;

    public TempProbeService(TemperatureRepository temperatureRepository) {
        this.temperatureRepository = temperatureRepository;
    }

    @GraphQLQuery(name = "temperatures")
    public List<TemperatureModel> getTemperatures() {
        return this.temperatureRepository.findAll();
    }

    @GraphQLQuery(name = "tempProbes")
    public List<TempProbe> getTempProbes() {
        return LaunchControl.getInstance().getTempProbes();
    }

}
