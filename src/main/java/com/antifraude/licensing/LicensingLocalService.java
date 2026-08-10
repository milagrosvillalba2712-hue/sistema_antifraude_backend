package com.antifraude.licensing;

import com.antifraude.exception.BusinessException;
import com.antifraude.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Ciclo de vida de instalaciones on-premise: registrar instalacion, activar,
 * emitir/renovar lease firmado, heartbeat, validar vigencia, revocar y
 * registrar eventos auditables.
 *
 * La politica de vencimiento / gracia y el bloqueo progresivo viven en
 * {@link LicensingValidationService}; aqui se ejecutan las mutaciones.
 */
@Service
public class LicensingLocalService {

    private static final Logger log = LoggerFactory.getLogger(LicensingLocalService.class);

    public static final String INSTALACION_PENDIENTE = "PENDIENTE";
    public static final String INSTALACION_ACTIVA = "ACTIVA";
    public static final String INSTALACION_REVOCADA = "REVOCADA";
    public static final String LICENCIA_ACTIVA = "ACTIVA";
    public static final String LICENCIA_REVOCADA = "REVOCADA";

    private final InstalacionLocalRepository instalacionRepository;
    private final LicenciaLocalRepository licenciaRepository;
    private final EventoLicenciaLocalRepository eventoRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final EmpresaRepository empresaRepository;
    private final LicenseCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public LicensingLocalService(InstalacionLocalRepository instalacionRepository,
                                 LicenciaLocalRepository licenciaRepository,
                                 EventoLicenciaLocalRepository eventoRepository,
                                 SuscripcionRepository suscripcionRepository,
                                 EmpresaRepository empresaRepository,
                                 LicenseCryptoService cryptoService,
                                 ObjectMapper objectMapper) {
        this.instalacionRepository = instalacionRepository;
        this.licenciaRepository = licenciaRepository;
        this.eventoRepository = eventoRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.empresaRepository = empresaRepository;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InstalacionLocal registrarInstalacion(UUID empresaId, String identidadMaquina, String versionProducto) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", empresaId));
        String fingerprintHash = cryptoService.fingerprint(identidadMaquina);
        if (fingerprintHash == null) {
            throw new BusinessException("INSTALACION_FINGERPRINT_INVALIDO", "Identidad de maquina vacia");
        }
        Optional<InstalacionLocal> existente = instalacionRepository.findByFingerprintHash(fingerprintHash);
        if (existente.isPresent()) {
            InstalacionLocal previa = existente.get();
            if (INSTALACION_ACTIVA.equals(previa.getEstado()) || INSTALACION_PENDIENTE.equals(previa.getEstado())) {
                registrarEvento(previa, "REGISTRO_INSTALACION", "DUPLICADO",
                        Map.of("motivo", "identidad ya registrada", "estado", previa.getEstado()));
                return previa;
            }
        }

        InstalacionLocal instalacion = InstalacionLocal.builder()
                .empresa(empresa)
                .identificadorInstalacion("INST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .fingerprintHash(fingerprintHash)
                .clavePublicaPem("-----BEGIN PUBLIC KEY-----\nPENDIENTE-ACTIVACION\n-----END PUBLIC KEY-----")
                .estado(INSTALACION_PENDIENTE)
                .versionProducto(versionProducto)
                .clonDetectado(false)
                .build();
        instalacionRepository.save(instalacion);
        registrarEvento(instalacion, "REGISTRO_INSTALACION", "OK",
                Map.of("versionProducto", String.valueOf(versionProducto)));
        log.info("[LICENCIA] Instalacion registrada - {} - fingerprint: {}",
                instalacion.getIdentificadorInstalacion(), fingerprintHash);
        return instalacion;
    }

    @Transactional
    public LicenciaLocal activar(UUID instalacionId, Long suscripcionId) {
        InstalacionLocal instalacion = instalacionRepository.findById(instalacionId)
                .orElseThrow(() -> new ResourceNotFoundException("InstalacionLocal", "id", instalacionId));
        if (INSTALACION_REVOCADA.equals(instalacion.getEstado())) {
            throw new BusinessException("INSTALACION_REVOCADA", "La instalacion fue revocada y no puede reactivarse");
        }

        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion", "id", suscripcionId));
        PlanLicencia plan = suscripcion.getPlanLicencia();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instalacionId", instalacionId);
        payload.put("suscripcionReferencia", suscripcion.getId());
        payload.put("planCodigo", plan.getCodigo());
        payload.put("planVersion", planVersion(plan));
        payload.put("suscripcionVenceEn", suscripcion.getFechaFin().toString());
        payload.put("modulos", modulosDe(plan));
        payload.put("limites", limitesDe(plan));

        String leasePayload = safeJson(payload);
        String firma = cryptoService.firmar(leasePayload, instalacion.getFingerprintHash());

        LicenciaLocal licencia = LicenciaLocal.builder()
                .instalacion(instalacion)
                .suscripcionReferencia(String.valueOf(suscripcion.getId()))
                .planCodigo(plan.getCodigo())
                .planVersion(planVersion(plan))
                .estado(LICENCIA_ACTIVA)
                .emitidaEn(OffsetDateTime.now())
                .venceEn(suscripcion.getFechaFin().atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime())
                .diasGracia(diasGraciaDe(plan))
                .modulosJson(safeJson(modulosDe(plan)))
                .limitesJson(safeJson(limitesDe(plan)))
                .leasePayload(leasePayload)
                .leaseFirma(firma)
                .kidFirma(LicenseCryptoService.KID_FIRMA)
                .ultimaValidacionEn(OffsetDateTime.now())
                .build();
        licencia = licenciaRepository.save(licencia);

        instalacion.setEstado(INSTALACION_ACTIVA);
        instalacion.setActivadaEn(OffsetDateTime.now());
        instalacion.setUltimoHeartbeatEn(OffsetDateTime.now());
        instalacionRepository.save(instalacion);

        registrarEvento(instalacion, "ACTIVACION_INSTALACION", "OK",
                Map.of("licenciaId", licencia.getId(), "planCodigo", plan.getCodigo(),
                        "venceEn", licencia.getVenceEn().toString(), "kid", LicenseCryptoService.KID_FIRMA));
        log.info("[LICENCIA] Instalacion activada - {} - plan {} - vence {}",
                instalacion.getIdentificadorInstalacion(), plan.getCodigo(), licencia.getVenceEn());
        return licencia;
    }

    @Transactional
    public InstalacionLocal heartbeat(UUID instalacionId) {
        InstalacionLocal instalacion = instalacionRepository.findById(instalacionId)
                .orElseThrow(() -> new ResourceNotFoundException("InstalacionLocal", "id", instalacionId));
        instalacion.setUltimoHeartbeatEn(OffsetDateTime.now());
        if (INSTALACION_ACTIVA.equals(instalacion.getEstado())) {
            instalacionRepository.save(instalacion);
        }
        registrarEvento(instalacion, "HEARTBEAT", "OK", Map.of());
        return instalacion;
    }

    @Transactional
    public void revocar(UUID instalacionId, String motivo) {
        InstalacionLocal instalacion = instalacionRepository.findById(instalacionId)
                .orElseThrow(() -> new ResourceNotFoundException("InstalacionLocal", "id", instalacionId));
        Optional<LicenciaLocal> licenciaOpt = licenciaRepository.findTopByInstalacionIdOrderByEmitidaEnDesc(instalacionId);
        instalacion.setEstado(INSTALACION_REVOCADA);
        instalacionRepository.save(instalacion);
        licenciaOpt.ifPresent(licencia -> {
            if (!LICENCIA_REVOCADA.equals(licencia.getEstado())) {
                licencia.setEstado(LICENCIA_REVOCADA);
                licenciaRepository.save(licencia);
            }
        });
        registrarEvento(instalacion, "REVOCACION_INSTALACION", "REVOCADA",
                Map.of("motivo", String.valueOf(motivo)));
        log.warn("[LICENCIA] Instalacion revocada - {}", instalacion.getIdentificadorInstalacion());
    }

    @Transactional(readOnly = true)
    public InstalacionLocal obtener(UUID instalacionId) {
        return instalacionRepository.findById(instalacionId)
                .orElseThrow(() -> new ResourceNotFoundException("InstalacionLocal", "id", instalacionId));
    }

    public void registrarEvento(InstalacionLocal instalacion, String tipoEvento, String resultado,
                                Map<String, Object> detalle) {
        EventoLicenciaLocal evento = EventoLicenciaLocal.builder()
                .instalacionId(instalacion.getId())
                .tipoEvento(tipoEvento)
                .resultado(resultado)
                .correlationId(cryptoService.generarCorrelationId(instalacion.getId()))
                .detalleSanitizadoJson(detalle)
                .build();
        eventoRepository.save(evento);
    }

    private Integer planVersion(PlanLicencia plan) {
        return plan.getId().intValue();
    }

    private Integer diasGraciaDe(PlanLicencia plan) {
        return switch (plan.getCodigo()) {
            case "BASICO" -> 7;
            case "ESTANDAR" -> 15;
            default -> 30;
        };
    }

    private Object modulosDe(PlanLicencia plan) {
        if (plan.getModulosIncluidosJson() == null || plan.getModulosIncluidosJson().isBlank()) {
            return java.util.List.of();
        }
        try {
            return objectMapper.readValue(plan.getModulosIncluidosJson(), Object.class);
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    private Map<String, Object> limitesDe(PlanLicencia plan) {
        Map<String, Object> limites = new LinkedHashMap<>();
        limites.put("usuarios", plan.getLimiteUsuarios());
        limites.put("transaccionesMensuales", plan.getLimiteTransaccionesMensuales());
        limites.put("consultasKycMensuales", plan.getLimiteConsultasKycMensuales());
        limites.put("reportesMensuales", plan.getLimiteReportesMensuales());
        return limites;
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar payload de licencia", e);
        }
    }
}