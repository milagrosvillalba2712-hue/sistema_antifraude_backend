package com.antifraude.common.repository;

import com.antifraude.common.entity.Caso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CasoRepository extends JpaRepository<Caso, Long> {
    Optional<Caso> findByCodigo(String codigo);
    
    List<Caso> findByEstado(Caso.EstadoCaso estado);

    long countByEstado(Caso.EstadoCaso estado);
    
    @Query("SELECT c FROM Caso c WHERE c.usuarioAnalista.id = :analistaId AND c.estado NOT IN ('CERRADO', 'RESUELTO')")
    List<Caso> findOpenCasesByAnalista(UUID analistaId);
    
    @Query("SELECT COUNT(c) FROM Caso c WHERE c.usuarioAnalista.id = :analistaId AND c.estado NOT IN ('CERRADO', 'RESUELTO')")
    Long countOpenCasesByAnalista(UUID analistaId);
}
