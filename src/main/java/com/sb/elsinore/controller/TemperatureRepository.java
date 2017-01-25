package com.sb.elsinore.controller;

import com.sb.elsinore.Temp;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Created by doug on 25/01/17.
 */
@RepositoryRestResource(collectionResourceRel = "temperatures", path = "temperatures")
public interface TemperatureRepository extends PagingAndSortingRepository<Temp, Long> {
}
