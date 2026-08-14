package com.antifraude.alerts;

import com.antifraude.audit.Auditoria;
import com.antifraude.audit.AuditoriaRepository;
import com.antifraude.audit.AuditoriaService;
import com.antifraude.common.entity.Escenario;
import com.antifraude.common.repository.EscenarioRepository;
import com.antifraude.dto.*;
import com.antifraude.exception.BusinessException;
import com.antifraude.exception.ResourceNotFoundException;
import com.antifraude.external.ExternalInvestigationClient;
import com.antifraude.external.ExternalProviderException;
import com.antifraude.external.ProviderResult;
import com.antifraude.licensing.EnforcementService;
import com.antifraude.observability.ApiEventoService;
import com.antifraude.profile.DisponibilidadRepository;
import com.antifraude.profile.DisponibilidadUsuario;
import com.antifraude.rules.EjecucionRegla;
import com.antifraude.rules.EjecucionReglaRepository;
import com.antifraude.rules.ReglaRiesgo;
import com.antifraude.transactions.Transaccion;
import com.antifraude.transactions.TransaccionRepository;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
    private final AprobacionSupervisorRepository aprobacionSupervisorRepository;
    private final EvidenciaAlertaRepository evidenciaAlertaRepository;
    private final HallazgoAlertaRepository hallazgoAlertaRepository;
    private final ClienteSnapshotAlertaRepository clienteSnapshotAlertaRepository;
    private final TransaccionDetalleSnapshotRepository transaccionDetalleSnapshotRepository;
    private final EjecucionReglaRepository ejecucionReglaRepository;
    private final EscenarioRepository escenarioRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final ExternalInvestigationClient externalInvestigationClient;
    private final EnforcementService enforcementService;
    private final ApiEventoService apiEventoService;

    public AlertaService(AlertaRepository alertaRepository,
                          HistorialAsignacionRepository historialRepository,
                          AuditoriaService auditoriaService,
                          UsuarioRepository usuarioRepository,
                          DisponibilidadRepository disponibilidadRepository,
                          TransaccionRepository transaccionRepository,
                          ResolucionAlertaRepository resolucionAlertaRepository,
                          AprobacionSupervisorRepository aprobacionSupervisorRepository,
                          EvidenciaAlertaRepository evidenciaAlertaRepository,
                          HallazgoAlertaRepository hallazgoAlertaRepository,
                          ClienteSnapshotAlertaRepository clienteSnapshotAlertaRepository,
                          TransaccionDetalleSnapshotRepository transaccionDetalleSnapshotRepository,
                          EjecucionReglaRepository ejecucionReglaRepository,
                          EscenarioRepository escenarioRepository,
                          AuditoriaRepository auditoriaRepository,
                          ExternalInvestigationClient externalInvestigationClient,
                          EnforcementService enforcementService,
                          ApiEventoService apiEventoService) {
        this.alertaRepository = alertaRepository;
        this.historialRepository = historialRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.transaccionRepository = transaccionRepository;
        this.resolucionAlertaRepository = resolucionAlertaRepository;
        this.aprobacionSupervisorRepository = aprobacionSupervisorRepository;
        this.evidenciaAlertaRepository = evidenciaAlertaRepository;
        this.hallazgoAlertaRepository = hallazgoAlertaRepository;
        this.clienteSnapshotAlertaRepository = clienteSnapshotAlertaRepository;
        this.transaccionDetalleSnapshotRepository = transaccionDetalleSnapshotRepository;
        this.ejecucionReglaRepository = ejecucionReglaRepository;
        this.escenarioRepository = escenarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.externalInvestigationClient = externalInvestigationClient;
        this.enforcementService = enforcementService;
        this.apiEventoService = apiEventoService;
    }

    public Alerta crearAlerta(Transaccion transaccion, ReglaRiesgo regla, String prioridad) {
        log.info("[ALERTS] Creando alerta - Transaccion ID: {} - Regla: {} - Prioridad: {}",
                transaccion.getId(), regla != null ? regla.getNombre() : "Score de riesgo", prioridad);
        Alerta alerta = Alerta.builder()
                .transaccion(transaccion)
                .empresa(transaccion.getEmpresa())
                .codigo("ALT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .severidad(prioridad)
                .score(transaccion.getScoreRiesgo() != null ? transaccion.getScoreRiesgo() : java.math.BigDecimal.ZERO)
                .estado("NUEVA")
                .descripcion(regla != null
                        ? "Generada por regla: " + regla.getNombre()
                        : "Generada por score de riesgo alto sin regla guiada critica asociada")
                .reglasDisparadasJson(regla != null ? "[\"" + regla.getCodigo() + "\"]" : "[]")
                .build();
        Alerta creada = alertaRepository.save(alerta);
        log.info("[ALERTS] Alerta creada - ID: {} - Prioridad: {}", creada.getId(), prioridad);
        return creada;
    }

    public List<Alerta> listarTodas() {
        log.debug("[ALERTS] Listando todas las alertas");
        List<Alerta> alertas = alertaRepository.findAllByOrderByFechaGeneracionDesc();
        log.debug("[ALERTS] Total alertas: {}", alertas.size());
        return alertas;
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertaResponse> buscarPaginado(String search, String severidad, String estado,
                                                       Long escenarioId, UUID analistaId, String rangoFecha,
                                                       String desde, String hasta, String sort,
                                                       int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)), sortFor(sort));
        Specification<Alerta> spec = alertaSpecification(search, severidad, estado, escenarioId, analistaId,
                rangoFecha, desde, hasta);
        Page<AlertaResponse> result = alertaRepository.findAll(spec, pageable).map(this::toAlertaResponse);
        return new PageResponse<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
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

    @Transactional(readOnly = true)
    public AlertaFiltrosResponse obtenerFiltros() {
        List<FilterOptionResponse> escenarios = escenarioRepository.findAll().stream()
                .map(e -> new FilterOptionResponse(String.valueOf(e.getId()), e.getNombre()))
                .toList();
        List<FilterOptionResponse> analistas;
        try {
            analistas = listarAnalistasDisponibles().stream()
                    .map(a -> new FilterOptionResponse(String.valueOf(a.usuarioId()), a.nombre()))
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("[ALERTS] No se pudieron cargar analistas para filtros. Revise migracion UUID de usuarios: {}", ex.getMessage());
            analistas = List.of();
        }
        return new AlertaFiltrosResponse(
                List.of(option("CRITICA", "Crítica"), option("ALTA", "Alta"), option("MEDIA", "Media"), option("BAJA", "Baja")),
                List.of(option("NUEVA", "Nueva"), option("ASIGNADA", "Asignada"), option("EN_REVISION", "En Revisión"),
                        option("PENDIENTE_APROBACION", "Pendiente De Aprobación"), option("REEVALUACION", "Reevaluación"), option("CERRADA", "Cerrada")),
                escenarios,
                analistas,
                List.of(option("24h", "Últimas 24 Horas"), option("7d", "Últimos 7 Días"), option("30d", "Últimos 30 Días"), option("avanzado", "Avanzado")),
                List.of(option("recientes", "Más Recientes"), option("antiguas", "Más Antiguas"), option("score_desc", "Mayor Score"),
                        option("score_asc", "Menor Score"), option("severidad_desc", "Mayor Severidad")),
                List.of(10, 20, 50, 100));
    }

    public Alerta asignarAlerta(Long alertaId, Usuario analista, Usuario ejecutor, HttpServletRequest request) {
        log.info("[ALERTS] Asignando alerta ID: {} a analista: {} - IP: {}",
                alertaId, analista.getEmail(), request.getRemoteAddr());
        Alerta alerta = buscarPorId(alertaId);
        Usuario anterior = alerta.getAsignadoA();
        String valorAnterior = alertaAuditJson(alerta);
        alerta.setAsignadoA(analista);
        alerta.setEstado("ASIGNADA");
        alerta.setFechaAsignacion(OffsetDateTime.now());

        HistorialAsignacion historial = HistorialAsignacion.builder()
                .alerta(alerta)
                .usuarioOrigen(anterior)
                .usuarioDestino(analista)
                .motivo(anterior != null ? "Reasignacion" : "Asignacion inicial")
                .tipo(anterior != null ? "REASIGNACION" : "ASIGNACION")
                .build();
        historialRepository.save(historial);

        auditoriaService.registrar(ejecutor != null ? ejecutor.getId() : analista.getId(),
                alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                anterior != null ? "REASIGNAR_ALERTA" : "ASIGNAR_ALERTA",
                "Alerta " + alertaId + " asignada a " + analista.getEmail(),
                request.getRemoteAddr(), request.getHeader("User-Agent"), "alertas", alertaId,
                valorAnterior, alertaAuditJson(alerta));
        Alerta actualizada = alertaRepository.save(alerta);
        log.info("[ALERTS] Alerta ID: {} asignada exitosamente a {}", alertaId, analista.getEmail());
        return actualizada;
    }

    public Alerta reasignarAlerta(Long alertaId, UUID nuevoAnalistaId, String motivo, String observacion,
                                    Usuario origen, HttpServletRequest request) {
        log.info("[ALERTS] Reasignando alerta ID: {} de {} a nuevo analista ID: {}",
                alertaId, origen.getEmail(), nuevoAnalistaId);
        Alerta alerta = buscarPorId(alertaId);
        if ("CERRADA".equals(alerta.getEstado())) {
            throw new BusinessException("ALERTA_CERRADA", "No se puede reasignar una alerta cerrada");
        }
        if (alerta.getAsignadoA() == null || !alerta.getAsignadoA().getId().equals(origen.getId())) {
            throw new BusinessException("UNAUTHORIZED", "Solo el analista asignado puede reasignar esta alerta");
        }

        if (nuevoAnalistaId == null) {
            throw new BusinessException("ANALISTA_REQUERIDO", "Debe seleccionar el analista destino");
        }
        if (Objects.equals(nuevoAnalistaId, origen.getId())) {
            throw new BusinessException("MISMO_ANALISTA", "Seleccione un analista diferente al asignado actual");
        }
        Usuario nuevoAnalista = usuarioRepository.findById(nuevoAnalistaId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", nuevoAnalistaId));
        List<DisponibilidadUsuario> estados = disponibilidadRepository.findActivasAhora(nuevoAnalistaId, OffsetDateTime.now());
        String estadoDestino = estados.isEmpty() ? "DISPONIBLE" : estados.get(0).getTipoEstado();
        if (!List.of("DISPONIBLE", "CAPACITACION").contains(estadoDestino)) {
            throw new BusinessException("ANALISTA_NO_DISPONIBLE", "El analista destino no esta disponible");
        }
        String valorAnterior = alertaAuditJson(alerta);

        HistorialAsignacion historial = HistorialAsignacion.builder()
                .alerta(alerta)
                .usuarioOrigen(origen)
                .usuarioDestino(nuevoAnalista)
                .motivo(buildMotivo(motivo, observacion))
                .tipo("REASIGNACION")
                .build();
        historialRepository.save(historial);

        alerta.setAsignadoA(nuevoAnalista);
        alerta.setEstado("ASIGNADA");
        alerta.setFechaAsignacion(OffsetDateTime.now());

        auditoriaService.registrar(origen.getId(),
                alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                "REASIGNAR_ALERTA",
                "Alerta " + alertaId + " reasignada: " + buildMotivo(motivo, observacion),
                request.getRemoteAddr(), request.getHeader("User-Agent"), "alertas", alertaId,
                valorAnterior, alertaAuditJson(alerta));
        return alertaRepository.save(alerta);
    }

    public Alerta resolverAlerta(Long alertaId, String observacion, HttpServletRequest request) {
        log.info("[ALERTS] Resolviendo alerta ID: {} - Observacion: {} - IP: {}",
                alertaId, observacion, request.getRemoteAddr());
        Alerta alerta = buscarPorId(alertaId);
        String valorAnterior = alertaAuditJson(alerta);
        alerta.setEstado("CERRADA");
        alerta.setObservacion(observacion);
        alerta.setFechaResolucion(OffsetDateTime.now());
        if (alerta.getAsignadoA() != null) {
            auditoriaService.registrar(alerta.getAsignadoA().getId(),
                    alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                    "RESOLVER_ALERTA",
                    "Alerta " + alertaId + " resuelta: " + observacion,
                    request.getRemoteAddr(), request.getHeader("User-Agent"), "alertas", alertaId,
                    valorAnterior, alertaAuditJson(alerta));
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
            List<DisponibilidadUsuario> estados = disponibilidadRepository.findActivasAhora(usuario.getId(), OffsetDateTime.now());
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
        ExternalInvestigationData externalData = cargarDatosExternos(alerta, tx);
        List<TransaccionAlertaResponse> historial = externalData.historial();
        if (tx != null && tx.getIdentificadorDocumento() != null) {
            List<TransaccionAlertaResponse> historialLocal = transaccionRepository.findUltimasPorDocumento(tx.getIdentificadorDocumento()).stream()
                    .limit(15)
                    .map(this::transaccionResponse)
                    .toList();
            if (!historialLocal.isEmpty()) {
                historial = historialLocal;
            }
        }
        List<TimelineEventResponse> eventos = obtenerTimeline(alertaId).stream()
                .map(e -> new TimelineEventResponse(null, e.tipo(), e.descripcion(), e.fecha(), e.usuario()))
                .toList();
        ResolucionAlertaResponse resolucion = resolucionAlertaRepository
                .findFirstByAlertaIdOrderByFechaResolucionDesc(alertaId)
                .map(this::toResolucionResponse)
                .orElse(null);
        AprobacionSupervisorResponse aprobacion = aprobacionSupervisorRepository
                .findFirstByAlertaIdOrderByFechaSolicitudDesc(alertaId)
                .map(this::toAprobacionResponse)
                .orElse(null);
        List<ReglaAlertaResponse> reglasDisparadas = reglasDisparadas(alerta);
        List<HallazgoAlertaResponse> hallazgos = hallazgos(alerta, reglasDisparadas);
        List<EvidenciaAlertaResponse> evidencias = listarEvidencias(alertaId);
        return new AlertaDetalleResponse(toAlertaResponse(alerta), transaccionResponse(tx), reglaResponse(alerta.getRegla()),
                reglasDisparadas, hallazgos, clienteResponse(tx, alerta, externalData), historial, externalData.servicios(), eventos,
                accionesTimeline(alertaId), evidencias, resolucion, aprobacion, accionesDisponibles(alerta));
    }

    @Transactional(readOnly = true)
    public List<ReglaAlertaResponse> obtenerReglasDisparadas(Long alertaId) {
        return reglasDisparadas(buscarPorId(alertaId));
    }

    @Transactional(readOnly = true)
    public List<EvidenciaAlertaResponse> listarEvidencias(Long alertaId) {
        buscarPorId(alertaId);
        return evidenciaAlertaRepository.findByAlertaIdOrderByFechaCargaDesc(alertaId).stream()
                .map(this::toEvidenciaResponse)
                .toList();
    }

    public EvidenciaAlerta crearEvidencia(Long alertaId, EvidenciaAlertaRequest body, Usuario usuario,
                                          HttpServletRequest request) {
        Alerta alerta = buscarPorId(alertaId);
        validarEvidencia(body);
        EvidenciaAlerta evidencia = EvidenciaAlerta.builder()
                .alerta(alerta)
                .nombre(blankToDefault(body.nombre(), "Evidencia"))
                .descripcion(body.descripcion())
                .tipo(blankToDefault(body.tipo(), "DOCUMENTO"))
                .extension(normalizeExtension(body.extension()))
                .mimeType(body.mimeType())
                .tamanoBytes(body.tamanoBytes())
                .referenciaArchivo(body.referenciaArchivo())
                .estado(blankToDefault(body.estado(), "CARGADA"))
                .cargadoPor(usuario)
                .build();
        EvidenciaAlerta guardada = evidenciaAlertaRepository.save(evidencia);
        auditoriaService.registrar(usuario != null ? usuario.getId() : null,
                alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                "CREAR_EVIDENCIA_ALERTA",
                "Evidencia cargada en alerta " + alertaId + ": " + guardada.getNombre(),
                request.getRemoteAddr(), request.getHeader("User-Agent"), "alertas", alertaId,
                "{}", evidenciaAuditJson(guardada));
        return guardada;
    }

    public EvidenciaAlerta actualizarEvidencia(Long alertaId, Long evidenciaId, EvidenciaAlertaRequest body,
                                               Usuario usuario, HttpServletRequest request) {
        Alerta alerta = buscarPorId(alertaId);
        EvidenciaAlerta evidencia = evidenciaAlertaRepository.findById(evidenciaId)
                .orElseThrow(() -> new ResourceNotFoundException("EvidenciaAlerta", "id", evidenciaId));
        if (!evidencia.getAlerta().getId().equals(alertaId)) {
            throw new BusinessException("EVIDENCIA_NO_PERTENECE_ALERTA", "La evidencia no pertenece a la alerta indicada");
        }
        validarEvidencia(body);
        String anterior = evidenciaAuditJson(evidencia);
        evidencia.setNombre(blankToDefault(body.nombre(), evidencia.getNombre()));
        evidencia.setDescripcion(body.descripcion());
        evidencia.setTipo(blankToDefault(body.tipo(), evidencia.getTipo()));
        evidencia.setExtension(normalizeExtension(body.extension()));
        evidencia.setMimeType(body.mimeType());
        evidencia.setTamanoBytes(body.tamanoBytes());
        evidencia.setReferenciaArchivo(body.referenciaArchivo());
        evidencia.setEstado(blankToDefault(body.estado(), evidencia.getEstado()));
        EvidenciaAlerta actualizada = evidenciaAlertaRepository.save(evidencia);
        auditoriaService.registrar(usuario != null ? usuario.getId() : null,
                alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                "EDITAR_EVIDENCIA_ALERTA",
                "Evidencia editada en alerta " + alertaId + ": " + actualizada.getNombre(),
                request.getRemoteAddr(), request.getHeader("User-Agent"), "alertas", alertaId,
                anterior, evidenciaAuditJson(actualizada));
        return actualizada;
    }

    public void eliminarEvidencia(Long alertaId, Long evidenciaId, Usuario usuario, HttpServletRequest request) {
        Alerta alerta = buscarPorId(alertaId);
        EvidenciaAlerta evidencia = evidenciaAlertaRepository.findById(evidenciaId)
                .orElseThrow(() -> new ResourceNotFoundException("EvidenciaAlerta", "id", evidenciaId));
        if (!evidencia.getAlerta().getId().equals(alertaId)) {
            throw new BusinessException("EVIDENCIA_NO_PERTENECE_ALERTA", "La evidencia no pertenece a la alerta indicada");
        }
        String anterior = evidenciaAuditJson(evidencia);
        evidenciaAlertaRepository.delete(evidencia);
        auditoriaService.registrar(usuario != null ? usuario.getId() : null,
                alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                "ELIMINAR_EVIDENCIA_ALERTA",
                "Evidencia eliminada de alerta " + alertaId + ": " + evidencia.getNombre(),
                request.getRemoteAddr(), request.getHeader("User-Agent"), "alertas", alertaId,
                anterior, "{}");
    }

    public ResolucionAlerta resolverFormalmente(Long alertaId, Usuario usuario, ResolucionAlertaRequest body,
                                                HttpServletRequest request) {
        Alerta alerta = buscarPorId(alertaId);
        if (alerta.getAsignadoA() == null) {
            throw new BusinessException("ALERTA_NO_ASIGNADA", "La alerta debe estar asignada antes de resolverla");
        }
        if (usuario == null || !alerta.getAsignadoA().getId().equals(usuario.getId())) {
            throw new BusinessException("ALERTA_ASIGNADA_A_OTRO_USUARIO",
                    "Solo el analista asignado puede resolver esta alerta");
        }
        String valorAnterior = alertaAuditJson(alerta);
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

        alerta.setEstado("PENDIENTE_APROBACION");
        alerta.setObservacion(body.conclusion());
        alerta.setFechaResolucion(null);
        alertaRepository.save(alerta);

        AprobacionSupervisor aprobacion = AprobacionSupervisor.builder()
                .alerta(alerta)
                .resolucionAlerta(guardada)
                .estado("PENDIENTE")
                .observacion("Resolucion propuesta por " + usuario.getNombre())
                .fechaSolicitud(OffsetDateTime.now())
                .build();
        aprobacionSupervisorRepository.save(aprobacion);

        auditoriaService.registrar(usuario != null ? usuario.getId() : null,
                alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                "PROPONER_RESOLUCION_ALERTA",
                "Propuesta de resolucion de alerta " + alertaId + ": " + resultado,
                request.getRemoteAddr(), request.getHeader("User-Agent"), "alertas", alertaId,
                valorAnterior, alertaAuditJson(alerta));
        return guardada;
    }

    public AprobacionSupervisor aprobarResolucion(Long alertaId, Usuario supervisor, String observacion,
                                                  HttpServletRequest request) {
        Alerta alerta = buscarPorId(alertaId);
        if (!"PENDIENTE_APROBACION".equals(alerta.getEstado())) {
            throw new BusinessException("ESTADO_INVALIDO", "La alerta no esta pendiente de aprobacion");
        }
        ResolucionAlerta resolucion = resolucionAlertaRepository.findFirstByAlertaIdOrderByFechaResolucionDesc(alertaId)
                .orElseThrow(() -> new BusinessException("RESOLUCION_REQUERIDA", "No existe resolucion propuesta"));
        String valorAnterior = alertaAuditJson(alerta);
        AprobacionSupervisor aprobacion = aprobacionSupervisorRepository.findFirstByAlertaIdOrderByFechaSolicitudDesc(alertaId)
                .orElseGet(() -> AprobacionSupervisor.builder().alerta(alerta).resolucionAlerta(resolucion).fechaSolicitud(OffsetDateTime.now()).build());
        aprobacion.setSupervisor(supervisor);
        aprobacion.setEstado("APROBADA");
        aprobacion.setObservacion(observacion);
        aprobacion.setFechaAprobacion(OffsetDateTime.now());
        aprobacionSupervisorRepository.save(aprobacion);

        alerta.setEstado("CERRADA");
        alerta.setFechaResolucion(OffsetDateTime.now());
        alerta.setObservacion(resolucion.getConclusion());
        alertaRepository.save(alerta);
        auditoriaService.registrar(supervisor != null ? supervisor.getId() : null,
                alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                "APROBAR_RESOLUCION_ALERTA",
                "Resolucion aprobada para alerta " + alertaId + ": " + safe(observacion),
                request.getRemoteAddr(), request.getHeader("User-Agent"), "alertas", alertaId,
                valorAnterior, alertaAuditJson(alerta));
        return aprobacion;
    }

    public AprobacionSupervisor rechazarResolucion(Long alertaId, Usuario supervisor, String motivo, String faltantes,
                                                   HttpServletRequest request) {
        Alerta alerta = buscarPorId(alertaId);
        if (!"PENDIENTE_APROBACION".equals(alerta.getEstado())) {
            throw new BusinessException("ESTADO_INVALIDO", "La alerta no esta pendiente de aprobacion");
        }
        ResolucionAlerta resolucion = resolucionAlertaRepository.findFirstByAlertaIdOrderByFechaResolucionDesc(alertaId)
                .orElseThrow(() -> new BusinessException("RESOLUCION_REQUERIDA", "No existe resolucion propuesta"));
        String valorAnterior = alertaAuditJson(alerta);
        AprobacionSupervisor aprobacion = aprobacionSupervisorRepository.findFirstByAlertaIdOrderByFechaSolicitudDesc(alertaId)
                .orElseGet(() -> AprobacionSupervisor.builder().alerta(alerta).resolucionAlerta(resolucion).fechaSolicitud(OffsetDateTime.now()).build());
        aprobacion.setSupervisor(supervisor);
        aprobacion.setEstado("RECHAZADA");
        aprobacion.setMotivoRechazo(motivo);
        aprobacion.setFaltantes(faltantes);
        aprobacion.setFechaAprobacion(OffsetDateTime.now());
        aprobacionSupervisorRepository.save(aprobacion);

        alerta.setEstado("REEVALUACION");
        alerta.setObservacion("Reevaluacion requerida: " + safe(motivo));
        alerta.setFechaResolucion(null);
        alertaRepository.save(alerta);
        auditoriaService.registrar(supervisor != null ? supervisor.getId() : null,
                alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                "RECHAZAR_RESOLUCION_ALERTA",
                "Resolucion rechazada para alerta " + alertaId + ": " + safe(motivo),
                request.getRemoteAddr(), request.getHeader("User-Agent"), "alertas", alertaId,
                valorAnterior, alertaAuditJson(alerta));
        return aprobacion;
    }

    public Alerta cerrarAlerta(Long alertaId) {
        log.info("[ALERTS] Cerrando alerta ID: {}", alertaId);
        Alerta alerta = buscarPorId(alertaId);
        String valorAnterior = alertaAuditJson(alerta);
        alerta.setEstado("CERRADA");
        alerta.setFechaResolucion(OffsetDateTime.now());
        Alerta cerrada = alertaRepository.save(alerta);
        auditoriaService.registrar(alerta.getAsignadoA() != null ? alerta.getAsignadoA().getId() : null,
                alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                "CERRAR_ALERTA", "Alerta " + alertaId + " cerrada manualmente",
                null, null, "alertas", alertaId, valorAnterior, alertaAuditJson(alerta));
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

    public record TimelineEvent(String tipo, String descripcion, OffsetDateTime fecha, String usuario) {}

    public AlertaResponse toAlertaResponse(Alerta a) {
        return new AlertaResponse(
                a.getId(),
                a.getCodigo(),
                a.getTransaccion() != null ? a.getTransaccion().getId() : null,
                null,
                null,
                null,
                "Sin escenario",
                a.getTransaccion() != null ? a.getTransaccion().getIdentificadorDocumento() : null,
                clienteNombre(a.getTransaccion()),
                a.getTransaccion() != null ? a.getTransaccion().getMonto() : null,
                a.getTransaccion() != null ? a.getTransaccion().getMoneda() : null,
                a.getTransaccion() != null ? a.getTransaccion().getCanal() : null,
                a.getTransaccion() != null ? a.getTransaccion().getPaisOrigen() : null,
                a.getTransaccion() != null ? a.getTransaccion().getFechaTransaccion() : null,
                a.getTransaccion() != null && a.getTransaccion().getNivelRiesgo() != null
                        ? a.getTransaccion().getNivelRiesgo().getCodigo() : null,
                a.getScore() != null ? a.getScore() : (a.getTransaccion() != null ? a.getTransaccion().getScoreRiesgo() : null),
                severityFor(a),
                a.getPrioridad(), a.getEstado(), a.getObservacion(),
                a.getAsignadoA() != null ? a.getAsignadoA().getId() : null,
                a.getAsignadoA() != null ? a.getAsignadoA().getNombre() : null,
                a.getFechaGeneracion(), a.getFechaResolucion());
    }

    public AprobacionSupervisorResponse toAprobacionResponse(AprobacionSupervisor a) {
        return new AprobacionSupervisorResponse(
                a.getId(),
                a.getAlerta() != null ? a.getAlerta().getId() : null,
                a.getResolucionAlerta() != null ? a.getResolucionAlerta().getId() : null,
                a.getSupervisor() != null ? a.getSupervisor().getId() : null,
                a.getSupervisor() != null ? a.getSupervisor().getNombre() : null,
                a.getEstado(),
                a.getObservacion(),
                a.getMotivoRechazo(),
                a.getFaltantes(),
                a.getFechaSolicitud(),
                a.getFechaAprobacion());
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

    private TransaccionAlertaResponse transaccionResponse(Transaccion tx) {
        if (tx == null) return null;
        Map<String, Object> remitente = mapOf(
                "Nombre Completo", tx.getPersonaRemitente() != null ? tx.getPersonaRemitente().getNombreCompleto() : "No informado",
                "Documento", tx.getIdentificadorDocumento(),
                "Cuenta Origen", tx.getCuentaOrigen(),
                "Teléfono", "Pendiente API externa",
                "País De Residencia", tx.getPaisOrigen());
        Map<String, Object> beneficiario = mapOf(
                "Nombre Completo", tx.getPersonaBeneficiario() != null ? tx.getPersonaBeneficiario().getNombreCompleto() : "No informado",
                "Cuenta Destino", tx.getCuentaDestino(),
                "Banco Destino", "Pendiente API externa",
                "SWIFT/BIC", tx.getPaisDestinoRef() != null ? "Pendiente API externa" : "No aplica");
        Map<String, Object> operacion = mapOf(
                "Monto Origen", tx.getMonto(),
                "Moneda", tx.getMoneda(),
                "Monto Destino", "Pendiente cálculo de tasa",
                "Comisión", "Pendiente API/catálogo",
                "Impuestos", "Pendiente API/catálogo",
                "Tipo Transacción", tx.getTipoTransaccion());
        Map<String, Object> control = mapOf(
                "Identificador Único", tx.getTransactionUuid() != null ? tx.getTransactionUuid().toString() : tx.getCodigo(),
                "Código", tx.getCodigo(),
                "Fecha Y Hora", tx.getFechaTransaccion(),
                "Estado", tx.getEstadoEvaluacion() != null ? tx.getEstadoEvaluacion().name() : tx.getEstado(),
                "IP Origen", tx.getIpOrigen());
        Map<String, Object> internacional = mapOf(
                "País Origen", tx.getPaisOrigen(),
                "País Destino", tx.getPaisDestinoRef() != null ? tx.getPaisDestinoRef().getNombre() : "No informado",
                "Requiere SWIFT", tx.getPaisDestinoRef() != null,
                "Referencia Internacional", "Pendiente API externa");
        return new TransaccionAlertaResponse(tx.getId(), tx.getCodigo(), tx.getTransactionUuid() != null ? tx.getTransactionUuid().toString() : null,
                tx.getIdentificadorDocumento(), tx.getCuentaOrigen(), tx.getCuentaDestino(), tx.getMonto(),
                tx.getMoneda(), tx.getCanal(), tx.getTipoTransaccion(), tx.getIpOrigen(), tx.getPaisOrigen(),
                tx.getFechaTransaccion(), tx.getScoreRiesgo(),
                tx.getNivelRiesgo() != null ? tx.getNivelRiesgo().getCodigo() : null,
                tx.getEstadoEvaluacion() != null ? tx.getEstadoEvaluacion().name() : null,
                remitente, beneficiario, operacion, control, internacional);
    }

    private ReglaAlertaResponse reglaResponse(ReglaRiesgo regla) {
        if (regla == null) return null;
        return new ReglaAlertaResponse(regla.getId(), regla.getCodigo(), regla.getNombre(), regla.getDescripcion(),
                regla.getSeveridad(), regla.getPrioridad(), regla.getEstado(), regla.getCondicion(),
                regla.getCondicionesJson(), regla.getAccionesJson(), regla.getScoreBase(),
                regla.getEscenario() != null ? regla.getEscenario().getId() : null,
                regla.getEscenario() != null ? regla.getEscenario().getNombre() : "Sin escenario");
    }

    private ClienteAlertaResponse clienteResponse(Transaccion tx, Alerta alerta, ExternalInvestigationData externalData) {
        if (tx == null) return null;
        String documento = tx.getIdentificadorDocumento();
        Map<String, Object> perfil = externalData.perfil();
        Map<String, Object> documentos = externalData.documentos();
        Map<String, Object> screening = externalData.screening();
        Map<String, Object> personalRaw = nestedMap(perfil, "personal", mapOf());
        Map<String, Object> laboralRaw = nestedMap(perfil, "laboral", mapOf());
        Map<String, Object> academicoRaw = nestedMap(perfil, "academico", mapOf());
        Map<String, Object> familiarRaw = nestedMap(perfil, "familiar", mapOf());
        Map<String, Object> judicialRaw = nestedMap(perfil, "judicialRegulatorio", mapOf());
        Map<String, Object> personal = mapOf(
                "Nombre Completo", firstNonBlank(personalRaw.get("nombreCompleto"), clienteNombre(tx), "No informado"),
                "Número De Documento", documento,
                "Documento Enmascarado", firstNonBlank(personalRaw.get("documentoEnmascarado"), "No informado"),
                "Tipo De Documento", firstNonBlank(personalRaw.get("tipoDocumento"), "No informado"),
                "Fecha De Nacimiento", firstNonBlank(personalRaw.get("fechaNacimiento"), "No informado"),
                "Fecha De Emisión Documento", firstNonBlank(personalRaw.get("fechaEmisionDocumento"), "No informado"),
                "Fecha De Expiración Documento", firstNonBlank(personalRaw.get("fechaExpiracionDocumento"), "No informado"),
                "País De Emisión", firstNonBlank(personalRaw.get("paisEmisionDocumento"), "No informado"),
                "País De Residencia", firstNonBlank(personalRaw.get("paisResidencia"), tx.getPaisOrigen(), "No informado"),
                "País De Nacionalidad", firstNonBlank(personalRaw.get("paisNacionalidad"), "No informado"),
                "Ciudad De Residencia", firstNonBlank(personalRaw.get("ciudadResidencia"), "No informado"),
                "Departamento Residencia", firstNonBlank(personalRaw.get("departamentoResidencia"), "No informado"),
                "Dirección Residencia", firstNonBlank(personalRaw.get("direccionResidencia"), "No informado"),
                "Teléfono", firstNonBlank(personalRaw.get("telefonoFijoEnmascarado"), "No informado"),
                "Celular", firstNonBlank(personalRaw.get("telefonoMovilEnmascarado"), "No informado"),
                "Email", firstNonBlank(personalRaw.get("email"), "No informado"),
                "Edad", firstNonBlank(personalRaw.get("edad"), "No informado"),
                "Foto Documento Frente", firstNonBlank(personalRaw.get("fotoDocumentoFrenteReferencia"), documentReference(documentos, "CI_PY", "frenteDisponible"), "No disponible"),
                "Foto Documento Dorso", firstNonBlank(personalRaw.get("fotoDocumentoDorsoReferencia"), documentReference(documentos, "CI_PY", "dorsoDisponible"), "No disponible"),
                "Foto Perfil Cliente", firstNonBlank(personalRaw.get("fotoPerfilReferencia"), "No disponible"));
        Map<String, Object> laboral = mapOf(
                "Lugar De Trabajo", firstNonBlank(laboralRaw.get("lugarTrabajo"), "No informado"),
                "Dirección Del Trabajo", firstNonBlank(laboralRaw.get("direccionTrabajo"), "No informado"),
                "Contacto Corporativo", firstNonBlank(laboralRaw.get("contactoCorporativo"), "No informado"),
                "Ocupación", firstNonBlank(laboralRaw.get("ocupacion"), "No informado"),
                "Rango", firstNonBlank(laboralRaw.get("rango"), "No informado"),
                "Antigüedad", firstNonBlank(laboralRaw.get("antiguedad"), "No informado"),
                "Aproximación Salarial", firstNonBlank(laboralRaw.get("ingresoMensualEstimado"), "No informado"));
        Map<String, Object> academico = mapOf(
                "Nivel De Estudios", firstNonBlank(academicoRaw.get("nivelEstudios"), academicoRaw.get("nivel"), "No informado"),
                "Títulos Obtenidos", firstNonBlank(academicoRaw.get("titulosObtenidos"), "No informado"),
                "Institución Educativa", firstNonBlank(academicoRaw.get("institucionEducativa"), academicoRaw.get("institucion"), "No informado"),
                "Años De Cursada Y Graduación", firstNonBlank(academicoRaw.get("periodoCursada"), "No informado"),
                "Calificaciones Y Expedientes", firstNonBlank(academicoRaw.get("calificacionesExpedientes"), "No informado"),
                "Logros Destacados", firstNonBlank(academicoRaw.get("logrosDestacados"), "No informado"),
                "Certificaciones Y Cursos", firstNonBlank(academicoRaw.get("certificacionesCursos"), academicoRaw.get("certificaciones"), "No informado"));
        Map<String, Object> familiar = mapOf(
                "Estado Civil", firstNonBlank(familiarRaw.get("estadoCivil"), "No informado"),
                "Parentescos Directos", firstNonBlank(familiarRaw.get("parentescosDirectos"), "No informado"),
                "Contacto Familiar De Emergencia", firstNonBlank(familiarRaw.get("contactoEmergencia"), "No informado"),
                "Dirección Contacto Emergencia", firstNonBlank(familiarRaw.get("direccionContactoEmergencia"), "No informado"));
        Map<String, Object> judicial = mapOf(
                "Antecedentes Penales", firstNonBlank(judicialRaw.get("antecedentesPenales"), judicialRaw.get("antecedentes"), "No informado"),
                "Procesos Judiciales Activos", firstNonBlank(judicialRaw.get("procesosJudicialesActivos"), "No informado"),
                "Órdenes Y Requerimientos", firstNonBlank(judicialRaw.get("ordenesRequerimientos"), "No informado"),
                "Historial De Litigios", firstNonBlank(judicialRaw.get("historialLitigios"), "No informado"),
                "PEP", firstNonBlank(judicialRaw.get("pep"), "No informado"),
                "Sancionado", firstNonBlank(judicialRaw.get("sancionado"), "No informado"),
                "Nivel De Riesgo", firstNonBlank(judicialRaw.get("nivelRiesgo"), perfil.get("nivelRiesgo"), "No informado"),
                "Hallazgos", firstNonBlank(judicialRaw.get("hallazgos"), screening.get("hallazgos"), "No informado"));
        String fuente = externalData.disponible()
                ? "MOCK_EXTERNO_REGULA"
                : clienteSnapshotAlertaRepository.findByAlertaId(alerta.getId())
                        .map(ClienteSnapshotAlerta::getFuente)
                        .orElse("API_EXTERNA_NO_DISPONIBLE");
        String pep = booleanLabel(judicialRaw.get("pep"));
        String sancionado = booleanLabel(judicialRaw.get("sancionado"));
        String listas = screening.get("resultado") != null ? String.valueOf(screening.get("resultado")) : "Pendiente de consulta KYC";
        return new ClienteAlertaResponse(tx.getIdentificadorDocumento(),
                tx.getPersonaRemitente() != null ? tx.getPersonaRemitente().getNombreCompleto() : null,
                tx.getPersonaBeneficiario() != null ? tx.getPersonaBeneficiario().getNombreCompleto() : null,
                pep, sancionado, listas, fuente,
                personal, laboral, academico, familiar, judicial);
    }

    private List<ServicioExternoAlertaResponse> serviciosExternos() {
        return List.of(new ServicioExternoAlertaResponse(
                "Consulta KYC externa",
                "API externa no disponible",
                "La vista queda preparada para integrar proveedores externos cuando estén disponibles"));
    }

    private ExternalInvestigationData cargarDatosExternos(Alerta alerta, Transaccion tx) {
        String lookupKey = externalLookupKey(alerta, tx);
        if (lookupKey == null) {
            return ExternalInvestigationData.unavailable("No hay identificador seguro para consultar el mock externo.");
        }
        try {
            UUID empresaId = alerta.getEmpresa().getId();
            boolean riesgoPaisHabilitado = enforcementService.moduloHabilitado(empresaId, "RIESGO_PAIS");
            boolean beneficiarioFinalHabilitado = enforcementService.moduloHabilitado(empresaId, "BENEFICIARIO_FINAL");
            Integer limiteHistorial = enforcementService.limiteHistorial(empresaId);
            int historial = limiteHistorial != null ? limiteHistorial : 15;

            ProviderResult<Map<String, Object>> perfil = externalInvestigationClient.consultarPerfil(lookupKey);
            ProviderResult<Map<String, Object>> documentos = externalInvestigationClient.consultarDocumentos(lookupKey);
            ProviderResult<Map<String, Object>> screening = externalInvestigationClient.consultarScreening(lookupKey);
            ProviderResult<Map<String, Object>> historialProvider = externalInvestigationClient.consultarHistorial(lookupKey, historial);
            ProviderResult<Map<String, Object>> estado = externalInvestigationClient.consultarEstadoProveedores();
            ProviderResult<Map<String, Object>> riesgoPais = riesgoPaisHabilitado
                    ? externalInvestigationClient.consultarRiesgoPais(codigoIso(tx))
                    : null;
            ProviderResult<Map<String, Object>> beneficiarioFinal = beneficiarioFinalHabilitado
                    ? externalInvestigationClient.consultarBeneficiarioFinal(lookupKey)
                    : null;
            registrarConsultaExterna(alerta, "KYC_PERFIL", perfil);
            registrarConsultaExterna(alerta, "KYC_DOCUMENTOS", documentos);
            registrarConsultaExterna(alerta, "SCREENING_LISTAS", screening);
            registrarConsultaExterna(alerta, "CORE_HISTORIAL", historialProvider);
            registrarConsultaExterna(alerta, "ESTADO_PROVEEDORES", estado);
            registrarConsultaExterna(alerta, "RIESGO_PAIS", riesgoPais);
            registrarConsultaExterna(alerta, "BENEFICIARIO_FINAL", beneficiarioFinal);

            List<ServicioExternoAlertaResponse> servicios = new ArrayList<>(List.of(
                    servicioOk("Perfil KYC", perfil),
                    servicioOk("Documentos Cliente", documentos),
                    servicioOk("Screening Listas", screening),
                    servicioOk("Historial Transaccional Externo", historialProvider),
                    servicioOk("Estado De Proveedores", estado)
            ));
            if (riesgoPais != null) {
                servicios.add(servicioOk("Riesgo Pais", riesgoPais));
            }
            if (beneficiarioFinal != null) {
                servicios.add(servicioOk("Beneficiario Final", beneficiarioFinal));
            }
            return new ExternalInvestigationData(true, perfil.body(), documentos.body(), screening.body(),
                    historialExterno(historialProvider.body()), servicios,
                    riesgoPais != null ? riesgoPais.body() : Map.of(),
                    beneficiarioFinal != null ? beneficiarioFinal.body() : Map.of());
        } catch (ExternalProviderException | IllegalStateException exception) {
            log.warn("[ALERTS] Mock externo no disponible para alerta {}: {}", alerta.getId(), exception.getMessage());
            if (exception instanceof ExternalProviderException external) {
                apiEventoService.registrarExterna(alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                        null, external.provider(), "DETALLE_ALERTA", external.statusHttp(), external.durationMs(),
                        external.correlationId(), external.category(), external.getMessage(), false, external.category(),
                        "alertas", alerta.getId() != null ? String.valueOf(alerta.getId()) : null);
            }
            return ExternalInvestigationData.unavailable("No se pudo consultar el mock externo: " + exception.getClass().getSimpleName());
        }
    }

    private void registrarConsultaExterna(Alerta alerta, String endpoint, ProviderResult<?> result) {
        if (result == null) {
            return;
        }
        apiEventoService.registrarExterna(alerta.getEmpresa() != null ? alerta.getEmpresa().getId() : null,
                null, result.provider(), endpoint, result.statusHttp(), result.durationMs(), result.correlationId(),
                null, "Consulta externa completada", true, null, "alertas",
                alerta.getId() != null ? String.valueOf(alerta.getId()) : null);
    }

    private String codigoIso(Transaccion tx) {
        if (tx != null && tx.getPaisOrigen() != null && !tx.getPaisOrigen().isBlank()) {
            return tx.getPaisOrigen();
        }
        return "PY";
    }

    private ServicioExternoAlertaResponse servicioOk(String servicio, ProviderResult<?> result) {
        return new ServicioExternoAlertaResponse(servicio, "DISPONIBLE_SIMULADO",
                "Consulta completada. Correlation ID: " + result.correlationId());
    }

    private String externalLookupKey(Alerta alerta, Transaccion tx) {
        if (tx != null && tx.getIdentificadorDocumento() != null && !tx.getIdentificadorDocumento().isBlank()) {
            return tx.getIdentificadorDocumento();
        }
        return switch ((int) (alerta.getId() == null ? 0 : alerta.getId() % 4)) {
            case 0 -> "400";
            case 1 -> "100";
            case 2 -> "200";
            default -> "300";
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> source, String key, Map<String, Object> fallback) {
        if (source == null) return fallback;
        Object value = source.get(key);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : fallback;
    }

    @SuppressWarnings("unchecked")
    private List<TransaccionAlertaResponse> historialExterno(Map<String, Object> response) {
        if (response == null || !(response.get("transacciones") instanceof List<?> rows)) return List.of();
        List<TransaccionAlertaResponse> result = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> raw)) continue;
            Map<String, Object> item = (Map<String, Object>) raw;
            result.add(new TransaccionAlertaResponse(null,
                    stringValue(item.get("codigo")),
                    null,
                    null,
                    null,
                    null,
                    bigDecimalValue(item.get("monto")),
                    stringValue(item.get("moneda")),
                    "EXTERNO",
                    stringValue(item.get("tipo")),
                    null,
                    null,
                    parseExternalDate(item.get("fecha")),
                    bigDecimalValue(item.get("scoreRiesgo")),
                    null,
                    stringValue(item.get("estado")),
                    mapOf("Fuente", "Mock externo Regula"),
                    mapOf(),
                    mapOf("Tipo", stringValue(item.get("tipo")), "Monto", item.get("monto"), "Moneda", item.get("moneda")),
                    mapOf("Código", stringValue(item.get("codigo")), "Fecha", stringValue(item.get("fecha"))),
                    mapOf()));
        }
        return result;
    }

    private OffsetDateTime parseExternalDate(Object value) {
        if (value == null) return null;
        try {
            return OffsetDateTime.parse(String.valueOf(value));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private java.math.BigDecimal bigDecimalValue(Object value) {
        if (value == null) return null;
        try {
            return new java.math.BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String booleanLabel(Object value) {
        if (value instanceof Boolean bool) return bool ? "Sí" : "No";
        return value == null ? "Pendiente de consulta KYC" : String.valueOf(value);
    }

    private Object firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value == null) continue;
            if (value instanceof String text && text.isBlank()) continue;
            return value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object documentReference(Map<String, Object> documents, String type, String availabilityKey) {
        if (documents == null || !(documents.get("documentos") instanceof List<?> rows)) return null;
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> raw)) continue;
            Map<String, Object> item = (Map<String, Object>) raw;
            if (!Objects.equals(type, item.get("tipo"))) continue;
            Object available = item.get(availabilityKey);
            if (Boolean.FALSE.equals(available)) return null;
            return item.get("referencia");
        }
        return null;
    }

    private List<ReglaAlertaResponse> reglasDisparadas(Alerta alerta) {
        List<ReglaAlertaResponse> reglas = new ArrayList<>();
        if (alerta.getTransaccion() != null) {
            reglas.addAll(ejecucionReglaRepository
                    .findByTransaccionIdAndResultadoEvaluacionOrderByFechaEjecucionDesc(alerta.getTransaccion().getId(), "CUMPLIO")
                    .stream()
                    .map(EjecucionRegla::getRegla)
                    .filter(Objects::nonNull)
                    .map(this::reglaResponse)
                    .toList());
        }
        if (alerta.getRegla() != null && reglas.stream().noneMatch(r -> Objects.equals(r.id(), alerta.getRegla().getId()))) {
            reglas.add(reglaResponse(alerta.getRegla()));
        }
        return reglas;
    }

    private List<HallazgoAlertaResponse> hallazgos(Alerta alerta, List<ReglaAlertaResponse> reglasDisparadas) {
        List<HallazgoAlertaResponse> persistidos = hallazgoAlertaRepository.findByAlertaIdOrderByFechaRegistroDesc(alerta.getId()).stream()
                .map(this::toHallazgoResponse)
                .toList();
        if (!persistidos.isEmpty()) return persistidos;

        List<HallazgoAlertaResponse> derivados = new ArrayList<>();
        for (ReglaAlertaResponse regla : reglasDisparadas) {
            derivados.add(new HallazgoAlertaResponse(null, tipoHallazgo(regla), regla.nombre(),
                    descripcionHallazgo(regla), regla.severidad(), regla.scoreBase(), "MOTOR_REGLAS",
                    regla.condicionesJson(), regla));
        }
        if (alerta.getTransaccion() != null && alerta.getTransaccion().getPaisDestinoRef() != null) {
            derivados.add(new HallazgoAlertaResponse(null, "PAIS_DESTINO", "País Destino A Revisar",
                    "La transacción posee país destino informado y debe contrastarse contra listas de riesgo.",
                    severityFor(alerta), alerta.getTransaccion().getScoreRiesgo(), "TRANSACCION", null, null));
        }
        return derivados;
    }

    private HallazgoAlertaResponse toHallazgoResponse(HallazgoAlerta h) {
        return new HallazgoAlertaResponse(h.getId(), h.getTipo(), h.getTitulo(), h.getDescripcion(),
                h.getSeveridad(), h.getScore(), h.getFuente(), h.getDetalleJson(), reglaResponse(h.getRegla()));
    }

    public EvidenciaAlertaResponse toEvidenciaResponse(EvidenciaAlerta e) {
        return new EvidenciaAlertaResponse(e.getId(), e.getNombre(), e.getDescripcion(), e.getTipo(), e.getExtension(),
                e.getMimeType(), e.getTamanoBytes(), e.getEstado(), e.getReferenciaArchivo(),
                e.getCargadoPor() != null ? e.getCargadoPor().getNombre() : null, e.getFechaCarga());
    }

    private List<TimelineEventResponse> accionesTimeline(Long alertaId) {
        List<TimelineEventResponse> acciones = new ArrayList<>(obtenerTimeline(alertaId).stream()
                .map(e -> new TimelineEventResponse(null, e.tipo(), e.descripcion(), e.fecha(), e.usuario()))
                .toList());
        acciones.addAll(auditoriaRepository.findByEntidadAfectadaAndEntidadIdOrderByFechaEventoDesc("alertas", String.valueOf(alertaId)).stream()
                .map(this::toTimelineEvent)
                .toList());
        acciones.sort((a, b) -> b.fecha().compareTo(a.fecha()));
        return acciones;
    }

    private TimelineEventResponse toTimelineEvent(Auditoria auditoria) {
        return new TimelineEventResponse(auditoria.getId(), auditoria.getAccion(), auditoria.getDescripcion(),
                auditoria.getFechaEvento(), auditoria.getUsuarioId() != null ? "Usuario #" + auditoria.getUsuarioId() : null);
    }

    private List<String> accionesDisponibles(Alerta alerta) {
        if ("CERRADA".equals(alerta.getEstado())) return List.of("VER_DETALLE");
        List<String> acciones = new ArrayList<>(List.of("VER_DETALLE", "REASIGNAR", "CARGAR_EVIDENCIA"));
        if ("PENDIENTE_APROBACION".equals(alerta.getEstado())) {
            acciones.add("APROBAR_RESOLUCION");
            acciones.add("RECHAZAR_RESOLUCION");
        } else {
            acciones.add("RESOLVER");
        }
        return acciones;
    }

    private void validarEvidencia(EvidenciaAlertaRequest body) {
        if (body == null) throw new BusinessException("EVIDENCIA_REQUERIDA", "Debe enviar datos de evidencia");
        String extension = normalizeExtension(body.extension());
        Set<String> permitidas = Set.of("PDF", "JPG", "JPEG", "PNG", "CSV", "XLSX", "DOCX", "TXT");
        if (extension != null && !extension.isBlank() && !permitidas.contains(extension)) {
            throw new BusinessException("EXTENSION_NO_PERMITIDA", "Extensión no permitida. Compatibles: PDF, JPG, JPEG, PNG, CSV, XLSX, DOCX, TXT");
        }
        if (body.tamanoBytes() != null && body.tamanoBytes() > 10L * 1024L * 1024L) {
            throw new BusinessException("EVIDENCIA_MUY_GRANDE", "La evidencia no puede superar 10 MB");
        }
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) return extension;
        return extension.replace(".", "").trim().toUpperCase();
    }

    private FilterOptionResponse option(String value, String label) {
        return new FilterOptionResponse(value, label);
    }

    private String tipoHallazgo(ReglaAlertaResponse regla) {
        String source = (safe(regla.codigo()) + " " + safe(regla.nombre()) + " " + safe(regla.descripcion())).toUpperCase();
        if (source.contains("PAIS")) return "PAIS_RIESGO";
        if (source.contains("LISTA")) return "LISTA_REGULATORIA";
        if (source.contains("PEP")) return "PEP";
        if (source.contains("MONTO") || source.contains("IMPORTE")) return "IMPORTE";
        if (source.contains("FRECUENCIA")) return "FRECUENCIA";
        if (source.contains("HORARIO")) return "HORARIO";
        return "REGLA_MANUAL";
    }

    private String descripcionHallazgo(ReglaAlertaResponse regla) {
        if (regla.descripcion() != null && !regla.descripcion().isBlank()) return regla.descripcion();
        return "La regla " + safe(regla.codigo()) + " se cumplió durante la evaluación de la transacción.";
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String evidenciaAuditJson(EvidenciaAlerta evidencia) {
        if (evidencia == null) return "{}";
        return "{"
                + "\"id\":" + evidencia.getId()
                + ",\"nombre\":\"" + safe(evidencia.getNombre()) + "\""
                + ",\"tipo\":\"" + safe(evidencia.getTipo()) + "\""
                + ",\"estado\":\"" + safe(evidencia.getEstado()) + "\""
                + "}";
    }

    private Specification<Alerta> alertaSpecification(String search, String severidad, String estado,
                                                      Long escenarioId, UUID analistaId, String rangoFecha,
                                                      String desde, String hasta) {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("transaccion", JoinType.LEFT);
                root.fetch("asignadoA", JoinType.LEFT);
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            var tx = root.join("transaccion", JoinType.LEFT);
            var asignado = root.join("asignadoA", JoinType.LEFT);
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("codigo")), like),
                        cb.like(cb.lower(root.get("descripcion")), like),
                        cb.like(cb.lower(asignado.get("nombre")), like)));
            }
            if (severidad != null && !severidad.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("severidad")), severidad.toUpperCase()));
            }
            if (estado != null && !estado.isBlank()) predicates.add(cb.equal(root.get("estado"), estado));
            if (analistaId != null) predicates.add(cb.equal(asignado.get("id"), analistaId));
            OffsetDateTime[] rango = resolveDateRange(rangoFecha, desde, hasta);
            if (rango[0] != null) predicates.add(cb.greaterThanOrEqualTo(root.get("fechaGeneracion"), rango[0]));
            if (rango[1] != null) predicates.add(cb.lessThanOrEqualTo(root.get("fechaGeneracion"), rango[1]));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort sortFor(String sort) {
        return switch (sort == null ? "recientes" : sort) {
            case "antiguas" -> Sort.by(Sort.Direction.ASC, "fechaGeneracion");
            case "score_desc" -> Sort.by(Sort.Direction.DESC, "score");
            case "score_asc" -> Sort.by(Sort.Direction.ASC, "score");
            case "severidad_desc" -> Sort.by(Sort.Direction.DESC, "severidad").and(Sort.by(Sort.Direction.DESC, "fechaGeneracion"));
            default -> Sort.by(Sort.Direction.DESC, "fechaGeneracion");
        };
    }

    private OffsetDateTime[] resolveDateRange(String rangoFecha, String desde, String hasta) {
        OffsetDateTime start = parseDate(desde);
        OffsetDateTime end = parseDate(hasta);
        OffsetDateTime now = OffsetDateTime.now();
        if ((start == null && end == null) && rangoFecha != null) {
            start = switch (rangoFecha) {
                case "24h" -> now.minusHours(24);
                case "7d" -> now.minusDays(7);
                case "30d" -> now.minusDays(30);
                default -> null;
            };
        }
        return new OffsetDateTime[]{start, end};
    }

    private OffsetDateTime parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String severityFor(Alerta alerta) {
        if (alerta.getSeveridad() != null) {
            return alerta.getSeveridad();
        }
        return switch (alerta.getPrioridad() == null ? "" : alerta.getPrioridad()) {
            case "CRITICA" -> "CRITICA";
            case "ALTA" -> "ALTA";
            case "MEDIA" -> "MEDIA";
            default -> "BAJA";
        };
    }

    private String buildMotivo(String motivo, String observacion) {
        String base = motivo == null || motivo.isBlank() ? "Sin motivo informado" : motivo.trim();
        if (observacion == null || observacion.isBlank()) return base;
        return base + " | Observacion: " + observacion.trim();
    }

    private String clienteNombre(Transaccion tx) {
        if (tx == null) return null;
        if (tx.getPersonaRemitente() != null) return tx.getPersonaRemitente().getNombreCompleto();
        if (tx.getPersonaBeneficiario() != null) return tx.getPersonaBeneficiario().getNombreCompleto();
        return tx.getIdentificadorDocumento();
    }

    private String alertaAuditJson(Alerta alerta) {
        if (alerta == null) return "{}";
        return "{"
                + "\"id\":" + alerta.getId()
                + ",\"codigo\":\"" + safe(alerta.getCodigo()) + "\""
                + ",\"estado\":\"" + safe(alerta.getEstado()) + "\""
                + ",\"prioridad\":\"" + safe(alerta.getPrioridad()) + "\""
                + ",\"asignadoA\":" + (alerta.getAsignadoA() != null ? alerta.getAsignadoA().getId() : null)
                + ",\"asignadoNombre\":\"" + safe(alerta.getAsignadoA() != null ? alerta.getAsignadoA().getNombre() : null) + "\""
                + "}";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record ExternalInvestigationData(
            boolean disponible,
            Map<String, Object> perfil,
            Map<String, Object> documentos,
            Map<String, Object> screening,
            List<TransaccionAlertaResponse> historial,
            List<ServicioExternoAlertaResponse> servicios,
            Map<String, Object> riesgoPais,
            Map<String, Object> beneficiarioFinal) {
        private static ExternalInvestigationData unavailable(String mensaje) {
            return new ExternalInvestigationData(false, Map.of(), Map.of(), Map.of(), List.of(),
                    List.of(new ServicioExternoAlertaResponse(
                            "Mock Externo Regula",
                            "API_EXTERNA_NO_DISPONIBLE",
                            mensaje)),
                    Map.of(), Map.of());
        }
    }
}
