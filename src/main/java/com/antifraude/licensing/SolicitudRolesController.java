package com.antifraude.licensing;

import com.antifraude.audit.AuditoriaService;
import com.antifraude.common.entity.Moneda;
import com.antifraude.common.repository.MonedaRepository;
import com.antifraude.exception.BusinessException;
import com.antifraude.security.tenant.TenantContext;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
    private final LicensingControlPlaneClient controlPlaneClient;
    private final MonedaRepository monedaRepository;

    public SolicitudRolesController(SolicitudRolesRepository solicitudRolesRepository,
                                     PlanLicenciaRepository planLicenciaRepository,
                                     UsuarioEmpresaRepository usuarioEmpresaRepository,
                                     RolesAdquiridosRepository rolesAdquiridosRepository,
                                     PagoRepository pagoRepository,
                                     UsuarioRepository usuarioRepository,
                                     AuditoriaService auditoriaService,
                                     LicensingControlPlaneClient controlPlaneClient,
                                     MonedaRepository monedaRepository) {
        this.solicitudRolesRepository = solicitudRolesRepository;
        this.planLicenciaRepository = planLicenciaRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.rolesAdquiridosRepository = rolesAdquiridosRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
        this.controlPlaneClient = controlPlaneClient;
        this.monedaRepository = monedaRepository;
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

        List<BigDecimal> preciosRol = planLicenciaRepository.findPreciosRol(rolCodigoToId(request.rolCodigo()), empresaId);
        BigDecimal precioUnitario = preciosRol.isEmpty() ? BigDecimal.valueOf(400) : preciosRol.get(0);
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

    /** Inicia el pago real de una solicitud con Stripe Checkout. */
    @PostMapping("/{solicitudId}/pagar")
    @PreAuthorize("hasAuthority('USUARIOS_CREAR')")
    @Transactional
    public ResponseEntity<?> iniciarPagoStripe(@PathVariable Long solicitudId,
                                               @RequestBody(required = false) PagoRequest request,
                                               HttpServletRequest httpRequest) {
        SolicitudRoles solicitud = solicitudRolesRepository.findById(solicitudId)
                .orElseThrow(() -> new BusinessException("SOLICITUD_NO_ENCONTRADA", "Solicitud no encontrada"));

        if (!"PENDIENTE".equals(solicitud.getEstado()) && !"APROBADA".equals(solicitud.getEstado())) {
            throw new BusinessException("SOLICITUD_NO_PAGABLE", "La solicitud no esta en estado pagable");
        }

        UUID empresaId = TenantContext.getEmpresaId();
        String moneda = normalizarMoneda(request != null ? request.moneda() : null);

        Pago pago = Pago.builder()
                .empresa(solicitud.getEmpresa())
                .codigo("PAGO-SOL-" + solicitudId + "-" + System.currentTimeMillis())
                .monto(solicitud.getPrecioTotal())
                .monedaRef(monedaRef(moneda))
                .metodoPago("STRIPE_CHECKOUT")
                .concepto("Roles adicionales: " + solicitud.getRolSolicitado() + " x" + solicitud.getCantidad())
                .solicitudRoles(solicitud)
                .estado(Pago.EstadoPago.PENDIENTE)
                .build();
        pagoRepository.save(pago);

        String defaultSuccess = "http://localhost:5173/users?rolesPayment=success";
        String defaultCancel = "http://localhost:5173/users?rolesPayment=cancel";
        Map<String, Object> checkout = controlPlaneClient.createStripeOneTimeCheckout(
                empresaId,
                solicitud.getPrecioTotal(),
                moneda,
                pago.getConcepto(),
                String.valueOf(pago.getId()),
                Map.of(
                        "solicitudRolesId", solicitud.getId(),
                        "rolSolicitado", solicitud.getRolSolicitado(),
                        "cantidad", solicitud.getCantidad()
                ),
                request != null && request.successUrl() != null ? request.successUrl() : defaultSuccess,
                request != null && request.cancelUrl() != null ? request.cancelUrl() : defaultCancel
        );

        String sessionId = String.valueOf(checkout.getOrDefault("stripeCheckoutSessionId", ""));
        if (StringUtils.hasText(sessionId) && !"null".equalsIgnoreCase(sessionId)) {
            pago.setReferenciaExterna(sessionId);
        }
        actualizarMontoLocalDesdeCheckout(pago, checkout, moneda);

        auditoriaService.registrar(TenantContext.getUsuarioId(), empresaId, "INICIAR_PAGO_STRIPE",
                "Inicio de pago Stripe por solicitud " + solicitudId + ": " + solicitud.getPrecioTotal(),
                httpRequest.getRemoteAddr(), null, "pago", null, null, null);

        log.info("[PAGO] Checkout Stripe creado para solicitud {}: {}",
                solicitudId, checkout.get("estado"));

        java.util.LinkedHashMap<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("pagoId", pago.getId());
        response.put("solicitudId", solicitudId);
        response.put("monto", pago.getMonto());
        response.put("moneda", pago.getMoneda());
        response.put("metodo", "STRIPE_CHECKOUT");
        response.put("estado", pago.getEstado());
        response.put("online", checkout.getOrDefault("online", false));
        response.put("checkoutUrl", checkout.getOrDefault("checkoutUrl", ""));
        response.put("stripeCheckoutSessionId", checkout.getOrDefault("stripeCheckoutSessionId", ""));
        response.put("mensaje", checkout.getOrDefault("mensaje", "Sesion de pago creada en Stripe Checkout."));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stripe-confirmar")
    @PreAuthorize("hasAuthority('USUARIOS_CREAR')")
    @Transactional
    public ResponseEntity<?> confirmarPagoStripe(@RequestBody ConfirmarStripeRequest request,
                                                  HttpServletRequest httpRequest) {
        if (request == null || request.sessionId() == null || request.sessionId().isBlank()) {
            throw new BusinessException("STRIPE_SESSION_REQUERIDA", "Debe indicar la sesion de Stripe");
        }
        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new BusinessException("EMPRESA_NO_IDENTIFICADA", "No se pudo identificar la empresa");
        }
        Map<String, Object> checkout = controlPlaneClient.getStripeCheckoutSession(request.sessionId());
        String estadoStripe = String.valueOf(checkout.getOrDefault("estado", "PENDIENTE"));

        Pago pago = pagoRepository.findByReferenciaExterna(request.sessionId())
                .orElseThrow(() -> new BusinessException("PAGO_NO_ENCONTRADO", "No se encontro pago local para la sesion Stripe"));
        if (!empresaId.equals(pago.getEmpresa().getId())) {
            throw new BusinessException("PAGO_EMPRESA_INVALIDA", "El pago no corresponde a la empresa autenticada");
        }
        if (pago.getSolicitudRoles() == null) {
            throw new BusinessException("PAGO_TIPO_INCORRECTO", "Este pago no corresponde a una solicitud de roles");
        }

        if ("CONFIRMADO".equals(estadoStripe)) {
            activarSolicitudPagada(pago);
            auditoriaService.registrar(TenantContext.getUsuarioId(), empresaId, "PAGO_STRIPE_CONFIRMADO",
                    "Pago Stripe confirmado para solicitud " + pago.getSolicitudRoles().getId(),
                    httpRequest.getRemoteAddr(), null, "pago", pago.getId(), null, null);
            return ResponseEntity.ok(Map.of(
                    "pagoId", pago.getId(),
                    "solicitudId", pago.getSolicitudRoles().getId(),
                    "estado", "PAGADO",
                    "mensaje", "Pago confirmado. Los roles ya estan disponibles para invitacion."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "pagoId", pago.getId(),
                "solicitudId", pago.getSolicitudRoles().getId(),
                "estado", estadoStripe,
                "mensaje", "Stripe aun no confirmo el pago."
        ));
    }

    private void activarSolicitudPagada(Pago pago) {
        SolicitudRoles solicitud = pago.getSolicitudRoles();
        if (!"PAGADA".equals(solicitud.getEstado())) {
            pago.setEstado(Pago.EstadoPago.PAGADO);
            pago.setFechaPago(OffsetDateTime.now());
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
        }
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
    public record PagoRequest(String successUrl, String cancelUrl, String moneda) {}
    public record ConfirmarStripeRequest(String sessionId) {}

    private String normalizarMoneda(Object value) {
        String moneda = value != null ? String.valueOf(value).trim().toUpperCase() : "USD";
        return switch (moneda) {
            case "USD", "PYG" -> moneda;
            default -> "USD";
        };
    }

    private Moneda monedaRef(String codigo) {
        return monedaRepository.findByCodigoIso(codigo).orElse(null);
    }

    private void actualizarMontoLocalDesdeCheckout(Pago pago, Map<String, Object> checkout, String monedaFallback) {
        Object monto = checkout.get("monto");
        if (monto != null) {
            pago.setMonto(decimalValue(monto));
        }
        String moneda = normalizarMoneda(checkout.getOrDefault("moneda", monedaFallback));
        pago.setMonedaRef(monedaRef(moneda));
        pagoRepository.save(pago);
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        return new BigDecimal(String.valueOf(value));
    }
}
