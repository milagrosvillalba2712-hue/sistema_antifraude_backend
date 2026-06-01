package com.antifraude.dashboard;

import com.antifraude.alerts.AlertaRepository;
import com.antifraude.dto.DashboardResponse;
import com.antifraude.transactions.TransaccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final TransaccionRepository transaccionRepository;
    private final AlertaRepository alertaRepository;

    public DashboardService(TransaccionRepository transaccionRepository, AlertaRepository alertaRepository) {
        this.transaccionRepository = transaccionRepository;
        this.alertaRepository = alertaRepository;
    }

    public DashboardResponse obtenerDashboard() {
        log.debug("[DASHBOARD] Generando dashboard");
        long totalTransacciones = transaccionRepository.count();
        long transaccionesSospechosas = transaccionRepository.countByEstado("SOSPECHOSA");
        long alertasPendientes = alertaRepository.countByEstado("PENDIENTE");
        long alertasResueltas = alertaRepository.countByEstado("RESUELTA");

        Map<String, Long> transaccionesPorEstado = new HashMap<>();
        transaccionesPorEstado.put("APROBADA", transaccionRepository.countByEstado("APROBADA"));
        transaccionesPorEstado.put("REVISION", transaccionRepository.countByEstado("REVISION"));
        transaccionesPorEstado.put("SOSPECHOSA", transaccionesSospechosas);

        Map<String, Long> alertasPorPrioridad = new HashMap<>();
        alertaRepository.countByPrioridadGrouped().forEach(obj ->
                alertasPorPrioridad.put((String) obj[0], (Long) obj[1]));

        log.debug("[DASHBOARD] Transacciones: {} | Sospechosas: {} | Alertas pendientes: {} | Resueltas: {}",
                totalTransacciones, transaccionesSospechosas, alertasPendientes, alertasResueltas);

        return new DashboardResponse(
                totalTransacciones, transaccionesSospechosas, alertasPendientes, alertasResueltas,
                transaccionRepository.promedioScoreRiesgo(),
                transaccionesPorEstado, alertasPorPrioridad);
    }
}
