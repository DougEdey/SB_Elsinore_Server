package com.sb.elsinore.repositories;

import com.sb.elsinore.models.Switch;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "switches", path = "switches")
public interface SwitchRepository extends PagingAndSortingRepository<Switch, Long> {
}
