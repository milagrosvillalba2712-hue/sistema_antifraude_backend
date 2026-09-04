package com.antifraude.transactions;

import com.antifraude.drools.fact.CoincidenciaListaFact;
import com.antifraude.dto.TransaccionEvaluacionResponse;
import com.antifraude.dto.TransaccionRequest;
import com.antifraude.dto.TransaccionResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private static final Logger log = LoggerFactory.getLogger(TransaccionController.class);

    private final TransaccionService transaccionService;
    private final ObjectMapper objectMapper;

    public TransaccionController(TransaccionService transaccionService, ObjectMapper objectMapper) {
        this.transaccionService = transaccionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<TransaccionEvaluacionResponse> crear(@Valid @RequestBody TransaccionRequest request,
                                                               HttpServletRequest httpRequest) {
        log.info("[TX] POST /api/transacciones - UUID: {} - IP: {}", request.transactionUuid(), httpRequest.getRemoteAddr());
        Transaccion transaccion = transaccionService.crearDesdeRequest(request);
        transaccion = transaccionService.procesarTransaccion(transaccion);
        log.info("[TX] Transaccion creada y procesada - ID: {} - Estado: {} - Score: {}",
                transaccion.getId(), transaccion.getEstado(), transaccion.getScoreRiesgo());
        return ResponseEntity.status(HttpStatus.CREATED).body(toEvaluacionResponse(transaccion));
    }

    @GetMapping
    public ResponseEntity<List<TransaccionResponse>> listar() {
        log.info("[TX] GET /api/transacciones");
        List<TransaccionResponse> response = transaccionService.listarTodas().stream().map(this::toResponse).toList();
        log.info("[TX] Retornando {} transacciones", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransaccionResponse> buscar(@PathVariable Long id) {
        log.info("[TX] GET /api/transacciones/{}", id);
        return ResponseEntity.ok(toResponse(transaccionService.buscarPorId(id)));
    }

    @GetMapping("/documento/{documento}")
    public ResponseEntity<List<TransaccionResponse>> buscarPorDocumento(@PathVariable String documento) {
        log.info("[TX] GET /api/transacciones/documento/{}", documento);
        return ResponseEntity.ok(transaccionService.buscarPorDocumento(documento).stream().map(this::toResponse).toList());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<TransaccionResponse>> buscarPorEstado(@PathVariable String estado) {
        log.info("[TX] GET /api/transacciones/estado/{}", estado);
        return ResponseEntity.ok(transaccionService.buscarPorEstado(estado).stream().map(this::toResponse).toList());
    }

    private TransaccionEvaluacionResponse toEvaluacionResponse(Transaccion t) {
        BigDecimal score = t.getScoreRiesgo() != null ? t.getScoreRiesgo() : BigDecimal.ZERO;
        return new TransaccionEvaluacionResponse(
                t.getId(),
                t.getTransactionUuid() != null ? t.getTransactionUuid().toString() : null,
                t.getCodigo(),
                t.getTipoTransaccion(),
                t.getEstado(),
                t.getEstadoEvaluacion() != null ? t.getEstadoEvaluacion().name() : null,
                score,
                t.getNivelRiesgo() != null ? t.getNivelRiesgo().getCodigo() : null,
                "OBSERVADA".equalsIgnoreCase(t.getEstado())
                        || score.compareTo(new BigDecimal("70")) >= 0,
                parseReglas(t),
                parseScreening(t),
                t.getFechaTransaccion(),
                t.getFechaProcesamiento());
    }

    private List<TransaccionEvaluacionResponse.ReglaDisparadaDto> parseReglas(Transaccion t) {
        if (t.getReglasDisparadasJson() == null || t.getReglasDisparadasJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(t.getReglasDisparadasJson(),
                    new TypeReference<List<TransaccionEvaluacionResponse.ReglaDisparadaDto>>() {});
        } catch (Exception e) {
            log.warn("[TX] No se pudo deserializar reglasDisparadasJson: {}", e.getMessage());
            return List.of();
        }
    }

    private List<TransaccionEvaluacionResponse.CoincidenciaDto> parseScreening(Transaccion t) {
        if (t.getScreeningResultJson() == null || t.getScreeningResultJson().isBlank()) {
            return List.of();
        }
        try {
            List<CoincidenciaListaFact> coincidencias = objectMapper.readValue(t.getScreeningResultJson(),
                    new TypeReference<List<CoincidenciaListaFact>>() {});
            return coincidencias.stream().map(c -> new TransaccionEvaluacionResponse.CoincidenciaDto(
                    c.getNombreSujeto(),
                    c.getCampoEvaluado(),
                    c.getParteTransaccion(),
                    c.getScoreMatch(),
                    c.getSeveridad(),
                    c.getDescripcion())).toList();
        } catch (Exception e) {
            log.warn("[TX] No se pudo deserializar screeningResultJson: {}", e.getMessage());
            return List.of();
        }
    }

    private TransaccionResponse toResponse(Transaccion t) {
        return new TransaccionResponse(
                t.getId(),
                t.getTransactionUuid().toString(),
                t.getCodigo(),
                t.getIdentificadorDocumentoEnmascarado(),
                t.getIdentificadorDocumentoEnmascarado(),
                t.getTipoDocumentoRemitente() != null ? t.getTipoDocumentoRemitente().getCodigo() : null,
                t.getPaisEmisorDocumentoRemitente() != null ? t.getPaisEmisorDocumentoRemitente().getCodigoIso() : null,
                t.getDocumentoBeneficiarioEnmascarado(),
                t.getTipoDocumentoBeneficiario() != null ? t.getTipoDocumentoBeneficiario().getCodigo() : null,
                t.getPaisEmisorDocumentoBeneficiario() != null ? t.getPaisEmisorDocumentoBeneficiario().getCodigoIso() : null,
                t.getMonto(),
                t.getMoneda(),
                t.getCanal(),
                t.getTipoTransaccion(),
                t.getEstado(),
                t.getEstadoEvaluacion() != null ? t.getEstadoEvaluacion().name() : null,
                t.getScoreRiesgo(),
                t.getFechaTransaccion(),
                t.getFechaProcesamiento(),
                t.getRemitenteNombreCompleto() != null ? t.getRemitenteNombreCompleto()
                        : (t.getPersonaRemitente() != null ? t.getPersonaRemitente().getNombreCompleto() : null),
                t.getBeneficiarioNombreCompleto() != null ? t.getBeneficiarioNombreCompleto()
                        : (t.getPersonaBeneficiario() != null ? t.getPersonaBeneficiario().getNombreCompleto() : null),
                t.getProducto() != null ? t.getProducto().getNombre() : null,
                t.getPaisOrigenRef() != null ? t.getPaisOrigenRef().getNombre() : t.getPaisOrigen(),
                t.getPaisDestinoRef() != null ? t.getPaisDestinoRef().getNombre() : null,
                t.getNivelRiesgo() != null ? t.getNivelRiesgo().getCodigo() : null,
                t.getEntidadOrigenTipo(),
                t.getEntidadOrigenCodigo(),
                t.getEntidadOrigenNombre(),
                t.getEntidadDestinoTipo(),
                t.getEntidadDestinoCodigo(),
                t.getEntidadDestinoNombre(),
                t.getReferenciaExterna());
    }
}
