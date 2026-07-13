package com.antifraude.common.repository;

import com.antifraude.common.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    @Query("SELECT d FROM Documento d WHERE d.persona.id = :personaId")
    List<Documento> findByPersonaId(@Param("personaId") Long personaId);
}
