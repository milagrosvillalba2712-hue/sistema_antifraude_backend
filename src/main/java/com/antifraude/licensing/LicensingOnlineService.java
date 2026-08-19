package com.antifraude.licensing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Flujo online de validacion y renovacion del lease firmado (RS256) contra el
 * Control Plane. Reutilizable por el endpoint manual de Admin Empresa y por
 * los jobs de licenciamiento programados (Fase 2). No depende del contexto de
 * request: opera sobre la instalacion que se le entrega.
 */
@Service
public class LicensingOnlineService {

    private final LicensingControlPlaneClient controlPlaneClient;
    private final LicensingValidationService validationService;
    private final LicenciaLocalRepository licenciaRepository;
    private final InstalacionLocalRepository instalacionRepository;
    private final EventoLicenciaLocalRepository eventoRepository;
    private final ObjectMapper objectMapper;

    public LicensingOnlineService(LicensingControlPlaneClient controlPlaneClient,
                                  LicensingValidationService validationService,
                                  LicenciaLocalRepository licenciaRepository,
                                  InstalacionLocalRepository instalacionRepository,
                                  EventoLicenciaLocalRepository eventoRepository,
                                  ObjectMapper objectMapper) {
        this.controlPlaneClient = controlPlaneClient;
        this.validationService = validationService;
        this.licenciaRepository = licenciaRepository;
        this.instalacionRepository = instalacionRepository;
        this.eventoRepository = eventoRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Valida el lease contra el Control Plane; si el lease devuelto permite
     * renovar (token + JWKS online), renueva la licencia local firmada y luego
     * aplica la politica local de vigencia/gracia.
     */
    @Transactional
    public ResultadoValidacionOnline validarYRenovar(InstalacionLocal instalacion) {
        Map<String, Object> controlPlane = controlPlaneClient.validarLease(instalacion.getId(),
                instalacion.getFingerprintHash());
        boolean online = Boolean.TRUE.equals(controlPlane.get("online"));
        if (online && controlPlane.get("leaseToken") != null) {
            renovarLeaseFirmado(instalacion, controlPlane, controlPlaneClient.jwks());
        }
        LicensingValidationService.ResultadoValidacion resultado = validationService.validar(instalacion.getId(), online);
        return new ResultadoValidacionOnline(resultado.instalacionId(), resultado.licenciaId(), resultado.modo(),
                resultado.motivo(), resultado.detalle(), resultado.firmaValida(), resultado.online(),
                resultado.venceEn(), controlPlane);
    }

    private void renovarLeaseFirmado(InstalacionLocal instalacion, Map<String, Object> controlPlane,
                                     Map<String, Object> jwks) {
        String leaseToken = String.valueOf(controlPlane.get("leaseToken"));
        String[] parts = leaseToken.split("\\.");
        if (parts.length != 3 || !Boolean.TRUE.equals(jwks.get("online"))) {
            return;
        }
        LicenciaLocal licencia = licenciaRepository.findTopByInstalacionIdOrderByEmitidaEnDesc(instalacion.getId())
                .orElse(null);
        if (licencia == null) {
            licencia = LicenciaLocal.builder()
                    .instalacion(instalacion)
                    .suscripcionReferencia("CONTROL_PLANE")
                    .planVersion(1)
                    .emitidaEn(OffsetDateTime.now())
                    .estado(LicensingLocalService.LICENCIA_ACTIVA)
                    .modulosJson("[]")
                    .limitesJson("{}")
                    .build();
        }
        OffsetDateTime venceEn = OffsetDateTime.parse(String.valueOf(controlPlane.get("vence")));
        OffsetDateTime graceUntil = OffsetDateTime.parse(String.valueOf(controlPlane.get("graceUntil")));
        @SuppressWarnings("unchecked")
        Map<String, Object> leasePayload = controlPlane.get("leasePayload") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();

        licencia.setPlanCodigo(String.valueOf(controlPlane.getOrDefault("plan", licencia.getPlanCodigo())));
        licencia.setEstado(LicensingLocalService.LICENCIA_ACTIVA);
        licencia.setEmitidaEn(OffsetDateTime.now());
        licencia.setVenceEn(venceEn);
        licencia.setDiasGracia((int) Math.max(0, Duration.between(venceEn, graceUntil).toDays()));
        licencia.setModulosJson(safeJson(leasePayload.getOrDefault("modules", List.of())));
        licencia.setLimitesJson(safeJson(mapOf(
                "usuarios", leasePayload.get("maxUsers"),
                "transaccionesMensuales", leasePayload.get("maxTransactionsMonth"),
                "consultasKycMensuales", leasePayload.get("maxKycMonth"),
                "reportesMensuales", leasePayload.get("maxReportsMonth"),
                "reglas", leasePayload.get("maxRules")
        )));
        licencia.setLeasePayload(parts[0] + "." + parts[1]);
        licencia.setLeaseFirma(parts[2]);
        licencia.setKidFirma(String.valueOf(controlPlane.get("kid")));
        licencia.setUltimaValidacionEn(OffsetDateTime.now());
        licenciaRepository.save(licencia);

        instalacion.setClavePublicaPem(safeJson(jwks));
        instalacion.setUltimoHeartbeatEn(OffsetDateTime.now());
        instalacionRepository.save(instalacion);

        eventoRepository.save(EventoLicenciaLocal.builder()
                .instalacionId(instalacion.getId())
                .licenciaId(licencia.getId())
                .tipoEvento("RENOVACION_CONTROL_PLANE_RS256")
                .resultado("OK")
                .correlationId(LicenseCryptoService.generarCorrelationId(instalacion.getId()))
                .detalleSanitizadoJson(mapOf("kid", licencia.getKidFirma(), "venceEn", licencia.getVenceEn()))
                .build());
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    public record ResultadoValidacionOnline(UUID instalacionId, UUID licenciaId, LicensingValidationService.Modo modo,
                                            String motivo, String detalle, boolean firmaValida, boolean online,
                                            OffsetDateTime venceEn, Map<String, Object> controlPlane) {
    }
}