package com.antifraude.lists;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ImportacionListaControlClienteRepository extends JpaRepository<ImportacionListaControlCliente, Long> {
    List<ImportacionListaControlCliente> findByEmpresaIdOrderByFechaHoraCreacionDesc(UUID empresaId);
}

