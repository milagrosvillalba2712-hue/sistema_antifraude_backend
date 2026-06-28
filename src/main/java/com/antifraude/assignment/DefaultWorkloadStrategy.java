package com.antifraude.assignment;

import com.antifraude.alerts.Alerta;
import com.antifraude.alerts.AlertaRepository;
import com.antifraude.alerts.EstadisticaCargaAnalista;
import com.antifraude.alerts.EstadisticaCargaAnalistaRepository;
import com.antifraude.users.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class DefaultWorkloadStrategy implements AssignmentStrategy {

    private static final Logger log = LoggerFactory.getLogger(DefaultWorkloadStrategy.class);

    private final AlertaRepository alertaRepository;
    private final EstadisticaCargaAnalistaRepository cargaRepository;

    public DefaultWorkloadStrategy(AlertaRepository alertaRepository,
                                    EstadisticaCargaAnalistaRepository cargaRepository) {
        this.alertaRepository = alertaRepository;
        this.cargaRepository = cargaRepository;
    }

    @Override
    public Usuario asignar(Alerta alerta, List<Usuario> candidatos) {
        log.info("[ASSIGNMENT] Evaluando {} candidatos para alerta ID: {}", candidatos.size(), alerta.getId());

        return candidatos.stream()
                .map(analista -> new CandidateScore(analista, calcularCarga(analista),
                        calcularTiempoPromedio(analista), calcularAsignacionesHoy(analista)))
                .sorted(Comparator
                        .comparingInt(CandidateScore::carga)
                        .thenComparingLong(CandidateScore::tiempoPromedio)
                        .thenComparingInt(CandidateScore::asignacionesHoy))
                .peek(c -> log.debug("[ASSIGNMENT] Candidato: {} - Carga: {} - TiempoProm: {} - AsigHoy: {}",
                        c.analista().getNombre(), c.carga(), c.tiempoPromedio(), c.asignacionesHoy()))
                .findFirst()
                .map(CandidateScore::analista)
                .orElse(null);
    }

    private int calcularCarga(Usuario analista) {
        return (int) alertaRepository.countByAsignadoAIdAndEstadoIn(
                analista.getId(),
                List.of("PENDIENTE", "ASIGNADA", "INVESTIGANDO"));
    }

    private long calcularTiempoPromedio(Usuario analista) {
        Optional<EstadisticaCargaAnalista> stats = cargaRepository
                .findByUsuarioIdAndFecha(analista.getId(), LocalDate.now());
        return stats.map(EstadisticaCargaAnalista::getTiempoPromedioResolucion).orElse(0L);
    }

    private int calcularAsignacionesHoy(Usuario analista) {
        Optional<EstadisticaCargaAnalista> stats = cargaRepository
                .findByUsuarioIdAndFecha(analista.getId(), LocalDate.now());
        return stats.map(EstadisticaCargaAnalista::getAlertasAsignadas).orElse(0);
    }

    private record CandidateScore(Usuario analista, int carga, long tiempoPromedio, int asignacionesHoy) {}
}
