package com.antifraude.kyc;

import com.antifraude.dto.KycRequest;
import com.antifraude.dto.KycResponse;
import com.antifraude.exception.BusinessException;
import com.antifraude.external.*;
import com.antifraude.licensing.ConsumoService;
import com.antifraude.licensing.EnforcementService;
import com.antifraude.security.tenant.TenantContext;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.UUID;

@Service
public class KycService {
    private final IdentificacionesClient identities;
    private final BcpSancionesClient sanctions;
    private final SepreladPepClient pep;
    private final ExternalAuditService audit;
    private final EnforcementService enforcementService;
    private final ConsumoService consumoService;

    public KycService(IdentificacionesClient identities, BcpSancionesClient sanctions,
                      SepreladPepClient pep, ExternalAuditService audit,
                      EnforcementService enforcementService, ConsumoService consumoService) {
        this.identities=identities; this.sanctions=sanctions; this.pep=pep; this.audit=audit;
        this.enforcementService = enforcementService;
        this.consumoService = consumoService;
    }

    public KycResponse consultar(KycRequest request, UUID usuarioId) {
        String type = request.tipoConsulta() == null ? "IDENTIDAD" : request.tipoConsulta().toUpperCase(Locale.ROOT);
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
}
