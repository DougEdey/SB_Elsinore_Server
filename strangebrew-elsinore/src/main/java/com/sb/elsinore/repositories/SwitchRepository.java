package com.sb.elsinore.repositories;

import com.sb.elsinore.models.SwitchModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "switches", path = "switches")
public interface SwitchRepository extends JpaRepository<SwitchModel, Long> {
}
