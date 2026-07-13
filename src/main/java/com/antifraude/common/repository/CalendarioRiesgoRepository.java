package com.antifraude.common.repository;

import com.antifraude.common.entity.CalendarioRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CalendarioRiesgoRepository extends JpaRepository<CalendarioRiesgo, Long> {
    Optional<CalendarioRiesgo> findByFecha(LocalDate fecha);
}
