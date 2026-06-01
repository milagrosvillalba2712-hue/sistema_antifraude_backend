package com.antifraude.kyc;

import com.antifraude.dto.KycRequest;
import com.antifraude.dto.KycResponse;
import com.antifraude.external.ConsultaExterna;
import com.antifraude.external.ConsultaExternaRepository;
import com.antifraude.external.ExternalApiClient;
import com.antifraude.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class KycService {

    private static final Logger log = LoggerFactory.getLogger(KycService.class);

    private final ExternalApiClient externalApiClient;
    private final ConsultaExternaRepository consultaExternaRepository;

    public KycService(ExternalApiClient externalApiClient, ConsultaExternaRepository consultaExternaRepository) {
        this.externalApiClient = externalApiClient;
        this.consultaExternaRepository = consultaExternaRepository;
    }

    public KycResponse consultar(KycRequest request, Long usuarioId) {
        log.info("[KYC] Consultando {} - Documento: {} - UsuarioId: {}",
                request.tipoConsulta(), request.identificadorDocumento(), usuarioId);
        try {
            Map<String, Object> resultado = externalApiClient.consultar(request.tipoConsulta(), request.identificadorDocumento());
            Boolean positivo = (Boolean) resultado.getOrDefault("coincidencia", false);

            ConsultaExterna consulta = ConsultaExterna.builder()
                    .identificadorDocumento(request.identificadorDocumento())
                    .tipoConsulta(request.tipoConsulta())
                    .resultado(positivo)
                    .build();
            consultaExternaRepository.save(consulta);

            String mensaje = positivo
                    ? "Se encontro una coincidencia en " + request.tipoConsulta()
                    : "Sin coincidencias en " + request.tipoConsulta();

            log.info("[KYC] Consulta completada - Documento: {} - Tipo: {} - Coincidencia: {}",
                    request.identificadorDocumento(), request.tipoConsulta(), positivo);

            return new KycResponse(request.identificadorDocumento(), request.tipoConsulta(), positivo, mensaje);
        } catch (Exception e) {
            log.error("[KYC] Error en consulta externa - Documento: {} - Tipo: {} - Error: {}",
                    request.identificadorDocumento(), request.tipoConsulta(), e.getMessage());
            throw new BusinessException("EXTERNAL_API_ERROR",
                    "Error al consultar servicio externo: " + e.getMessage());
        }
    }
}
