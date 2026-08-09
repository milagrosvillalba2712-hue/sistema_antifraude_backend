package com.antifraude.licensing;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/licensing")
@Transactional(readOnly = true)
public class LicensingController {

    private final EmpresaRepository empresaRepository;
    private final PlanLicenciaRepository planLicenciaRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final ContratoRepository contratoRepository;
    private final PagoRepository pagoRepository;
    private final UsoSuscripcionRepository usoSuscripcionRepository;
    private final RolSistemaRepository rolSistemaRepository;
    private final PermisoSistemaRepository permisoSistemaRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;

    public LicensingController(EmpresaRepository empresaRepository,
                               PlanLicenciaRepository planLicenciaRepository,
                               SuscripcionRepository suscripcionRepository,
                               ContratoRepository contratoRepository,
                               PagoRepository pagoRepository,
                               UsoSuscripcionRepository usoSuscripcionRepository,
                               RolSistemaRepository rolSistemaRepository,
                               PermisoSistemaRepository permisoSistemaRepository,
                               UsuarioEmpresaRepository usuarioEmpresaRepository) {
        this.empresaRepository = empresaRepository;
        this.planLicenciaRepository = planLicenciaRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.contratoRepository = contratoRepository;
        this.pagoRepository = pagoRepository;
        this.usoSuscripcionRepository = usoSuscripcionRepository;
        this.rolSistemaRepository = rolSistemaRepository;
        this.permisoSistemaRepository = permisoSistemaRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
    }

    @GetMapping("/empresas")
    public ResponseEntity<List<Map<String, Object>>> empresas() {
        return ResponseEntity.ok(empresaRepository.findAll().stream().map(e -> mapOf(
                "id", e.getId(), "codigo", e.getCodigo(), "nombre", e.getNombre(), "ruc", e.getRuc(),
                "emailContacto", e.getEmailContacto(), "telefonoContacto", e.getTelefonoContacto(), "estado", e.getEstado()
        )).toList());
    }

    @GetMapping("/planes")
    public ResponseEntity<List<Map<String, Object>>> planes() {
        return ResponseEntity.ok(planLicenciaRepository.findAll().stream().map(p -> mapOf(
                "id", p.getId(), "codigo", p.getCodigo(), "nombre", p.getNombre(), "limiteUsuarios", p.getLimiteUsuarios(),
                "limiteTransaccionesMensuales", p.getLimiteTransaccionesMensuales(), "precioAnual", p.getPrecioAnual(), "activo", p.getActivo()
        )).toList());
    }

    @GetMapping("/suscripciones")
    public ResponseEntity<List<Map<String, Object>>> suscripciones(@RequestParam(required = false) UUID empresaId) {
        List<Suscripcion> rows = empresaId != null
                ? suscripcionRepository.findByEmpresaId(empresaId)
                : suscripcionRepository.findAll();
        return ResponseEntity.ok(rows.stream().map(s -> mapOf(
                "id", s.getId(), "empresaId", s.getEmpresa().getId(), "empresa", s.getEmpresa().getNombre(),
                "planId", s.getPlanLicencia().getId(), "plan", s.getPlanLicencia().getNombre(),
                "fechaInicio", s.getFechaInicio(), "fechaFin", s.getFechaFin(), "estado", s.getEstado(),
                "renovacionAutomatica", s.getRenovacionAutomatica()
        )).toList());
    }

    @GetMapping("/contratos")
    public ResponseEntity<List<Map<String, Object>>> contratos() {
        return ResponseEntity.ok(contratoRepository.findAll().stream().map(c -> mapOf(
                "id", c.getId(), "empresaId", c.getEmpresa().getId(), "empresa", c.getEmpresa().getNombre(),
                "numero", c.getNumero(), "fechaFirma", c.getFechaFirma(), "estado", c.getEstado(),
                "urlDocumento", c.getUrlDocumento()
        )).toList());
    }

    @GetMapping("/pagos")
    public ResponseEntity<List<Map<String, Object>>> pagos(@RequestParam(required = false) UUID empresaId) {
        List<Pago> rows = empresaId != null ? pagoRepository.findByEmpresaId(empresaId) : pagoRepository.findAll();
        return ResponseEntity.ok(rows.stream().map(p -> mapOf(
                "id", p.getId(), "empresaId", p.getEmpresa().getId(), "empresa", p.getEmpresa().getNombre(),
                "referencia", p.getReferencia(), "monto", p.getMonto(), "moneda", p.getMoneda(),
                "fechaPago", p.getFechaPago(), "estado", p.getEstado()
        )).toList());
    }

    @GetMapping("/uso")
    public ResponseEntity<List<Map<String, Object>>> uso(@RequestParam(required = false) UUID empresaId) {
        List<UsoSuscripcion> rows = empresaId != null
                ? usoSuscripcionRepository.findByEmpresaIdOrderByAnioDescMesDesc(empresaId)
                : usoSuscripcionRepository.findAll();
        return ResponseEntity.ok(rows.stream().map(u -> mapOf(
                "id", u.getId(), "empresaId", u.getEmpresa().getId(), "empresa", u.getEmpresa().getNombre(),
                "anio", u.getAnio(), "mes", u.getMes(), "usuariosActivos", u.getUsuariosActivos(),
                "transaccionesProcesadas", u.getTransaccionesProcesadas(), "consultasKyc", u.getConsultasKyc(),
                "alertasGeneradas", u.getAlertasGeneradas(), "reportesGenerados", u.getReportesGenerados()
        )).toList());
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> roles() {
        return ResponseEntity.ok(rolSistemaRepository.findAll().stream().map(r -> mapOf(
                "id", r.getId(), "codigo", r.getCodigo(), "nombre", r.getNombre(), "tipo", r.getTipo(), "activo", r.getActivo()
        )).toList());
    }

    @GetMapping("/permisos")
    public ResponseEntity<List<Map<String, Object>>> permisos() {
        return ResponseEntity.ok(permisoSistemaRepository.findAll().stream().map(p -> mapOf(
                "id", p.getId(), "codigo", p.getCodigo(), "descripcion", p.getDescripcion()
        )).toList());
    }

    @GetMapping("/usuario-empresa")
    public ResponseEntity<List<Map<String, Object>>> usuarioEmpresa() {
        return ResponseEntity.ok(usuarioEmpresaRepository.findAll().stream().map(ue -> mapOf(
                "id", ue.getId(), "usuarioId", ue.getUsuario().getId(), "usuario", ue.getUsuario().getEmail(),
                "empresaId", ue.getEmpresa() != null ? ue.getEmpresa().getId() : null,
                "empresa", ue.getEmpresa() != null ? ue.getEmpresa().getNombre() : "Global",
                "rolId", ue.getRol().getId(), "rol", ue.getRol().getCodigo(), "activo", ue.getActivo()
        )).toList());
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
