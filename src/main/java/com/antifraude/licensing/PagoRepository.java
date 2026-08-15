package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByEmpresaId(UUID empresaId);
}
