package com.antifraude.licensing;

import com.antifraude.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints locales de gestion de licencia on-premise (fuera del
 * LicencingController SaaS read-only). Operan bajo la clave de instalacion
 * de la empresa y reflejan el ciclo de vida local de la licencia.
 */
@RestController
@RequestMapping("/api/licensing-local")
public class LicensingLocalController {

    private final LicensingLocalService licensingService;
    private final LicensingValidationService validationService;
    private final LicensingControlPlaneClient controlPlaneClient;
    private final EventoLicenciaLocalRepository eventoRepository;
    private final LicenciaLocalRepository licenciaRepository;

    public LicensingLocalController(LicensingLocalService licensingService,
                                    LicensingValidationService validationService,
                                    LicensingControlPlaneClient controlPlaneClient,
                                    EventoLicenciaLocalRepository eventoRepository,
                                    LicenciaLocalRepository licenciaRepository) {
        this.licensingService = licensingService;
        this.validationService = validationService;
        this.controlPlaneClient = controlPlaneClient;
        this.eventoRepository = eventoRepository;
        this.licenciaRepository = licenciaRepository;
    }

    @PostMapping("/install")
    public ResponseEntity<Map<String, Object>> instalar(@RequestBody SolicitudInstalacion solicitud,
                                                        HttpServletRequest request) {
        InstalacionLocal instalacion = licensingService.registrarInstalacion(
                solicitud.empresaId(), solicitud.identidadMaquina(), solicitud.versionProducto());
        return ResponseEntity.ok(Map.of(
                "instalacionId", instalacion.getId(),
                "identificadorInstalacion", instalacion.getIdentificadorInstalacion(),
                "estado", instalacion.getEstado(),
                "fingerprintHashEnmascarado", enmascarar(instalacion.getFingerprintHash()),
                "ip", com.antifraude.config.ClientIpResolver.resolve(request),
                "mensaje", "Instalacion registrada. Active la licencia para emitir el lease."
        ));
    }

    @PostMapping("/activate")
    public ResponseEntity<Map<String, Object>> activar(@RequestBody SolicitudActivacion solicitud) {
        LicenciaLocal licencia = licensingService.activar(solicitud.instalacionId(), solicitud.suscripcionId());
        return ResponseEntity.ok(Map.of(
                "licenciaId", licencia.getId(),
                "estado", licencia.getEstado(),
                "planCodigo", licencia.getPlanCodigo(),
                "planVersion", licencia.getPlanVersion(),
                "venceEn", licencia.getVenceEn().toString(),
                "diasGracia", licencia.getDiasGracia(),
                "kid", licencia.getKidFirma(),
                "leaseFirma", enmascarar(licencia.getLeaseFirma()),
                "mensaje", "Licencia activada y lease firmado emitido."
        ));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(@RequestBody SolicitudInstalacionId solicitud) {
        InstalacionLocal instalacion = licensingService.heartbeat(solicitud.instalacionId());
        return ResponseEntity.ok(Map.of(
                "instalacionId", instalacion.getId(),
                "identificadorInstalacion", instalacion.getIdentificadorInstalacion(),
                "ultimoHeartbeatEn", String.valueOf(instalacion.getUltimoHeartbeatEn()),
                "estado", instalacion.getEstado()
        ));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validar(@RequestBody SolicitudInstalacionId solicitud,
                                                       boolean online) {
        LicensingValidationService.ResultadoValidacion resultado =
                validationService.validar(solicitud.instalacionId(), online);
        return ResponseEntity.ok(resultadoADto(resultado));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validarPorQuery(@RequestParam UUID instalacionId,
                                                               @RequestParam(defaultValue = "true") boolean online) {
        LicensingValidationService.ResultadoValidacion resultado =
                validationService.validar(instalacionId, online);
        return ResponseEntity.ok(resultadoADto(resultado));
    }

    @GetMapping("/status/{instalacionId}")
    public ResponseEntity<Map<String, Object>> estado(@PathVariable UUID instalacionId) {
        InstalacionLocal instalacion = licensingService.obtener(instalacionId);
        LicenciaLocal licencia = licenciaRepository.findTopByInstalacionIdOrderByEmitidaEnDesc(instalacionId)
                .orElse(null);
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("instalacionId", instalacion.getId());
        dto.put("identificadorInstalacion", instalacion.getIdentificadorInstalacion());
        dto.put("estadoInstalacion", instalacion.getEstado());
        dto.put("clonDetectado", instalacion.isClonDetectado());
        dto.put("activadaEn", String.valueOf(instalacion.getActivadaEn()));
        dto.put("ultimoHeartbeatEn", String.valueOf(instalacion.getUltimoHeartbeatEn()));
        if (licencia != null) {
            dto.put("licencia", Map.of(
                    "licenciaId", licencia.getId(),
                    "estado", licencia.getEstado(),
                    "planCodigo", licencia.getPlanCodigo(),
                    "venceEn", licencia.getVenceEn().toString(),
                    "diasGracia", licencia.getDiasGracia(),
                    "ultimaValidacionEn", String.valueOf(licencia.getUltimaValidacionEn()),
                    "kid", licencia.getKidFirma()
            ));
        }
        dto.put("controlPlane", Map.of("habilitado", controlPlaneClient.habilitado()));
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/events/{instalacionId}")
    public ResponseEntity<Object> eventos(@PathVariable UUID instalacionId) {
        return ResponseEntity.ok(eventoRepository.findTop20ByInstalacionIdOrderByFechaEventoDesc(instalacionId));
    }

    private Map<String, Object> resultadoADto(LicensingValidationService.ResultadoValidacion r) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("instalacionId", r.instalacionId());
        dto.put("licenciaId", r.licenciaId());
        dto.put("modo", r.modo().name());
        dto.put("motivo", r.motivo());
        dto.put("detalle", r.detalle());
        dto.put("firmaValida", r.firmaValida());
        dto.put("online", r.online());
        dto.put("venceEn", String.valueOf(r.venceEn()));
        return dto;
    }

    private String enmascarar(String valor) {
        if (valor == null || valor.length() <= 8) {
            return "***";
        }
        return valor.substring(0, 6) + "..." + valor.substring(valor.length() - 4);
    }

    public record SolicitudInstalacion(UUID empresaId, String identidadMaquina, String versionProducto) {
    }

    public record SolicitudActivacion(UUID instalacionId, Long suscripcionId) {
    }

    public record SolicitudInstalacionId(UUID instalacionId) {
    }
}