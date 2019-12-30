package com.sb.elsinore.repositories;

import com.sb.elsinore.models.I2CSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "i2c_settings", path = "i2c_settings")
public interface I2CSettingsRepository extends JpaRepository<I2CSettings, Long> {
}
