package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultaKycAlertaRepository extends JpaRepository<ConsultaKycAlerta, Long> {
    List<ConsultaKycAlerta> findByAlertaIdOrderByFechaConsultaDesc(Long alertaId);
}
