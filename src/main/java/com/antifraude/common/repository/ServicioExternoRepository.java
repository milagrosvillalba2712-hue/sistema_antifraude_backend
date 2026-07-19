package com.antifraude.common.repository;

import com.antifraude.common.entity.ServicioExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServicioExternoRepository extends JpaRepository<ServicioExterno, Long> {
    Optional<ServicioExterno> findByCodigo(String codigo);
}
