package com.antifraude.common.repository;

import com.antifraude.common.entity.CasoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CasoAlertaRepository extends JpaRepository<CasoAlerta, Long> {
    List<CasoAlerta> findByCasoId(Long casoId);
    List<CasoAlerta> findByAlertaId(Long alertaId);
}
