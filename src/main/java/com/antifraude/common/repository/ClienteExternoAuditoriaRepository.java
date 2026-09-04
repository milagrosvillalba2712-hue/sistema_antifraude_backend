package com.antifraude.common.repository;

import com.antifraude.common.entity.ClienteExternoAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClienteExternoAuditoriaRepository extends JpaRepository<ClienteExternoAuditoria, Long> {

    Page<ClienteExternoAuditoria> findByClienteExternoIdOrderByFechaHoraCreacionDesc(UUID clienteExternoId, Pageable pageable);
}
