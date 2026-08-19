package com.antifraude.licensing;

import com.antifraude.audit.AuditoriaService;
import com.antifraude.exception.BusinessException;
import com.antifraude.security.tenant.TenantContext;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin-empresa/solicitud-roles")
public class SolicitudRolesController {

    private static final Logger log = LoggerFactory.getLogger(SolicitudRolesController.class);

    private final SolicitudRolesRepository solicitudRolesRepository;
    private final PlanLicenciaRepository planLicenciaRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final RolesAdquiridosRepository rolesAdquiridosRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public SolicitudRolesController(SolicitudRolesRepository solicitudRolesRepository,
                                     PlanLicenciaRepository planLicenciaRepository,
                                     UsuarioEmpresaRepository usuarioEmpresaRepository,
                                     RolesAdquiridosRepository rolesAdquiridosRepository,
                                     PagoRepository pagoRepository,
                                     UsuarioRepository usuarioRepository,
                                     AuditoriaService auditoriaService) {
        this.solicitudRolesRepository = solicitudRolesRepository;
        this.planLicenciaRepository = planLicenciaRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.rolesAdquiridosRepository = rolesAdquiridosRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    /** Verifica si la empresa puede crear un usuario del rol indicado. */
    @GetMapping("/verificar")
    @PreAuthorize("hasAuthority('USUARIOS_CREAR')")
    public ResponseEntity<?> verificarDisponibilidad(@RequestParam String rolCodigo) {
        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new BusinessException("EMPRESA_NO_IDENTIFICADA", "No se pudo identificar la empresa");
        }

        PlanLicencia plan = obtenerPlanActual(empresaId);
        int limite = obtenerLimitePorRol(plan, rolCodigo);
        long usados = solicitudRolesRepository.contarUsuariosPorRol(empresaId, rolCodigo);
        long extras = solicitudRolesRepository.contarRolesAdquiridosExtra(empresaId, rolCodigo);
        int totalDisponible = limite + (int) extras;

        return ResponseEntity.ok(Map.of(
                "rolCodigo", rolCodigo,
                "limitePlan", limite,
                "rolesAdquiridosExtra", extras,
                "totalDisponible", totalDisponible,
                "usados", usados,
                "disponibles", totalDisponible - usados,
                "puedeCrear", usados < totalDisponible
        ));
    }

    /** Solicita roles adicionales (trigger del flujo de compra). */
    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS_CREAR')")
    public ResponseEntity<?> crearSolicitud(@RequestBody SolicitudRequest request,
                                            HttpServletRequest httpRequest) {
        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new BusinessException("EMPRESA_NO_IDENTIFICADA", "No se pudo identificar la empresa");
        }

        Empresa empresa = new Empresa();
        empresa.setId(empresaId);

        var precioRol = planLicenciaRepository.findPrecioRol(rolCodigoToId(request.rolCodigo()), empresaId);
        BigDecimal precioUnitario = precioRol != null ? precioRol : BigDecimal.valueOf(400);
        BigDecimal precioTotal = precioUnitario.multiply(BigDecimal.valueOf(request.cantidad()));

        SolicitudRoles solicitud = SolicitudRoles.builder()
                .empresa(empresa)
                .rolSolicitado(request.rolCodigo())
                .cantidad(request.cantidad())
                .precioUnitario(precioUnitario)
                .precioTotal(precioTotal)
                .observacion(request.observacion())
                .estado("PENDIENTE")
                .build();
        solicitud = solicitudRolesRepository.save(solicitud);

        auditoriaService.registrar(TenantContext.getUsuarioId(), empresaId, "SOLICITUD_ROLES",
                "Solicitud de " + request.cantidad() + " rol(es) " + request.rolCodigo(),
                httpRequest.getRemoteAddr(), null, "solicitud_roles", null, null, null);

        log.info("[SOLICITUD] Creada solicitud {} para empresa {}: {} x{}",
                solicitud.getId(), empresaId, request.rolCodigo(), request.cantidad());

