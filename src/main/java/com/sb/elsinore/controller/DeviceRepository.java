package com.sb.elsinore.controller;

import com.sb.elsinore.Device;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * This is the controller for PID and temp probe
 * Created by doug on 01/01/17.
 */
@RepositoryRestResource(collectionResourceRel = "devices", path = "devices")
public interface DeviceRepository extends PagingAndSortingRepository<Device, Long> {
    /**
     * Get a list of devices based on the type
     *
     * @param type The type of device to look for
     * @return The list of devices, may be empty
     */
    List<Device> findByType(@Param("type") String type);
}
