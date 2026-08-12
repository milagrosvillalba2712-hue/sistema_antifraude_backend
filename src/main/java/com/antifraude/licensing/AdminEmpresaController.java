package com.antifraude.licensing;

import com.antifraude.audit.Auditoria;
import com.antifraude.audit.AuditoriaRepository;
import com.antifraude.audit.AuditoriaService;
import com.antifraude.config.ClientIpResolver;
import com.antifraude.exception.BusinessException;
import com.antifraude.external.ConsultaExterna;
import com.antifraude.external.ConsultaExternaRepository;
import com.antifraude.security.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
    private final ConsumoLicenciaLocalRepository consumoLocalRepository;
    private final EventoLicenciaLocalRepository eventoRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final ConsultaExternaRepository consultaExternaRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final LicensingValidationService validationService;
    private final LicensingControlPlaneClient controlPlaneClient;
    private final AuditoriaService auditoriaService;

    public AdminEmpresaController(EmpresaRepository empresaRepository,
                                  SuscripcionRepository suscripcionRepository,
                                  PagoRepository pagoRepository,
                                  UsoSuscripcionRepository usoSuscripcionRepository,
                                  InstalacionLocalRepository instalacionRepository,
                                  LicenciaLocalRepository licenciaRepository,
                                  ConsumoLicenciaLocalRepository consumoLocalRepository,
                                  EventoLicenciaLocalRepository eventoRepository,
                                  UsuarioEmpresaRepository usuarioEmpresaRepository,
                                  ConsultaExternaRepository consultaExternaRepository,
                                  AuditoriaRepository auditoriaRepository,
                                  LicensingValidationService validationService,
                                  LicensingControlPlaneClient controlPlaneClient,
                                  AuditoriaService auditoriaService) {
        this.empresaRepository = empresaRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.usoSuscripcionRepository = usoSuscripcionRepository;
        this.instalacionRepository = instalacionRepository;
        this.licenciaRepository = licenciaRepository;
        this.consumoLocalRepository = consumoLocalRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.consultaExternaRepository = consultaExternaRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.validationService = validationService;
        this.controlPlaneClient = controlPlaneClient;
        this.auditoriaService = auditoriaService;
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
                .findByEmpresaIdAndAnioAndMes(empresaId, LocalDate.now().getYear(), LocalDate.now().getMonthValue())
                .orElse(null);
        List<ConsultaExterna> consultas = consultaExternaRepository.findTop50ByEmpresaIdOrderByFechaConsultaDesc(empresaId);

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
        LicensingValidationService.ResultadoValidacion resultado = validationService.validar(instalacion.getId(), true);
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
                "venceEn", resultado.venceEn()
        ));
    }

    @GetMapping("/consumo")
    public ResponseEntity<Map<String, Object>> consumo() {
        UUID empresaId = empresaActual();
        InstalacionLocal instalacion = instalacionRepository.findTopByEmpresaIdOrderByActivadaEnDesc(empresaId)
                .orElse(null);
        List<ConsumoLicenciaLocal> consumoLocal = instalacion == null
                ? List.of()
                : consumoLocalRepository.findTop12ByInstalacionIdOrderByAnioDescMesDesc(instalacion.getId());
        return ResponseEntity.ok(mapOf(
                "usoSuscripcion", usoSuscripcionRepository.findByEmpresaIdOrderByAnioDescMesDesc(empresaId).stream()
                        .map(this::usoDto).toList(),
                "consumoLocal", consumoLocal.stream().map(this::consumoLocalDto).toList(),
                "limites", planDto(suscripcionActiva(empresaId) != null ? suscripcionActiva(empresaId).getPlanLicencia() : null)
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
        List<ConsultaExterna> consultas = consultaExternaRepository.findTop50ByEmpresaIdOrderByFechaConsultaDesc(empresaId);
        return ResponseEntity.ok(mapOf(
                "resumen", apiResumenDto(consultas),
                "consultas", consultas.stream().map(this::consultaDto).toList()
        ));
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
        return ResponseEntity.ok(mapOf(
                "empresaId", empresaId,
                "parametrosEditables", List.of(
                        "Frecuencia de heartbeat",
                        "Frecuencia de sincronizacion de catalogos",
                        "Modo offline dentro de gracia",
                        "Notificaciones administrativas",
                        "Ventanas de mantenimiento"
                ),
                "jobs", List.of(
                        mapOf("codigo", "LICENSE_HEARTBEAT", "descripcion", "Reporta vida de instalacion", "estado", "ACTIVO"),
                        mapOf("codigo", "LICENSE_USAGE_SYNC", "descripcion", "Actualiza consumo local", "estado", "ACTIVO"),
                        mapOf("codigo", "CATALOG_SYNC", "descripcion", "Sincroniza catalogos permitidos", "estado", "PENDIENTE_IMPLEMENTACION")
                ),
                "modulosPlan", plan != null ? plan.getModulosIncluidosJson() : null,
                "paqueteControlPlane", paqueteControlPlane
        ));
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

    private Map<String, Object> consumoLocalDto(ConsumoLicenciaLocal c) {
        return mapOf("id", c.getId(), "anio", c.getAnio(), "mes", c.getMes(),
                "usuariosActivos", c.getUsuariosActivos(), "transaccionesProcesadas", c.getTransaccionesProcesadas(),
                "consultasKyc", c.getConsultasKyc(), "alertasGeneradas", c.getAlertasGeneradas(),
                "reportesGenerados", c.getReportesGenerados(), "fechaHoraModificacion", c.getFechaHoraModificacion());
    }

    private Map<String, Object> pagoDto(Pago p) {
        return mapOf("id", p.getId(), "codigo", p.getCodigo(), "monto", p.getMonto(), "moneda", p.getMoneda(),
                "fechaPago", p.getFechaPago(), "metodoPago", p.getMetodoPago(),
                "comprobanteReferencia", p.getComprobanteReferencia(), "estado", p.getEstado());
    }

    private Map<String, Object> consultaDto(ConsultaExterna c) {
        return mapOf("id", c.getId(), "tipoConsulta", c.getTipoConsulta(), "proveedor", c.getProveedor(),
                "statusHttp", c.getStatusHttp(), "duracionMs", c.getDuracionMs(), "intentos", c.getIntentos(),
                "resultado", c.getResultado(), "resultadoFuncional", c.getResultadoFuncional(),
                "categoriaError", c.getCategoriaError(), "estado", c.getEstado(), "fechaConsulta", c.getFechaConsulta(),
                "correlationId", c.getCorrelationId());
    }

    private Map<String, Object> apiResumenDto(List<ConsultaExterna> consultas) {
        Map<String, Long> porEstado = consultas.stream()
                .collect(Collectors.groupingBy(c -> c.getEstado() != null ? c.getEstado() : "SIN_ESTADO",
                        LinkedHashMap::new, Collectors.counting()));
        long errores = consultas.stream().filter(c -> c.getCategoriaError() != null && !c.getCategoriaError().isBlank()).count();
        long exitosas = consultas.stream().filter(c -> Boolean.TRUE.equals(c.getResultado())).count();
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

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
