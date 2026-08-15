package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoincidenciaListaAlertaRepository extends JpaRepository<CoincidenciaListaAlerta, Long> {
    List<CoincidenciaListaAlerta> findByAlertaIdOrderByFechaRegistroDesc(Long alertaId);
    List<CoincidenciaListaAlerta> findByTransaccionIdOrderByFechaRegistroDesc(Long transaccionId);
}
