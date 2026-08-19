package com.antifraude.licensing;

import com.antifraude.audit.Auditoria;
import com.antifraude.audit.AuditoriaRepository;
import com.antifraude.audit.AuditoriaService;
import com.antifraude.config.ClientIpResolver;
import com.antifraude.dto.ApiErrorDescriptor;
import com.antifraude.exception.BusinessException;
import com.antifraude.security.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin-empresa")
@Transactional(readOnly = true)
public class AdminEmpresaController {

    private final EmpresaRepository empresaRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final PagoRepository pagoRepository;
    private final UsoSuscripcionRepository usoSuscripcionRepository;
    private final InstalacionLocalRepository instalacionRepository;
    private final LicenciaLocalRepository licenciaRepository;
    private final EventoLicenciaLocalRepository eventoRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final LicensingControlPlaneClient controlPlaneClient;
    private final LicensingOnlineService onlineService;
    private final AuditoriaService auditoriaService;
    private final AdminEmpresaObservabilityService observabilityService;

    @Value("${app.licenses.jobs.enabled:true}")
    private boolean jobsHabilitados;

    public AdminEmpresaController(EmpresaRepository empresaRepository,
                                  SuscripcionRepository suscripcionRepository,
                                  PagoRepository pagoRepository,
                                  UsoSuscripcionRepository usoSuscripcionRepository,
                                  InstalacionLocalRepository instalacionRepository,
                                  LicenciaLocalRepository licenciaRepository,
                                  EventoLicenciaLocalRepository eventoRepository,
                                  UsuarioEmpresaRepository usuarioEmpresaRepository,
                                  AuditoriaRepository auditoriaRepository,
                                  LicensingControlPlaneClient controlPlaneClient,
                                  LicensingOnlineService onlineService,
                                  AuditoriaService auditoriaService,
                                  AdminEmpresaObservabilityService observabilityService) {
        this.empresaRepository = empresaRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.usoSuscripcionRepository = usoSuscripcionRepository;
        this.instalacionRepository = instalacionRepository;
        this.licenciaRepository = licenciaRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.controlPlaneClient = controlPlaneClient;
        this.onlineService = onlineService;
        this.auditoriaService = auditoriaService;
        this.observabilityService = observabilityService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> resumen() {
        UUID empresaId = empresaActual();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("EMPRESA_NO_ENCONTRADA", "Empresa no encontrada"));
        Suscripcion suscripcion = suscripcionActiva(empresaId);
        PlanLicencia plan = suscripcion != null ? suscripcion.getPlanLicencia() : null;
        InstalacionLocal instalacion = instalacionRepository.findTopByEmpresaIdOrderByActivadaEnDesc(empresaId)
                .orElse(null);
        LicenciaLocal licencia = licenciaVigente(instalacion);
        UsoSuscripcion uso = usoSuscripcionRepository
                .findFirstByEmpresaIdAndAnioAndMesOrderByIdDesc(empresaId, LocalDate.now().getYear(), LocalDate.now().getMonthValue())
                .orElse(null);
        List<Map<String, Object>> consultas = observabilityService.ultimasConsultasExternas(empresaId, 50);

        return ResponseEntity.ok(mapOf(
                "empresa", empresaDto(empresa),
                "suscripcion", suscripcionDto(suscripcion),
                "plan", planDto(plan),
                "licencia", licenciaDto(licencia),
                "instalacion", instalacionDto(instalacion),
                "usoActual", usoDto(uso),
                "apis", apiResumenDto(consultas),
                "controlPlane", conectividadDto(instalacion),
                "usuariosActivos", usuarioEmpresaRepository.findByEmpresaIdAndActivoTrueOrderByUsuarioNombreAsc(empresaId).size()
        ));
    }

    @GetMapping("/licencia")
    public ResponseEntity<Map<String, Object>> licencia() {
        UUID empresaId = empresaActual();
        InstalacionLocal instalacion = instalacionRepository.findTopByEmpresaIdOrderByActivadaEnDesc(empresaId)
                .orElse(null);
        LicenciaLocal licencia = licenciaVigente(instalacion);
        return ResponseEntity.ok(mapOf(
                "instalacion", instalacionDto(instalacion),
                "licencia", licenciaDto(licencia),
                "controlPlane", conectividadDto(instalacion),
                "eventos", eventos(instalacion)
        ));
    }

