package com.sb.elsinore.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"com.sb.elsinore.repositories"})
@EntityScan(basePackages = {"com.sb.elsinore.models"})
public class JpaDataConfiguration {
}
