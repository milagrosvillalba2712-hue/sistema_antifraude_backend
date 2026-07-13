package com.antifraude.common.repository;

import com.antifraude.common.entity.HorarioRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioRiesgoRepository extends JpaRepository<HorarioRiesgo, Long> {
    @Query("SELECT h FROM HorarioRiesgo h WHERE h.activo = true")
    List<HorarioRiesgo> findAllActive();
}
