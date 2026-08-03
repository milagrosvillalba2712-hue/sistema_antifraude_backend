package com.antifraude.kyc;

import com.antifraude.dto.KycRequest;
import com.antifraude.dto.KycResponse;
import com.antifraude.exception.BusinessException;
import com.antifraude.external.*;
import com.antifraude.licensing.EmpresaRepository;
import com.antifraude.security.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class KycService {
    private final IdentificacionesClient identities;
    private final BcpSancionesClient sanctions;
    private final SepreladPepClient pep;
    private final ConsultaExternaRepository audits;
    private final EmpresaRepository companies;

    public KycService(IdentificacionesClient identities, BcpSancionesClient sanctions,
                      SepreladPepClient pep, ConsultaExternaRepository audits, EmpresaRepository companies) {
        this.identities=identities; this.sanctions=sanctions; this.pep=pep; this.audits=audits; this.companies=companies;
    }

    public KycResponse consultar(KycRequest request, UUID usuarioId) {
        String type = request.tipoConsulta() == null ? "IDENTIDAD" : request.tipoConsulta().toUpperCase(Locale.ROOT);
        try {
            ProviderResult<?> result = switch (type) {
                case "IDENTIDAD", "IDENTIFICACIONES" -> identities.consultar(request.identificadorDocumento());
                case "SANCIONES", "BCP" -> sanctions.consultar(request.identificadorDocumento());
                case "PEP", "PERSONA_EXPUESTA" -> pep.consultar(request.identificadorDocumento());
                default -> throw new BusinessException("KYC_TYPE_INVALID", "Tipo de consulta KYC no soportado");
            };
            saveAudit(request, result.provider(), result.correlationId(), result.statusHttp(), result.durationMs(),
                    result.attempts(), result.match(), "COMPLETADA", null);
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
                3, false, "ERROR", exception.category());
    }

    private void saveAudit(KycRequest request, String provider, String correlation, int status, long duration,
                           int attempts, boolean match, String state, String category) {
        UUID companyId = TenantContext.getEmpresaId();
        if (companyId == null) throw new BusinessException("TENANT_REQUIRED", "La consulta requiere una empresa activa");
        audits.save(ConsultaExterna.builder()
                .empresa(companies.getReferenceById(companyId)).tipoConsulta(request.tipoConsulta()).proveedor(provider)
                .documentoHash(hash(request.identificadorDocumento())).correlationId(correlation).statusHttp(status)
                .duracionMs(duration).intentos(attempts).resultado(match)
                .resultadoFuncional(match ? "COINCIDENCIA" : "SIN_COINCIDENCIA")
                .categoriaError(category).estado(state).build());
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
