package com.antifraude.common.repository;

import com.antifraude.common.entity.ClienteExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteExternoRepository extends JpaRepository<ClienteExterno, UUID> {

    Optional<ClienteExterno> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<ClienteExterno> findByEmpresaIdOrderByNombreAsc(UUID empresaId);

    List<ClienteExterno> findByActivoTrueOrderByNombreAsc();

    @Query("SELECT c FROM ClienteExterno c WHERE c.apiKeyPrefix = :prefix AND c.activo = true")
    Optional<ClienteExterno> findByApiKeyPrefixAndActivoTrue(@Param("prefix") String prefix);

    @Modifying
    @Query("UPDATE ClienteExterno c SET c.fechaUltimoUso = :fecha WHERE c.id = :id")
    void actualizarFechaUltimoUso(@Param("id") UUID id, @Param("fecha") OffsetDateTime fecha);
}
