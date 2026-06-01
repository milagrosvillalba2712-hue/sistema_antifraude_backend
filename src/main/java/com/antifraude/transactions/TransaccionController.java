package com.antifraude.transactions;

import com.antifraude.dto.TransaccionRequest;
import com.antifraude.dto.TransaccionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private static final Logger log = LoggerFactory.getLogger(TransaccionController.class);

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    @PostMapping
    public ResponseEntity<TransaccionResponse> crear(@Valid @RequestBody TransaccionRequest request,
                                                      HttpServletRequest httpRequest) {
        log.info("[TX] POST /api/transacciones - UUID: {} - IP: {}", request.transactionUuid(), httpRequest.getRemoteAddr());
        Transaccion transaccion = transaccionService.crearDesdeRequest(request);
        transaccion = transaccionService.procesarTransaccion(transaccion);
        log.info("[TX] Transaccion creada y procesada - ID: {} - Estado: {} - Score: {}",
                transaccion.getId(), transaccion.getEstado(), transaccion.getScoreRiesgo());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(transaccion));
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

    private TransaccionResponse toResponse(Transaccion t) {
        return new TransaccionResponse(
                t.getId(), t.getTransactionUuid().toString(), t.getIdentificadorDocumento(),
                t.getMonto(), t.getMoneda(), t.getCanal(), t.getTipoTransaccion(),
                t.getEstado(), t.getScoreRiesgo(), t.getFechaTransaccion(), t.getFechaProcesamiento());
    }
}
