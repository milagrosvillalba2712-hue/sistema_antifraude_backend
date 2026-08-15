package com.antifraude.common.repository;

import com.antifraude.common.entity.Pais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaisRepository extends JpaRepository<Pais, Long> {
    Optional<Pais> findByCodigoIso(String codigoIso);
    Optional<Pais> findByNombre(String nombre);
    boolean existsByCodigoIso(String codigoIso);
}
