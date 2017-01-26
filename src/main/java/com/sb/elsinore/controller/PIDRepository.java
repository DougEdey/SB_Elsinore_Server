package com.sb.elsinore.controller;

import com.sb.elsinore.PID;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * Created by doug on 25/01/17.
 */
@RepositoryRestResource(collectionResourceRel = "pids", path = "pids")
public interface PIDRepository extends PagingAndSortingRepository<PID, Long> {
    List<PID> findByName(@Param("name") String name);
}
