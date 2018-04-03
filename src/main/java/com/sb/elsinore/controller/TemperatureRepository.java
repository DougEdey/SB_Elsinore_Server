package com.sb.elsinore.controller;

import com.sb.elsinore.devices.TempProbe;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * Created by doug on 25/01/17.
 */
@RepositoryRestResource(collectionResourceRel = "temperatures", path = "temperatures")
public interface TemperatureRepository extends PagingAndSortingRepository<TempProbe, Long> {

    List<TempProbe> findByName(@Param("name") String name);

    List<TempProbe> findByDevice(@Param("device") String device);
    
}
