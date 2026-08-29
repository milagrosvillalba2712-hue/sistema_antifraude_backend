package com.antifraude.common.repository;

import com.antifraude.common.entity.ConfiguracionDrools;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionDroolsRepository extends JpaRepository<ConfiguracionDrools, String> {
}
