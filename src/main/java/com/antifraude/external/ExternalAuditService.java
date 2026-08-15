package com.antifraude.external;

import com.antifraude.exception.BusinessException;
import com.antifraude.licensing.EmpresaRepository;
import com.antifraude.observability.ApiEventoService;
import com.antifraude.security.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class ExternalAuditService {
    private final ConsultaExternaRepository audits;
    private final EmpresaRepository companies;
    private final ApiEventoService apiEventoService;

    public ExternalAuditService(ConsultaExternaRepository audits, EmpresaRepository companies,
                                ApiEventoService apiEventoService) {
        this.audits = audits; this.companies = companies; this.apiEventoService = apiEventoService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID companyId, String document, String type, String provider, String correlation,
                       int status, long duration, int attempts, boolean match, String state, String category) {
        if (companyId == null) throw new BusinessException("TENANT_REQUIRED", "La consulta requiere una empresa activa");
        audits.save(ConsultaExterna.builder().empresa(companies.getReferenceById(companyId)).tipoConsulta(type)
                .proveedor(provider).documentoHash(hash(document)).correlationId(correlation).statusHttp(status)
                .duracionMs(duration).intentos(attempts).resultado(match)
                .resultadoFuncional(match ? "COINCIDENCIA" : "SIN_COINCIDENCIA")
                .categoriaError(category).estado(state).build());
        boolean exitosoTecnico = status >= 200 && status < 400 && category == null;
        apiEventoService.registrarExterna(companyId, TenantContext.getUsuarioId(), provider, type, status, duration,
                correlation, category, state, exitosoTecnico, category, "consultas_externas", correlation);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
