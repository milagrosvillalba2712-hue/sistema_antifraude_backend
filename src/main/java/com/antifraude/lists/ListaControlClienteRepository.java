package com.antifraude.lists;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListaControlClienteRepository extends JpaRepository<ListaControlCliente, Long> {
    List<ListaControlCliente> findByEmpresaIdOrderByTipoListaAscNombreAsc(UUID empresaId);
    Optional<ListaControlCliente> findByIdAndEmpresaId(Long id, UUID empresaId);
}

