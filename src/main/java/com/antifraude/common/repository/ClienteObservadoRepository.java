package com.antifraude.common.repository;

import com.antifraude.common.entity.ClienteObservado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteObservadoRepository extends JpaRepository<ClienteObservado, Long> {
    @Query("SELECT c FROM ClienteObservado c WHERE c.persona.id = :personaId AND c.activo = true")
    List<ClienteObservado> findByPersonaId(@Param("personaId") Long personaId);
}
