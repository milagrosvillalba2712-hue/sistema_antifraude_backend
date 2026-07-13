package com.antifraude.common.repository;

import com.antifraude.common.entity.PaisRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaisRiesgoRepository extends JpaRepository<PaisRiesgo, Long> {
    @Query("SELECT pr FROM PaisRiesgo pr WHERE pr.activo = true")
    List<PaisRiesgo> findAllActive();
}
