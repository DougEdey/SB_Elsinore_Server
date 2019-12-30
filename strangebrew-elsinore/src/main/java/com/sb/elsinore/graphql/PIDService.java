package com.sb.elsinore.graphql;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.models.PIDModel;
import com.sb.elsinore.repositories.PIDRepository;

import org.springframework.stereotype.Service;

import graphql.schema.DataFetcher;

@Service
public class PIDService {
    private final PIDRepository pidRepository;
    private final LaunchControl launchControl;

    public PIDService(LaunchControl launchControl, PIDRepository pidRepository) {
        this.pidRepository = pidRepository;
        this.launchControl = launchControl;
    }

    public DataFetcher getPIDModelById() {
        return dataFetchingEnvironment -> {
            String id = dataFetchingEnvironment.getArgument("id");
            Long pidId = Long.parseLong(id);
            return pidRepository.findById(pidId).orElse(null);
        };
    }

    public PIDModel getPIDModelByName(String name) {
        return pidRepository.findByTemperatureNameIgnoreCase(name);
    }

    public PIDModel updatePIDModel(PIDModel pidModel) {
        if (pidModel.getId() != null) {
            
        }
        return pidRepository.saveAndFlush(pidModel);
    }
}