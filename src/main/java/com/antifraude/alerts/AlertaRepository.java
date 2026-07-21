package com.antifraude.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long>, JpaSpecificationExecutor<Alerta> {

    List<Alerta> findAllByOrderByFechaGeneracionDesc();

    List<Alerta> findByEstado(String estado);

    List<Alerta> findByPrioridad(String prioridad);

    List<Alerta> findByTransaccionId(Long transaccionId);

    long countByEstado(String estado);

    long countByAsignadoAIsNullAndEstado(String estado);

    long countByAsignadoAIdAndEstadoIn(Long asignadoAId, List<String> estados);

    @Query("SELECT a.estado, COUNT(a) FROM Alerta a GROUP BY a.estado")
    List<Object[]> countByEstadoGrouped();

    @Query("SELECT a.prioridad, COUNT(a) FROM Alerta a GROUP BY a.prioridad")
    List<Object[]> countByPrioridadGrouped();
}
