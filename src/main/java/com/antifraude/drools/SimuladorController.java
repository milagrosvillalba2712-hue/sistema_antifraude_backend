package com.antifraude.drools;

import com.antifraude.common.entity.*;
import com.antifraude.common.repository.*;
import com.antifraude.dto.TransaccionRequest;
import com.antifraude.transactions.Transaccion;
import com.antifraude.transactions.Transaccion.EstadoEvaluacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/simulador")
public class SimuladorController {

    private static final Logger log = LoggerFactory.getLogger(SimuladorController.class);

    private final RiskContextBuilder riskContextBuilder;
    private final DroolsService droolsService;
    private final PaisRepository paisRepository;
    private final MonedaRepository monedaRepository;
    private final CanalRepository canalRepository;
    private final ProductoRepository productoRepository;
    private final PersonaRepository personaRepository;

    public SimuladorController(RiskContextBuilder riskContextBuilder, DroolsService droolsService,
                               PaisRepository paisRepository, MonedaRepository monedaRepository,
                               CanalRepository canalRepository, ProductoRepository productoRepository,
                               PersonaRepository personaRepository) {
        this.riskContextBuilder = riskContextBuilder;
        this.droolsService = droolsService;
        this.paisRepository = paisRepository;
        this.monedaRepository = monedaRepository;
        this.canalRepository = canalRepository;
        this.productoRepository = productoRepository;
        this.personaRepository = personaRepository;
    }

    @PostMapping("/evaluar")
    public ResponseEntity<SimulacionResponse> evaluar(@RequestBody TransaccionRequest request) {
        log.info("[SIM] POST /api/simulador/evaluar - UUID: {} - Monto: {}",
                request.transactionUuid(), request.monto());

        Transaccion transaccion = construirTransaccion(request);

        RiskContext context = riskContextBuilder.build(transaccion);
        RiskResult result = droolsService.evaluar(context);

        SimulacionResponse response = new SimulacionResponse(
                result.scoreTotal(),
                result.nivelRiesgo(),
                result.requiereAccionInmediata(),
                result.observaciones(),
                transaccion.getEstado(),
                transaccion.getEstadoEvaluacion() != null ? transaccion.getEstadoEvaluacion().name() : null
        );

        log.info("[SIM] Simulacion completada - Score: {} - Nivel: {}",
                result.scoreTotal(), result.nivelRiesgo());
        return ResponseEntity.ok(response);
    }

    private Transaccion construirTransaccion(TransaccionRequest request) {
        UUID uuid = UUID.fromString(request.transactionUuid());

        Moneda moneda = request.moneda() != null
                ? monedaRepository.findByCodigoIso(request.moneda()).orElse(null) : null;
        Canal canal = request.canal() != null
                ? canalRepository.findByCodigo(request.canal()).orElse(null) : null;
        Pais paisOrigen = resolvePais(request.paisOrigen());
        Pais paisDestino = request.paisDestino() != null
                ? paisRepository.findByCodigoIso(request.paisDestino()).orElse(null) : null;
        Producto producto = request.productoId() != null
                ? productoRepository.findById(request.productoId()).orElse(null) : null;
        Persona remitente = request.personaRemitenteId() != null
                ? personaRepository.findById(request.personaRemitenteId()).orElse(null) : null;
        Persona beneficiario = request.personaBeneficiarioId() != null
                ? personaRepository.findById(request.personaBeneficiarioId()).orElse(null) : null;

        return Transaccion.builder()
                .transactionUuid(uuid)
                .identificadorDocumento(request.identificadorDocumento())
                .cuentaOrigen(request.cuentaOrigen())
                .cuentaDestino(request.cuentaDestino())
                .monto(request.monto())
                .moneda(request.moneda())
                .monedaRef(moneda)
                .canal(request.canal())
                .canalRef(canal)
                .tipoTransaccion(request.tipoTransaccion())
                .ipOrigen(request.ipOrigen())
                .paisOrigen(request.paisOrigen())
                .paisOrigenRef(paisOrigen)
                .paisDestinoRef(paisDestino)
                .personaRemitente(remitente)
                .personaBeneficiario(beneficiario)
                .producto(producto)
                .fechaTransaccion(request.fechaTransaccion())
                .estado("SIMULACION")
                .estadoEvaluacion(EstadoEvaluacion.PENDIENTE)
                .build();
    }

    private Pais resolvePais(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        return paisRepository.findByNombre(nombre)
                .orElseGet(() -> paisRepository.findByCodigoIso(nombre).orElse(null));
    }

    public record SimulacionResponse(
            BigDecimal scoreTotal,
            String nivelRiesgo,
            boolean requiereAccionInmediata,
            String observaciones,
            String estado,
            String estadoEvaluacion
    ) {}
}
