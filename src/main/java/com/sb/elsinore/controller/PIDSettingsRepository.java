package com.sb.elsinore.controller;

import com.sb.elsinore.PIDSettings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "pidSettings", path = "pidSettings")
public interface PIDSettingsRepository extends PagingAndSortingRepository<PIDSettings, Long> {
}