    @PostMapping("/licencia/validar")
    @Transactional
    public ResponseEntity<Map<String, Object>> validarLicencia(HttpServletRequest request) {
        UUID empresaId = empresaActual();
        InstalacionLocal instalacion = instalacionRepository.findTopByEmpresaIdOrderByActivadaEnDesc(empresaId)
                .orElseThrow(() -> new BusinessException("INSTALACION_NO_ENCONTRADA",
                        "No existe una instalacion local para validar"));
        LicensingOnlineService.ResultadoValidacionOnline resultado = onlineService.validarYRenovar(instalacion);
        auditoriaService.registrar(TenantContext.getUsuarioId(), empresaId, "VALIDAR_LICENCIA_ADMIN_EMPRESA",
                "Validacion manual de licencia desde Admin Empresa", ClientIpResolver.resolve(request),
                request.getHeader("User-Agent"), "instalacion_local", instalacion.getId(), null, null);
        return ResponseEntity.ok(mapOf(
                "instalacionId", resultado.instalacionId(),
                "licenciaId", resultado.licenciaId(),
                "modo", resultado.modo().name(),
                "motivo", resultado.motivo(),
                "detalle", resultado.detalle(),
                "firmaValida", resultado.firmaValida(),
                "online", resultado.online(),
                "controlPlane", resultado.controlPlane(),
                "venceEn", resultado.venceEn()
        ));
    }

    @GetMapping("/consumo")
    public ResponseEntity<Map<String, Object>> consumo() {
        UUID empresaId = empresaActual();
        Suscripcion suscripcionActiva = suscripcionActiva(empresaId);

        // Uso_suscripcion filtrado por suscripción activa (evita duplicados)
        List<UsoSuscripcion> usoSuscripcion = suscripcionActiva != null
            ? usoSuscripcionRepository.findBySuscripcionIdOrderByAnioDescMesDesc(suscripcionActiva.getId())
            : List.of();
        
        return ResponseEntity.ok(mapOf(
                "usoSuscripcion", usoSuscripcion.stream().map(this::usoDto).toList(),
                "limites", planDto(suscripcionActiva != null ? suscripcionActiva.getPlanLicencia() : null)
        ));
    }

    @GetMapping("/pagos")
    public ResponseEntity<List<Map<String, Object>>> pagos() {
        UUID empresaId = empresaActual();
        return ResponseEntity.ok(pagoRepository.findByEmpresaId(empresaId).stream().map(this::pagoDto).toList());
    }

    @GetMapping("/apis")
    public ResponseEntity<Map<String, Object>> apis() {
        UUID empresaId = empresaActual();
        List<Map<String, Object>> consultas = observabilityService.ultimasConsultasExternas(empresaId, 50);
        return ResponseEntity.ok(mapOf(
                "resumen", apiResumenDto(consultas),
                "consultas", consultas.stream().map(this::consultaDto).toList()
        ));
    }

