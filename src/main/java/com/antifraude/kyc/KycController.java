package com.antifraude.kyc;

import com.antifraude.dto.KycRequest;
import com.antifraude.dto.KycResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    private static final Logger log = LoggerFactory.getLogger(KycController.class);

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/consultar")
    public ResponseEntity<KycResponse> consultar(@Valid @RequestBody KycRequest request,
                                                  Authentication auth, HttpServletRequest httpRequest) {
        log.info("[KYC] Consulta externa solicitada - Tipo: {} - Usuario: {}",
                request.tipoConsulta(), auth.getName());
        KycResponse response = kycService.consultar(request, null);
        log.info("[KYC] Consulta completada - Tipo: {} - Resultado: {}",
                request.tipoConsulta(), response.resultado());
        return ResponseEntity.ok(response);
    }
}
