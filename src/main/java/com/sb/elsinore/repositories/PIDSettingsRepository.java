package com.sb.elsinore.repositories;

import com.sb.elsinore.models.PIDSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "pidSettings", path = "pidSettings")
public interface PIDSettingsRepository extends JpaRepository<PIDSettings, Long> {
}
