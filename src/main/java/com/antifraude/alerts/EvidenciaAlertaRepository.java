package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenciaAlertaRepository extends JpaRepository<EvidenciaAlerta, Long> {
    List<EvidenciaAlerta> findByAlertaIdOrderByFechaCargaDesc(Long alertaId);
}
