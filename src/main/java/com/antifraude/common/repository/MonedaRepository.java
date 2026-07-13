package com.antifraude.common.repository;

import com.antifraude.common.entity.Moneda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonedaRepository extends JpaRepository<Moneda, Long> {
    Optional<Moneda> findByCodigoIso(String codigoIso);
    Optional<Moneda> findByNombre(String nombre);
}
