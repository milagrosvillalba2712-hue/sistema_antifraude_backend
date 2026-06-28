package com.antifraude.alerts;

import com.antifraude.audit.AuditoriaService;
import com.antifraude.exception.BusinessException;
import com.antifraude.exception.ResourceNotFoundException;
import com.antifraude.profile.DisponibilidadService;
import com.antifraude.profile.PerfilService;
import com.antifraude.rules.ReglaRiesgo;
import com.antifraude.transactions.Transaccion;
import com.antifraude.users.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AlertaService {

    private static final Logger log = LoggerFactory.getLogger(AlertaService.class);

    private final AlertaRepository alertaRepository;
    private final HistorialAsignacionRepository historialRepository;
    private final AuditoriaService auditoriaService;
    private final DisponibilidadService disponibilidadService;

    public AlertaService(AlertaRepository alertaRepository,
                          HistorialAsignacionRepository historialRepository,
                          AuditoriaService auditoriaService,
                          DisponibilidadService disponibilidadService) {
        this.alertaRepository = alertaRepository;
        this.historialRepository = historialRepository;
        this.auditoriaService = auditoriaService;
        this.disponibilidadService = disponibilidadService;
    }

    public Alerta crearAlerta(Transaccion transaccion, ReglaRiesgo regla, String prioridad) {
        log.info("[ALERTS] Creando alerta - Transaccion ID: {} - Regla: {} - Prioridad: {}",
                transaccion.getId(), regla.getNombre(), prioridad);
        Alerta alerta = Alerta.builder()
                .transaccion(transaccion)
                .regla(regla)
                .prioridad(prioridad)
                .estado("PENDIENTE")
                .build();
        Alerta creada = alertaRepository.save(alerta);
        log.info("[ALERTS] Alerta creada - ID: {} - Prioridad: {}", creada.getId(), prioridad);
        return creada;
    }

    public List<Alerta> listarTodas() {
        log.debug("[ALERTS] Listando todas las alertas");
        List<Alerta> alertas = alertaRepository.findAll();
        log.debug("[ALERTS] Total alertas: {}", alertas.size());
        return alertas;
    }

    public Alerta buscarPorId(Long id) {
        log.debug("[ALERTS] Buscando alerta por ID: {}", id);
        return alertaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[ALERTS] Alerta no encontrada con ID: {}", id);
                    return new ResourceNotFoundException("Alerta", "id", id);
                });
    }

    public List<Alerta> buscarPorEstado(String estado) {
        log.debug("[ALERTS] Buscando alertas por estado: {}", estado);
        return alertaRepository.findByEstado(estado);
    }

    public Alerta asignarAlerta(Long alertaId, Usuario analista, HttpServletRequest request) {
        log.info("[ALERTS] Asignando alerta ID: {} a analista: {} - IP: {}",
                alertaId, analista.getEmail(), request.getRemoteAddr());
        Alerta alerta = buscarPorId(alertaId);
        Usuario anterior = alerta.getAsignadoA();
        alerta.setAsignadoA(analista);
        alerta.setEstado("ASIGNADA");
        alerta.setFechaAsignacion(LocalDateTime.now());

        HistorialAsignacion historial = HistorialAsignacion.builder()
                .alerta(alerta)
                .usuarioOrigen(anterior)
                .usuarioDestino(analista)
                .motivo(anterior != null ? "Reasignacion" : "Asignacion inicial")
                .tipo(anterior != null ? "REASIGNACION" : "ASIGNACION")
                .build();
        historialRepository.save(historial);

        auditoriaService.registrar(analista.getId(), "ASIGNAR_ALERTA",
                "Alerta " + alertaId + " asignada a " + analista.getEmail(),
                request.getRemoteAddr(), "alertas", alertaId);
        Alerta actualizada = alertaRepository.save(alerta);
        log.info("[ALERTS] Alerta ID: {} asignada exitosamente a {}", alertaId, analista.getEmail());
        return actualizada;
    }

    public Alerta reasignarAlerta(Long alertaId, Long nuevoAnalistaId, String motivo,
                                    Usuario origen, HttpServletRequest request) {
        log.info("[ALERTS] Reasignando alerta ID: {} de {} a nuevo analista ID: {}",
                alertaId, origen.getEmail(), nuevoAnalistaId);
        Alerta alerta = buscarPorId(alertaId);
        if (alerta.getAsignadoA() == null || !alerta.getAsignadoA().getId().equals(origen.getId())) {
            throw new BusinessException("UNAUTHORIZED", "Solo el analista asignado puede reasignar esta alerta");
        }

        Usuario nuevoAnalista = null;
        if (nuevoAnalistaId != null) {
            nuevoAnalista = new Usuario();
            nuevoAnalista.setId(nuevoAnalistaId);
        }

        HistorialAsignacion historial = HistorialAsignacion.builder()
                .alerta(alerta)
                .usuarioOrigen(origen)
                .usuarioDestino(nuevoAnalista != null ? nuevoAnalista : origen)
                .motivo(motivo)
                .tipo("REASIGNACION")
                .build();
        historialRepository.save(historial);

        if (nuevoAnalista != null) {
            alerta.setAsignadoA(nuevoAnalista);
        }
        alerta.setFechaAsignacion(LocalDateTime.now());

        auditoriaService.registrar(origen.getId(), "REASIGNAR_ALERTA",
                "Alerta " + alertaId + " reasignada: " + motivo,
                request.getRemoteAddr(), "alertas", alertaId);
        return alertaRepository.save(alerta);
    }

    public Alerta autoAsignarAlerta(Long alertaId, HttpServletRequest request) {
        log.info("[ALERTS] Auto-asignando alerta ID: {}", alertaId);
        Alerta alerta = buscarPorId(alertaId);
        if (alerta.getAsignadoA() != null) {
            throw new BusinessException("ALREADY_ASSIGNED", "La alerta ya tiene un analista asignado");
        }

        List<Usuario> analistas = List.of();
        if (!analistas.isEmpty()) {
            Usuario asignado = analistas.get(0);
            alerta.setAsignadoA(asignado);
            alerta.setEstado("ASIGNADA");
            alerta.setFechaAsignacion(LocalDateTime.now());

            HistorialAsignacion historial = HistorialAsignacion.builder()
                    .alerta(alerta)
                    .usuarioDestino(asignado)
                    .motivo("Asignacion automatica por motor")
                    .tipo("ASIGNACION")
                    .build();
            historialRepository.save(historial);

            auditoriaService.registrar(asignado.getId(), "AUTO_ASIGNAR_ALERTA",
                    "Alerta " + alertaId + " auto-asignada a " + asignado.getEmail(),
                    request.getRemoteAddr(), "alertas", alertaId);
        }
        return alertaRepository.save(alerta);
    }

    public Alerta resolverAlerta(Long alertaId, String observacion, HttpServletRequest request) {
        log.info("[ALERTS] Resolviendo alerta ID: {} - Observacion: {} - IP: {}",
                alertaId, observacion, request.getRemoteAddr());
        Alerta alerta = buscarPorId(alertaId);
        alerta.setEstado("RESUELTA");
        alerta.setObservacion(observacion);
        alerta.setFechaResolucion(LocalDateTime.now());
        if (alerta.getAsignadoA() != null) {
            auditoriaService.registrar(alerta.getAsignadoA().getId(), "RESOLVER_ALERTA",
                    "Alerta " + alertaId + " resuelta: " + observacion,
                    request.getRemoteAddr(), "alertas", alertaId);
        }
        Alerta resuelta = alertaRepository.save(alerta);
        log.info("[ALERTS] Alerta ID: {} resuelta exitosamente", alertaId);
        return resuelta;
    }

    public long contarPorEstado(String estado) {
        return alertaRepository.countByEstado(estado);
    }

    @Transactional(readOnly = true)
    public List<HistorialAsignacion> obtenerHistorial(Long alertaId) {
        buscarPorId(alertaId);
        return historialRepository.findByAlertaIdOrderByFechaDesc(alertaId);
    }

    @Transactional(readOnly = true)
    public List<TimelineEvent> obtenerTimeline(Long alertaId) {
        Alerta alerta = buscarPorId(alertaId);
        List<TimelineEvent> eventos = new java.util.ArrayList<>();

        eventos.add(new TimelineEvent("CREACION", "Alerta generada",
                alerta.getFechaGeneracion(), null));

        List<HistorialAsignacion> historial = historialRepository.findByAlertaIdOrderByFechaDesc(alertaId);
        for (HistorialAsignacion h : historial) {
            String desc = h.getTipo().equals("ASIGNACION")
                    ? "Asignada a " + (h.getUsuarioDestino() != null ? h.getUsuarioDestino().getNombre() : "N/A")
                    : "Reasignada: " + (h.getMotivo() != null ? h.getMotivo() : "Sin motivo");
            eventos.add(new TimelineEvent(h.getTipo(), desc, h.getFecha(),
                    h.getUsuarioOrigen() != null ? h.getUsuarioOrigen().getNombre() : null));
        }

        if (alerta.getFechaResolucion() != null) {
            eventos.add(new TimelineEvent("RESOLUCION", "Alerta resuelta: " + alerta.getObservacion(),
                    alerta.getFechaResolucion(),
                    alerta.getAsignadoA() != null ? alerta.getAsignadoA().getNombre() : null));
        }

        eventos.sort((a, b) -> b.fecha().compareTo(a.fecha()));
        return eventos;
    }

    public record TimelineEvent(String tipo, String descripcion, LocalDateTime fecha, String usuario) {}
}
