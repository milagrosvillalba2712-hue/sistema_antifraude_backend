package com.antifraude.kyc;

import com.antifraude.dto.KycRequest;
import com.antifraude.dto.KycResponse;
import com.antifraude.exception.BusinessException;
import com.antifraude.external.*;
import com.antifraude.licensing.ConsumoService;
import com.antifraude.licensing.EnforcementService;
import com.antifraude.common.entity.Pais;
import com.antifraude.common.entity.TipoDocumento;
import com.antifraude.common.repository.PaisRepository;
import com.antifraude.common.repository.TipoDocumentoRepository;
import com.antifraude.security.tenant.TenantContext;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class KycService {
    private final IdentificacionesClient identities;
    private final BcpSancionesClient sanctions;
    private final SepreladPepClient pep;
    private final ExternalAuditService audit;
    private final EnforcementService enforcementService;
    private final ConsumoService consumoService;
    private final PaisRepository paisRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;

    public KycService(IdentificacionesClient identities, BcpSancionesClient sanctions,
                      SepreladPepClient pep, ExternalAuditService audit,
                      EnforcementService enforcementService, ConsumoService consumoService,
                      PaisRepository paisRepository, TipoDocumentoRepository tipoDocumentoRepository) {
        this.identities=identities; this.sanctions=sanctions; this.pep=pep; this.audit=audit;
        this.enforcementService = enforcementService;
        this.consumoService = consumoService;
        this.paisRepository = paisRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
    }

    public KycResponse consultar(KycRequest request, UUID usuarioId) {
        String type = request.tipoConsulta() == null ? "IDENTIDAD" : request.tipoConsulta().toUpperCase(Locale.ROOT);
        validarDocumento(request);
        UUID empresaId = TenantContext.getEmpresaId();
        enforcementService.verificarSuscripcionVigente(empresaId);
        enforcementService.verificarModulo(empresaId, "KYC");
        enforcementService.verificarLimiteKyc(empresaId);
        try {
            ProviderResult<?> result = switch (type) {
                case "IDENTIDAD", "IDENTIFICACIONES" -> identities.consultar(request.identificadorDocumento());
                case "SANCIONES", "BCP" -> sanctions.consultar(request.identificadorDocumento());
                case "PEP", "PERSONA_EXPUESTA" -> pep.consultar(request.identificadorDocumento());
                default -> throw new BusinessException("KYC_TYPE_INVALID", "Tipo de consulta KYC no soportado");
            };
            saveAudit(request, result.provider(), result.correlationId(), result.statusHttp(), result.durationMs(),
                    result.attempts(), result.match(), "COMPLETADA", null);
            consumoService.registrarConsultaKyc(empresaId);
            return new KycResponse("***", type, result.match(),
                    result.match() ? "Se encontró una coincidencia" : "Sin coincidencias");
        } catch (NonRetryableExternalException exception) {
            saveFailure(request, exception);
            throw new BusinessException("EXTERNAL_API_REJECTED", "El proveedor rechazó la consulta");
        } catch (ExternalProviderException exception) {
            saveFailure(request, exception);
            throw new BusinessException("EXTERNAL_API_ERROR", "El proveedor externo no está disponible");
        }
    }

    private void saveFailure(KycRequest request, ExternalProviderException exception) {
        saveAudit(request, exception.provider(), exception.correlationId(), exception.statusHttp(), exception.durationMs(),
                exception.attempts(), false, "ERROR", exception.category());
    }

    private void saveAudit(KycRequest request, String provider, String correlation, int status, long duration,
                           int attempts, boolean match, String state, String category) {
        audit.record(TenantContext.getEmpresaId(), request.identificadorDocumento(), request.tipoConsulta(), provider,
                correlation, status, duration, attempts, match, state, category);
    }

    private void validarDocumento(KycRequest request) {
        Pais pais = paisRepository.findByCodigoIso(request.paisEmisorDocumento().trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException("DOCUMENT_COUNTRY_INVALID", "Pais emisor de documento invalido"));
        TipoDocumento tipoDocumento = tipoDocumentoRepository.findByCodigo(request.tipoDocumento().trim().toUpperCase(Locale.ROOT))
                .or(() -> tipoDocumentoRepository.findByCodigoTecnico(request.tipoDocumento().trim().toUpperCase(Locale.ROOT)))
                .orElseThrow(() -> new BusinessException("DOCUMENT_TYPE_INVALID", "Tipo de documento invalido"));
        if (tipoDocumento.getPaisRelacion() != null
                && !tipoDocumento.getPaisRelacion().getCodigoIso().equalsIgnoreCase(pais.getCodigoIso())) {
            throw new BusinessException("DOCUMENT_TYPE_COUNTRY_MISMATCH", "El tipo de documento no corresponde al pais indicado");
        }
        if (tipoDocumento.getFormatoRegex() != null && !tipoDocumento.getFormatoRegex().isBlank()
                && !Pattern.matches(tipoDocumento.getFormatoRegex(), request.identificadorDocumento().trim())) {
            throw new BusinessException("DOCUMENT_FORMAT_INVALID", "El documento no cumple el formato esperado");
        }
    }
}
