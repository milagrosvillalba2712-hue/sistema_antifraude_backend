package com.antifraude.assignment;

import com.antifraude.alerts.*;
import com.antifraude.audit.AuditoriaService;
import com.antifraude.exception.BusinessException;
import com.antifraude.profile.DisponibilidadService;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AssignmentEngine {

    private static final Logger log = LoggerFactory.getLogger(AssignmentEngine.class);

    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadService disponibilidadService;
    private final AlertaRepository alertaRepository;
    private final HistorialAsignacionRepository historialRepository;
    private final EstadisticaCargaAnalistaRepository cargaRepository;
    private final AuditoriaService auditoriaService;
    private final AssignmentStrategy strategy;

    public AssignmentEngine(UsuarioRepository usuarioRepository,
                             DisponibilidadService disponibilidadService,
                             AlertaRepository alertaRepository,
                             HistorialAsignacionRepository historialRepository,
                             EstadisticaCargaAnalistaRepository cargaRepository,
                             AuditoriaService auditoriaService,
                             AssignmentStrategy strategy) {
        this.usuarioRepository = usuarioRepository;
        this.disponibilidadService = disponibilidadService;
        this.alertaRepository = alertaRepository;
        this.historialRepository = historialRepository;
        this.cargaRepository = cargaRepository;
        this.auditoriaService = auditoriaService;
        this.strategy = strategy;
    }

    public Usuario asignar(Alerta alerta) {
        log.info("[ASSIGNMENT] Iniciando asignacion para alerta ID: {}", alerta.getId());

        List<Usuario> analistas = usuarioRepository.findAll().stream()
                .filter(u -> "ANALISTA".equals(u.getRol()) && u.getActivo())
                .filter(u -> disponibilidadService.estaDisponible(u.getId()))
                .toList();

        if (analistas.isEmpty()) {
            log.warn("[ASSIGNMENT] No hay analistas disponibles para alerta ID: {}", alerta.getId());
            throw new BusinessException("NO_ANALYSTS", "No hay analistas disponibles para asignar");
        }

        Usuario asignado = strategy.asignar(alerta, analistas);
        log.info("[ASSIGNMENT] Analista seleccionado: {} para alerta ID: {}", asignado.getEmail(), alerta.getId());
        return asignado;
    }

    public Alerta asignarAutomaticamente(Long alertaId, HttpServletRequest request) {
        log.info("[ASSIGNMENT] Auto-asignando alerta ID: {}", alertaId);
        Alerta alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new com.antifraude.exception.ResourceNotFoundException("Alerta", "id", alertaId));

        if (alerta.getAsignadoA() != null) {
            throw new BusinessException("ALREADY_ASSIGNED", "La alerta ya tiene analista asignado");
        }

        Usuario analista = asignar(alerta);

        alerta.setAsignadoA(analista);
        alerta.setEstado("ASIGNADA");
        alerta.setFechaAsignacion(LocalDateTime.now());
        alertaRepository.save(alerta);

        HistorialAsignacion historial = HistorialAsignacion.builder()
                .alerta(alerta)
                .usuarioDestino(analista)
                .motivo("Asignacion automatica por motor")
                .tipo("ASIGNACION")
                .build();
        historialRepository.save(historial);

        actualizarCarga(analista.getId());
        auditoriaService.registrar(analista.getId(), "AUTO_ASIGNACION",
                "Alerta " + alertaId + " auto-asignada a " + analista.getEmail(),
                request != null ? request.getRemoteAddr() : "system", "alertas", alertaId);

        log.info("[ASSIGNMENT] Alerta ID: {} asignada a {}", alertaId, analista.getEmail());
        return alerta;
    }

    public void rebalancearAnalista(Long usuarioId) {
        log.info("[ASSIGNMENT] Rebalanceando alertas del usuario ID: {}", usuarioId);
        List<Alerta> alertas = alertaRepository.findByEstado("ASIGNADA").stream()
                .filter(a -> a.getAsignadoA() != null && a.getAsignadoA().getId().equals(usuarioId))
                .toList();

        for (Alerta alerta : alertas) {
            try {
                Usuario nuevoAnalista = asignar(alerta);
                if (!nuevoAnalista.getId().equals(usuarioId)) {
                    Usuario anterior = alerta.getAsignadoA();
                    alerta.setAsignadoA(nuevoAnalista);
                    alerta.setFechaAsignacion(LocalDateTime.now());
                    alertaRepository.save(alerta);

                    HistorialAsignacion historial = HistorialAsignacion.builder()
                            .alerta(alerta)
                            .usuarioOrigen(anterior)
                            .usuarioDestino(nuevoAnalista)
                            .motivo("Rebalanceo automatico por cambio de disponibilidad")
                            .tipo("REBALANCEO")
                            .build();
                    historialRepository.save(historial);

                    actualizarCarga(nuevoAnalista.getId());
                    log.info("[ASSIGNMENT] Alerta ID: {} rebalanceada a {}", alerta.getId(), nuevoAnalista.getEmail());
                }
            } catch (Exception e) {
                log.error("[ASSIGNMENT] Error rebalanceando alerta ID: {} - {}", alerta.getId(), e.getMessage());
            }
        }
        actualizarCarga(usuarioId);
    }

    public void rebalancearTodos() {
        log.info("[ASSIGNMENT] Rebalanceo global iniciado");
        List<Alerta> pendientes = alertaRepository.findByEstado("PENDIENTE").stream()
                .filter(a -> a.getAsignadoA() == null)
                .toList();
        for (Alerta alerta : pendientes) {
            try {
                asignarAutomaticamente(alerta.getId(), null);
            } catch (Exception e) {
                log.error("[ASSIGNMENT] Error auto-asignando alerta ID: {} - {}", alerta.getId(), e.getMessage());
            }
        }
    }

    private void actualizarCarga(Long usuarioId) {
        LocalDate hoy = LocalDate.now();
        EstadisticaCargaAnalista stats = cargaRepository.findByUsuarioIdAndFecha(usuarioId, hoy)
                .orElse(EstadisticaCargaAnalista.builder()
                        .usuario(usuarioRepository.findById(usuarioId).orElse(null))
                        .fecha(hoy)
                        .build());

        long asignadas = alertaRepository.countByAsignadoAIdAndEstadoIn(
                usuarioId, List.of("PENDIENTE", "ASIGNADA", "INVESTIGANDO"));
        long resueltas = alertaRepository.countByAsignadoAIdAndEstadoIn(
                usuarioId, List.of("RESUELTA", "DESCARTADA"));

        stats.setAlertasPendientes((int) asignadas);
        stats.setAlertasAsignadas(stats.getAlertasAsignadas() + 1);
        stats.setAlertasResueltas((int) resueltas);
        stats.setUltimaActualizacion(LocalDateTime.now());
        cargaRepository.save(stats);
    }
}
