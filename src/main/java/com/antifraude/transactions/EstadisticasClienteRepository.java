package com.antifraude.transactions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadisticasClienteRepository extends JpaRepository<EstadisticasCliente, Long> {

    Optional<EstadisticasCliente> findByIdentificadorDocumento(String identificadorDocumento);
}
