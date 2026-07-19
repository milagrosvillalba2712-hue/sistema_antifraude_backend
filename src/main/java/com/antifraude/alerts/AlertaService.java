package com.antifraude.alerts;

import com.antifraude.audit.AuditoriaService;
import com.antifraude.dto.*;
import com.antifraude.exception.BusinessException;
import com.antifraude.exception.ResourceNotFoundException;
import com.antifraude.profile.DisponibilidadRepository;
import com.antifraude.profile.DisponibilidadUsuario;
import com.antifraude.rules.ReglaRiesgo;
import com.antifraude.transactions.Transaccion;
import com.antifraude.transactions.TransaccionRepository;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AlertaService {

    private static final Logger log = LoggerFactory.getLogger(AlertaService.class);

    private final AlertaRepository alertaRepository;
    private final HistorialAsignacionRepository historialRepository;
    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final TransaccionRepository transaccionRepository;
    private final ResolucionAlertaRepository resolucionAlertaRepository;

    public AlertaService(AlertaRepository alertaRepository,
                          HistorialAsignacionRepository historialRepository,
                          AuditoriaService auditoriaService,
                          UsuarioRepository usuarioRepository,
                          DisponibilidadRepository disponibilidadRepository,
                          TransaccionRepository transaccionRepository,
                          ResolucionAlertaRepository resolucionAlertaRepository) {
        this.alertaRepository = alertaRepository;
        this.historialRepository = historialRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.transaccionRepository = transaccionRepository;
        this.resolucionAlertaRepository = resolucionAlertaRepository;
    }

    public Alerta crearAlerta(Transaccion transaccion, ReglaRiesgo regla, String prioridad) {
        log.info("[ALERTS] Creando alerta - Transaccion ID: {} - Regla: {} - Prioridad: {}",
                transaccion.getId(), regla != null ? regla.getNombre() : "Score de riesgo", prioridad);
        Alerta alerta = Alerta.builder()
                .transaccion(transaccion)
                .empresa(transaccion.getEmpresa())
                .regla(regla)
                .codigo("ALT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .prioridad(prioridad)
                .estado("NUEVA")
                .observacion(regla != null
                        ? "Generada por regla: " + regla.getNombre()
                        : "Generada por score de riesgo alto sin regla guiada critica asociada")
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

    public long contarSinAsignar() {
        return alertaRepository.countByAsignadoAIsNullAndEstado("NUEVA");
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

    public Alerta resolverAlerta(Long alertaId, String observacion, HttpServletRequest request) {
        log.info("[ALERTS] Resolviendo alerta ID: {} - Observacion: {} - IP: {}",
                alertaId, observacion, request.getRemoteAddr());
        Alerta alerta = buscarPorId(alertaId);
        alerta.setEstado("CERRADA");
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
    public List<AnalistaDisponibleResponse> listarAnalistasDisponibles() {
        List<Usuario> analistas = usuarioRepository.findActivosByRolCodigo("ANALISTA");
        return analistas.stream().map(usuario -> {
            List<DisponibilidadUsuario> estados = disponibilidadRepository.findActivasAhora(usuario.getId(), LocalDateTime.now());
            String estado = estados.isEmpty() ? "DISPONIBLE" : estados.get(0).getTipoEstado();
            boolean disponible = List.of("DISPONIBLE", "CAPACITACION").contains(estado);
            long activas = alertaRepository.countByAsignadoAIdAndEstadoIn(usuario.getId(), List.of("ASIGNADA", "EN_REVISION"));
            return new AnalistaDisponibleResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), estado, activas, disponible);
        }).toList();
    }

    @Transactional(readOnly = true)
    public AlertaDetalleResponse obtenerDetalleFormal(Long alertaId) {
        Alerta alerta = buscarPorId(alertaId);
        Transaccion tx = alerta.getTransaccion();
        List<Map<String, Object>> historial = new ArrayList<>();
        if (tx != null && tx.getIdentificadorDocumento() != null) {
            historial = transaccionRepository.findUltimasPorDocumento(tx.getIdentificadorDocumento()).stream()
                    .limit(15)
                    .map(this::transaccionMap)
                    .toList();
        }
        List<TimelineEventResponse> eventos = obtenerTimeline(alertaId).stream()
                .map(e -> new TimelineEventResponse(null, e.tipo(), e.descripcion(), e.fecha(), e.usuario()))
                .toList();
        ResolucionAlertaResponse resolucion = resolucionAlertaRepository
                .findFirstByAlertaIdOrderByFechaResolucionDesc(alertaId)
                .map(this::toResolucionResponse)
                .orElse(null);
        return new AlertaDetalleResponse(toAlertaResponse(alerta), transaccionMap(tx), reglaMap(alerta.getRegla()),
                clienteMap(tx), historial, serviciosExternos(), eventos, resolucion);
    }

    public ResolucionAlerta resolverFormalmente(Long alertaId, Usuario usuario, ResolucionAlertaRequest body,
                                                HttpServletRequest request) {
        Alerta alerta = buscarPorId(alertaId);
        ResolucionAlerta.Resultado resultado = ResolucionAlerta.Resultado.valueOf(body.resultado());
        ResolucionAlerta resolucion = ResolucionAlerta.builder()
                .alerta(alerta)
                .usuario(usuario)
                .resultado(resultado)
                .conclusion(body.conclusion())
                .decision(body.decision())
                .justificacion(body.justificacion())
                .evidenciaDescripcion(body.evidenciaDescripcion())
                .contactoCliente(body.contactoCliente())
                .fondosRetenidos(Boolean.TRUE.equals(body.fondosRetenidos()))
                .movimientoLiberable(Boolean.TRUE.equals(body.movimientoLiberable()))
                .requiereRos(Boolean.TRUE.equals(body.requiereRos()) || resultado == ResolucionAlerta.Resultado.ROS_REQUERIDO)
                .requiereBloqueo(Boolean.TRUE.equals(body.requiereBloqueo()))
                .requiereEscalamientoLegal(Boolean.TRUE.equals(body.requiereEscalamientoLegal()))
                .build();
        ResolucionAlerta guardada = resolucionAlertaRepository.save(resolucion);

        alerta.setEstado("CERRADA");
        alerta.setObservacion(body.conclusion());
        alerta.setFechaResolucion(LocalDateTime.now());
        alertaRepository.save(alerta);

        auditoriaService.registrar(usuario != null ? usuario.getId() : null, "RESOLUCION_FORMAL_ALERTA",
                "Resolucion formal de alerta " + alertaId + ": " + resultado,
                request.getRemoteAddr(), "alertas", alertaId);
        return guardada;
    }

    public Alerta cerrarAlerta(Long alertaId) {
        log.info("[ALERTS] Cerrando alerta ID: {}", alertaId);
        Alerta alerta = buscarPorId(alertaId);
        alerta.setEstado("CERRADA");
        alerta.setFechaResolucion(LocalDateTime.now());
        Alerta cerrada = alertaRepository.save(alerta);
        log.info("[ALERTS] Alerta ID: {} cerrada exitosamente", alertaId);
        return cerrada;
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

    public AlertaResponse toAlertaResponse(Alerta a) {
        return new AlertaResponse(
                a.getId(),
                a.getCodigo(),
                a.getTransaccion() != null ? a.getTransaccion().getId() : null,
                a.getRegla() != null ? a.getRegla().getId() : null,
                a.getRegla() != null ? a.getRegla().getNombre() : null,
                a.getTransaccion() != null ? a.getTransaccion().getScoreRiesgo() : null,
                a.getPrioridad(), a.getEstado(), a.getObservacion(),
                a.getAsignadoA() != null ? a.getAsignadoA().getId() : null,
                a.getAsignadoA() != null ? a.getAsignadoA().getNombre() : null,
                a.getFechaGeneracion(), a.getFechaResolucion());
    }

    public ResolucionAlertaResponse toResolucionResponse(ResolucionAlerta r) {
        return new ResolucionAlertaResponse(r.getId(), r.getAlerta().getId(),
                r.getUsuario() != null ? r.getUsuario().getId() : null,
                r.getUsuario() != null ? r.getUsuario().getNombre() : null,
                r.getResultado().name(), r.getConclusion(), r.getDecision(), r.getJustificacion(),
                r.getEvidenciaDescripcion(), r.getContactoCliente(), r.getFondosRetenidos(),
                r.getMovimientoLiberable(), r.getRequiereRos(), r.getRequiereBloqueo(),
                r.getRequiereEscalamientoLegal(), r.getFechaResolucion());
    }

    private Map<String, Object> transaccionMap(Transaccion tx) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (tx == null) return map;
        map.put("id", tx.getId());
        map.put("codigo", tx.getCodigo());
        map.put("transactionUuid", tx.getTransactionUuid());
        map.put("identificadorDocumento", tx.getIdentificadorDocumento());
        map.put("cuentaOrigen", tx.getCuentaOrigen());
        map.put("cuentaDestino", tx.getCuentaDestino());
        map.put("monto", tx.getMonto());
        map.put("moneda", tx.getMoneda());
        map.put("canal", tx.getCanal());
        map.put("tipoTransaccion", tx.getTipoTransaccion());
        map.put("ipOrigen", tx.getIpOrigen());
        map.put("paisOrigen", tx.getPaisOrigen());
        map.put("fechaTransaccion", tx.getFechaTransaccion());
        map.put("scoreRiesgo", tx.getScoreRiesgo());
        map.put("nivelRiesgo", tx.getNivelRiesgo() != null ? tx.getNivelRiesgo().getCodigo() : null);
        map.put("estadoEvaluacion", tx.getEstadoEvaluacion());
        return map;
    }

    private Map<String, Object> reglaMap(ReglaRiesgo regla) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (regla == null) return map;
        map.put("id", regla.getId());
        map.put("codigo", regla.getCodigo());
        map.put("nombre", regla.getNombre());
        map.put("severidad", regla.getSeveridad());
        map.put("estado", regla.getEstado());
        map.put("condicion", regla.getCondicion());
        map.put("condicionesJson", regla.getCondicionesJson());
        map.put("accionesJson", regla.getAccionesJson());
        return map;
    }

    private Map<String, Object> clienteMap(Transaccion tx) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (tx == null) return map;
        map.put("documento", tx.getIdentificadorDocumento());
        map.put("personaRemitente", tx.getPersonaRemitente() != null ? tx.getPersonaRemitente().getNombreCompleto() : null);
        map.put("personaBeneficiario", tx.getPersonaBeneficiario() != null ? tx.getPersonaBeneficiario().getNombreCompleto() : null);
        map.put("pep", "Pendiente de consulta KYC");
        map.put("observado", "Pendiente de consulta KYC");
        map.put("listas", "Pendiente de consulta KYC");
        return map;
    }

    private List<Map<String, Object>> serviciosExternos() {
        return List.of(Map.of(
                "servicio", "Consulta KYC externa",
                "estado", "API externa no disponible",
                "mensaje", "La vista queda preparada para integrar proveedores externos cuando esten disponibles"));
    }
}