        return ResponseEntity.ok(Map.of(
                "solicitudId", solicitud.getId(),
                "precioUnitario", precioUnitario,
                "precioTotal", precioTotal,
                "estado", "PENDIENTE"
        ));
    }

    /** Simula el pago de una solicitud (pasarela simulada). */
    @PostMapping("/{solicitudId}/pagar")
    @PreAuthorize("hasAuthority('USUARIOS_CREAR')")
    public ResponseEntity<?> simularPago(@PathVariable Long solicitudId,
                                         @RequestBody PagoSimRequest request,
                                         HttpServletRequest httpRequest) {
        SolicitudRoles solicitud = solicitudRolesRepository.findById(solicitudId)
                .orElseThrow(() -> new BusinessException("SOLICITUD_NO_ENCONTRADA", "Solicitud no encontrada"));

        if (!"PENDIENTE".equals(solicitud.getEstado()) && !"APROBADA".equals(solicitud.getEstado())) {
            throw new BusinessException("SOLICITUD_NO_PAGABLE", "La solicitud no esta en estado pagable");
        }

        UUID empresaId = TenantContext.getEmpresaId();

        Pago pago = Pago.builder()
                .empresa(solicitud.getEmpresa())
                .codigo("PAGO-SOL-" + solicitudId + "-" + System.currentTimeMillis())
                .monto(solicitud.getPrecioTotal())
                .metodoPago("SIMULADO")
                .concepto("Roles adicionales: " + solicitud.getRolSolicitado() + " x" + solicitud.getCantidad())
                .referenciaExterna(request.referenciaExterna() != null ? request.referenciaExterna() : "SIM-" + System.currentTimeMillis())
                .solicitudRoles(solicitud)
                .estado(Pago.EstadoPago.PAGADO)
                .fechaPago(OffsetDateTime.now())
                .build();
        pagoRepository.save(pago);

        solicitud.setEstado("PAGADA");
        solicitudRolesRepository.save(solicitud);

        RolesAdquiridos rolAdquirido = RolesAdquiridos.builder()
                .empresa(solicitud.getEmpresa())
                .solicitudRoles(solicitud)
                .rolCodigo(solicitud.getRolSolicitado())
                .cantidad(solicitud.getCantidad())
                .build();
        rolesAdquiridosRepository.save(rolAdquirido);

        auditoriaService.registrar(TenantContext.getUsuarioId(), empresaId, "PAGO_SIMULADO",
                "Pago simulado por solicitud " + solicitudId + ": " + solicitud.getPrecioTotal(),
                httpRequest.getRemoteAddr(), null, "pago", null, null, null);

        log.info("[PAGO] Solicitud {} pagada (simulado): {} {}",
                solicitudId, solicitud.getPrecioTotal(), "PYG");

        return ResponseEntity.ok(Map.of(
                "pagoId", pago.getId(),
                "solicitudId", solicitudId,
                "monto", solicitud.getPrecioTotal(),
                "metodo", "SIMULADO",
                "estado", "PAGADO",
                "mensaje", "Pago procesado. Los roles ya estan disponibles para invitacion."
        ));
    }

    private PlanLicencia obtenerPlanActual(UUID empresaId) {
        return planLicenciaRepository.findPlanActivoByEmpresa(empresaId)
                .orElseThrow(() -> new BusinessException("PLAN_NO_ENCONTRADO",
                        "No se encontro un plan activo para la empresa"));
    }

    private int obtenerLimitePorRol(PlanLicencia plan, String rolCodigo) {
        return switch (rolCodigo) {
            case "ADMINISTRADOR" -> plan.getLimiteAdministradores() != null ? plan.getLimiteAdministradores() : 1;
            case "SUPERVISOR" -> plan.getLimiteSupervisores() != null ? plan.getLimiteSupervisores() : 1;
            case "ANALISTA" -> plan.getLimiteAnalistas() != null ? plan.getLimiteAnalistas() : 2;
            case "AUDITOR" -> plan.getLimiteAuditores() != null ? plan.getLimiteAuditores() : 1;
            default -> 0;
        };
    }

    private String rolCodigoToId(String rolCodigo) {
        return rolCodigo;
    }

    public record SolicitudRequest(String rolCodigo, int cantidad, String observacion) {}
    public record PagoSimRequest(String referenciaExterna) {}
}
