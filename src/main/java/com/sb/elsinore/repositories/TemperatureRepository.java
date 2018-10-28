package com.sb.elsinore.repositories;

import com.sb.elsinore.models.TemperatureModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * Created by doug on 25/01/17.
 */
@RepositoryRestResource(collectionResourceRel = "temperatures", path = "temperatures")
@Repository
public interface TemperatureRepository extends JpaRepository<TemperatureModel, Long> {

    TemperatureModel findByNameIgnoreCase(@Param("name") String name);

    TemperatureModel findByDeviceIgnoreCase(@Param("device") String device);

}
