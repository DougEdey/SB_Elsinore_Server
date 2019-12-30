package com.sb.elsinore.repositories;

import com.sb.elsinore.models.Timer;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "timers", path = "timers")
public interface TimerRepository extends PagingAndSortingRepository<Timer, Long> {
}
