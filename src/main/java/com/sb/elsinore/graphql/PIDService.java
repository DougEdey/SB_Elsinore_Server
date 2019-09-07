package com.sb.elsinore.graphql;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.models.PIDModel;
import com.sb.elsinore.repositories.PIDRepository;

import org.springframework.stereotype.Service;

import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;

@Service
public class PIDService {
    private final PIDRepository pidRepository;
    private final LaunchControl launchControl;

    public PIDService(LaunchControl launchControl, PIDRepository pidRepository) {
        this.pidRepository = pidRepository;
        this.launchControl = launchControl;
    }

    @GraphQLQuery(name="pidModelById")
    public PIDModel getPIDModelById(Long id) {
        return pidRepository.findById(id).orElse(null);
    }

    @GraphQLQuery(name="pidModelByName")
    public PIDModel getPIDModelByName(String name) {
        return pidRepository.findByTemperatureNameIgnoreCase(name);
    }

    @GraphQLMutation(name="updatePIDModel")
    public PIDModel updatePIDModel(PIDModel pidModel) {
        if (pidModel.getId() != null) {
            
        }
        return pidRepository.saveAndFlush(pidModel);
    }
}