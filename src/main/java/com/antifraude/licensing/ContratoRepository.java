package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    boolean existsByNumero(String numero);
}
