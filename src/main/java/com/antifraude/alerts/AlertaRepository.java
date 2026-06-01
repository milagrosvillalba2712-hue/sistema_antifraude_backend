package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByEstado(String estado);

    List<Alerta> findByPrioridad(String prioridad);

    List<Alerta> findByTransaccionId(Long transaccionId);

    long countByEstado(String estado);

    @Query("SELECT a.estado, COUNT(a) FROM Alerta a GROUP BY a.estado")
    List<Object[]> countByEstadoGrouped();

    @Query("SELECT a.prioridad, COUNT(a) FROM Alerta a GROUP BY a.prioridad")
    List<Object[]> countByPrioridadGrouped();
}
