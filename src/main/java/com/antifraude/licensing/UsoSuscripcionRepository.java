package com.antifraude.licensing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsoSuscripcionRepository extends JpaRepository<UsoSuscripcion, Long> {
    List<UsoSuscripcion> findByEmpresaIdOrderByAnioDescMesDesc(UUID empresaId);

    Optional<UsoSuscripcion> findByEmpresaIdAndAnioAndMes(UUID empresaId, int anio, int mes);

    Optional<UsoSuscripcion> findFirstByEmpresaIdAndAnioAndMesOrderByIdDesc(UUID empresaId, int anio, int mes);

    Optional<UsoSuscripcion> findFirstByEmpresaIdAndSuscripcionIdAndPeriodoOrderByIdDesc(UUID empresaId, Long suscripcionId, LocalDate periodo);

    List<UsoSuscripcion> findBySuscripcionIdOrderByAnioDescMesDesc(Long suscripcionId);
}
