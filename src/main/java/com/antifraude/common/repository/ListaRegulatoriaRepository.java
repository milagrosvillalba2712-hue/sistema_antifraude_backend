package com.antifraude.common.repository;

import com.antifraude.common.entity.ListaRegulatoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ListaRegulatoriaRepository extends JpaRepository<ListaRegulatoria, Long> {
    Optional<ListaRegulatoria> findByCodigo(String codigo);
}
