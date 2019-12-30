package com.sb.elsinore.repositories;

import com.sb.elsinore.models.OutputDeviceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "output_devices", path = "output_devices")
public interface OutputDeviceRepository extends JpaRepository<OutputDeviceModel, Long> {
}
