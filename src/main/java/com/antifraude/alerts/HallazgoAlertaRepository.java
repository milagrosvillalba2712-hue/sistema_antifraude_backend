package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HallazgoAlertaRepository extends JpaRepository<HallazgoAlerta, Long> {
    List<HallazgoAlerta> findByAlertaIdOrderByFechaRegistroDesc(Long alertaId);
}
