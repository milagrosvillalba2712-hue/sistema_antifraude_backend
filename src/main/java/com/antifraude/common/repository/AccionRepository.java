package com.antifraude.common.repository;

import com.antifraude.common.entity.Accion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccionRepository extends JpaRepository<Accion, Long> {
    Optional<Accion> findByCodigo(String codigo);
}