    @GetMapping("/errores")
    public ResponseEntity<Map<String, Object>> errores(@RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) String origen,
                                                       @RequestParam(required = false) String desde,
                                                       @RequestParam(required = false) String hasta) {
        UUID empresaId = empresaActual();
        List<ApiErrorDescriptor> catalogo = observabilityService.catalogoErrores();
        List<Map<String, Object>> eventosRecientes = observabilityService.apiErrors(empresaId, status, origen, parseDate(desde), parseDate(hasta));
        List<Map<String, Object>> eventosInternos = eventosRecientes.stream()
                .filter(evento -> "INTERNA".equalsIgnoreCase(String.valueOf(evento.get("tipo"))))
                .toList();
        List<Map<String, Object>> eventosExternos = eventosRecientes.stream()
                .filter(evento -> "EXTERNA".equalsIgnoreCase(String.valueOf(evento.get("tipo"))))
                .toList();
        List<Integer> statusCodes = catalogo.stream()
                .map(ApiErrorDescriptor::statusCode)
                .collect(Collectors.toSet())
                .stream()
                .sorted()
                .toList();
        List<String> origenes = observabilityService.origenesError(empresaId);
        return ResponseEntity.ok(mapOf(
                "catalogo", catalogo,
                "internas", eventosInternos,
                "externas", eventosExternos,
                "eventosExternos", eventosExternos,
                "eventosRecientes", eventosRecientes,
                "origenes", origenes,
                "statusCodes", statusCodes
        ));
    }

    @GetMapping("/system-overview")
    public ResponseEntity<Map<String, Object>> systemOverview() {
        return ResponseEntity.ok(observabilityService.overview(empresaActual()));
    }

    @GetMapping("/conectividad")
    public ResponseEntity<Map<String, Object>> conectividad() {
        UUID empresaId = empresaActual();
        InstalacionLocal instalacion = instalacionRepository.findTopByEmpresaIdOrderByActivadaEnDesc(empresaId)
                .orElse(null);
        return ResponseEntity.ok(mapOf(
                "controlPlane", conectividadDto(instalacion),
                "eventosLicencia", eventos(instalacion)
        ));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<Map<String, Object>>> usuarios() {
        UUID empresaId = empresaActual();
        return ResponseEntity.ok(usuarioEmpresaRepository.findByEmpresaIdAndActivoTrueOrderByUsuarioNombreAsc(empresaId)
                .stream().map(this::usuarioEmpresaDto).toList());
    }

    @GetMapping("/configuracion")
    public ResponseEntity<Map<String, Object>> configuracion() {
        UUID empresaId = empresaActual();
        PlanLicencia plan = suscripcionActiva(empresaId) != null ? suscripcionActiva(empresaId).getPlanLicencia() : null;
        Map<String, Object> paqueteControlPlane = controlPlaneClient.configurationPackage();
        Map<String, Object> configuracionLocal = observabilityService.configuracionLocal(empresaId);
        return ResponseEntity.ok(mapOf(
                "empresaId", empresaId,
                "jobsHabilitados", jobsHabilitados,
                "parametrosEditables", configuracionLocal.get("parametrosEditables"),
                "jobs", configuracionLocal.get("jobs"),
                "modulosPlan", plan != null ? plan.getModulosIncluidosJson() : null,
                "paqueteControlPlane", paqueteControlPlane
        ));
    }

    @PatchMapping("/configuracion/parametros/{codigo}")
    @Transactional
    public ResponseEntity<Map<String, Object>> actualizarParametro(
            @PathVariable String codigo,
            @RequestBody Map<String, Object> nuevoDetalle,
            HttpServletRequest request) {
        UUID empresaId = empresaActual();
        UUID usuarioId = TenantContext.getUsuarioId();
        String ip = ClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        Map<String, Object> resultado = observabilityService.actualizarParametro(empresaId, codigo, nuevoDetalle, usuarioId, ip, userAgent);
        return ResponseEntity.ok(mapOf("parametro", resultado));
    }

    @PostMapping("/catalogos/sincronizar")
    @Transactional
    public ResponseEntity<Map<String, Object>> sincronizarCatalogos(HttpServletRequest request) {
        UUID empresaId = empresaActual();
        Map<String, Object> manifest = controlPlaneClient.catalogManifest();
        auditoriaService.registrar(TenantContext.getUsuarioId(), empresaId, "SOLICITAR_SYNC_CATALOGOS",
                "Solicitud manual de sincronizacion de catalogos desde Admin Empresa",
                ClientIpResolver.resolve(request), request.getHeader("User-Agent"),
                "catalogo_sync", empresaId, null, "{\"manifestOnline\":" + Boolean.TRUE.equals(manifest.get("online")) + "}");
        return ResponseEntity.ok(mapOf(
                "estado", Boolean.TRUE.equals(manifest.get("online")) ? "MANIFEST_RECIBIDO" : "SIN_CONECTIVIDAD",
                "mensaje", Boolean.TRUE.equals(manifest.get("online"))
                        ? "Manifest de catalogos recibido desde Control Plane simulado."
                        : "La solicitud quedo auditada, pero el Control Plane no esta disponible.",
                "controlPlaneHabilitado", controlPlaneClient.habilitado(),
                "manifest", manifest
        ));
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<Map<String, Object>>> auditoria() {
        UUID empresaId = empresaActual();
        return ResponseEntity.ok(auditoriaRepository.findTop50ByEmpresaIdOrderByFechaEventoDesc(empresaId)
                .stream().map(this::auditoriaDto).toList());
    }

    private UUID empresaActual() {
        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new BusinessException("TENANT_REQUERIDO", "No se pudo resolver la empresa del usuario autenticado");
        }
        return empresaId;
    }

    private Suscripcion suscripcionActiva(UUID empresaId) {
        return suscripcionRepository.findByEmpresaId(empresaId).stream()
                .max(Comparator.comparing(Suscripcion::getFechaFin))
                .orElse(null);
    }

    private LicenciaLocal licenciaVigente(InstalacionLocal instalacion) {
        return instalacion == null ? null
                : licenciaRepository.findTopByInstalacionIdOrderByEmitidaEnDesc(instalacion.getId()).orElse(null);
    }

    private List<Map<String, Object>> eventos(InstalacionLocal instalacion) {
        return instalacion == null ? List.of()
                : eventoRepository.findTop20ByInstalacionIdOrderByFechaEventoDesc(instalacion.getId())
                .stream().map(e -> mapOf(
                        "id", e.getId(),
                        "tipoEvento", e.getTipoEvento(),
                        "resultado", e.getResultado(),
                        "correlationId", e.getCorrelationId(),
                        "fechaEvento", e.getFechaEvento(),
                        "detalle", e.getDetalleSanitizadoJson()
                )).toList();
    }

    private Map<String, Object> empresaDto(Empresa e) {
        if (e == null) return null;
        return mapOf("id", e.getId(), "codigo", e.getCodigo(), "nombre", e.getNombre(), "ruc", e.getRuc(),
                "emailContacto", e.getEmailContacto(), "telefonoContacto", e.getTelefonoContacto(), "estado", e.getEstado());
    }

    private Map<String, Object> suscripcionDto(Suscripcion s) {
        if (s == null) return null;
        return mapOf("id", s.getId(), "planId", s.getPlanLicencia().getId(), "plan", s.getPlanLicencia().getNombre(),
                "fechaInicio", s.getFechaInicio(), "fechaFin", s.getFechaFin(), "estado", s.getEstado(),
                "renovacionAutomatica", s.getRenovacionAutomatica());
    }

    private Map<String, Object> planDto(PlanLicencia p) {
        if (p == null) return null;
        return mapOf("id", p.getId(), "codigo", p.getCodigo(), "nombre", p.getNombre(),
                "limiteUsuarios", p.getLimiteUsuarios(),
                "limiteTransaccionesMensuales", p.getLimiteTransaccionesMensuales(),
                "limiteConsultasKycMensuales", p.getLimiteConsultasKycMensuales(),
                "limiteReportesMensuales", p.getLimiteReportesMensuales(),
                "limiteReglas", p.getLimiteReglas(),
                "limiteHistorialTransaccional", p.getLimiteHistorialTransaccional(),
                "limiteEscenarios", p.getLimiteEscenarios(),
                "modulosIncluidosJson", p.getModulosIncluidosJson(),
                "precioAnual", p.getPrecioAnual());
    }

    private Map<String, Object> instalacionDto(InstalacionLocal i) {
        if (i == null) return null;
        return mapOf("id", i.getId(), "identificadorInstalacion", i.getIdentificadorInstalacion(),
                "estado", i.getEstado(), "versionProducto", i.getVersionProducto(),
                "activadaEn", i.getActivadaEn(), "ultimoHeartbeatEn", i.getUltimoHeartbeatEn(),
                "clonDetectado", i.isClonDetectado());
    }

    private Map<String, Object> licenciaDto(LicenciaLocal l) {
        if (l == null) return null;
        return mapOf("id", l.getId(), "estado", l.getEstado(), "planCodigo", l.getPlanCodigo(),
                "planVersion", l.getPlanVersion(), "emitidaEn", l.getEmitidaEn(), "venceEn", l.getVenceEn(),
                "diasGracia", l.getDiasGracia(), "ultimaValidacionEn", l.getUltimaValidacionEn(),
                "kid", l.getKidFirma());
    }

    private Map<String, Object> usoDto(UsoSuscripcion u) {
        if (u == null) return null;
        return mapOf("id", u.getId(), "anio", u.getAnio(), "mes", u.getMes(),
                "usuariosActivos", u.getUsuariosActivos(), "transaccionesProcesadas", u.getTransaccionesProcesadas(),
                "consultasKyc", u.getConsultasKyc(), "alertasGeneradas", u.getAlertasGeneradas(),
                "reportesGenerados", u.getReportesGenerados());
    }

    private Map<String, Object> pagoDto(Pago p) {
        return mapOf("id", p.getId(), "codigo", p.getCodigo(), "monto", p.getMonto(), "moneda", p.getMoneda(),
                "fechaPago", p.getFechaPago(), "metodoPago", p.getMetodoPago(),
                "comprobanteReferencia", p.getComprobanteReferencia(), "estado", p.getEstado());
    }

    private Map<String, Object> consultaDto(Map<String, Object> c) {
        return mapOf("id", c.get("id"), "tipoConsulta", c.get("tipo_consulta"), "proveedor", c.get("proveedor"),
                "statusHttp", c.get("status_http"), "duracionMs", c.get("duracion_ms"), "intentos", c.get("intentos"),
                "resultado", c.get("resultado"), "resultadoFuncional", c.get("resultado_funcional"),
                "categoriaError", c.get("categoria_error"), "estado", c.get("estado"),
                "fechaConsulta", c.get("fecha_consulta"), "correlationId", c.get("correlation_id"));
    }

    private Map<String, Object> consultaErrorDto(Map<String, Object> c) {
        String codigo = c.get("categoria_error") != null && !String.valueOf(c.get("categoria_error")).isBlank()
                ? String.valueOf(c.get("categoria_error"))
                : "HTTP_" + c.get("status_http");
        String proveedor = c.get("proveedor") != null ? String.valueOf(c.get("proveedor")) : "PROVEEDOR";
        ApiErrorDescriptor descriptor = observabilityService.catalogoErrores().stream()
                .filter(error -> error.codigoError().equalsIgnoreCase(codigo))
                .filter(error -> proveedor == null || error.api().equalsIgnoreCase(proveedor))
                .findFirst()
                .or(() -> observabilityService.catalogoErrores().stream()
                        .filter(error -> error.codigoError().equalsIgnoreCase(codigo))
                        .findFirst())
                .orElse(new ApiErrorDescriptor(
                        proveedor,
                        "EXTERNA",
                        proveedor,
                        codigo,
                        c.get("status_http") != null ? ((Number) c.get("status_http")).intValue() : 0,
                        "Error reportado por API externa",
                        "Evento registrado en api_evento.",
                        "EXTERNA",
                        c.get("fecha_consulta") != null ? (OffsetDateTime) c.get("fecha_consulta") : null
                ));
        return mapOf("id", c.get("id"),
                "origen", descriptor.origen(),
                "tipoOrigen", descriptor.tipoOrigen(),
                "api", descriptor.api(),
                "codigo_error", descriptor.codigoError(),
                "status_code", c.get("status_http") != null ? c.get("status_http") : descriptor.statusCode(),
                "mensaje", descriptor.mensaje(),
                "detalles", descriptor.detalles(),
                "tipoConsulta", c.get("tipo_consulta"),
                "proveedor", proveedor,
                "estado", c.get("estado"),
                "fechaConsulta", c.get("fecha_consulta"),
                "correlationId", c.get("correlation_id"),
                "duracionMs", c.get("duracion_ms"),
                "intentos", c.get("intentos"));
    }

    private Map<String, Object> apiResumenDto(List<Map<String, Object>> consultas) {
        Map<String, Long> porEstado = consultas.stream()
                .collect(Collectors.groupingBy(c -> c.get("estado") != null ? String.valueOf(c.get("estado")) : "SIN_ESTADO",
                        LinkedHashMap::new, Collectors.counting()));
        long errores = consultas.stream().filter(c -> c.get("categoria_error") != null
                && !String.valueOf(c.get("categoria_error")).isBlank()).count();
        long exitosas = consultas.stream()
                .filter(c -> "EXITOSO".equalsIgnoreCase(String.valueOf(c.get("resultado")))).count();
        return mapOf("total", consultas.size(), "exitosas", exitosas, "errores", errores, "porEstado", porEstado);
    }

    private Map<String, Object> conectividadDto(InstalacionLocal instalacion) {
        return mapOf("controlPlaneHabilitado", controlPlaneClient.habilitado(),
                "estado", controlPlaneClient.habilitado() ? "CONFIGURADO" : "NO_CONFIGURADO",
                "ultimoHeartbeatEn", instalacion != null ? instalacion.getUltimoHeartbeatEn() : null,
                "estadoInstalacion", instalacion != null ? instalacion.getEstado() : "SIN_INSTALACION");
    }

    private Map<String, Object> usuarioEmpresaDto(UsuarioEmpresa ue) {
        return mapOf("id", ue.getId(), "usuarioId", ue.getUsuario().getId(), "nombre", ue.getUsuario().getNombre(),
                "email", ue.getUsuario().getEmail(), "activo", ue.getUsuario().getActivo(),
                "rolId", ue.getRol().getId(), "rol", ue.getRol().getCodigo(), "rolNombre", ue.getRol().getNombre(),
                "asignacionActiva", ue.getActivo());
    }

    private Map<String, Object> auditoriaDto(Auditoria a) {
        return mapOf("id", a.getId(), "usuarioId", a.getUsuarioId(), "accion", a.getAccion(),
                "descripcion", a.getDescripcion(), "entidadAfectada", a.getEntidadAfectada(),
                "entidadId", a.getEntidadId(), "direccionIp", a.getDireccionIp(), "fechaEvento", a.getFechaEvento());
    }

    private OffsetDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
