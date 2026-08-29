package com.antifraude.drools;

import com.antifraude.common.entity.*;
import com.antifraude.common.repository.*;
import com.antifraude.dto.SimuladorRequest;
import com.antifraude.dto.SimuladorResponse;
import com.antifraude.exception.BusinessException;
import com.antifraude.transactions.Transaccion;
import com.antifraude.transactions.Transaccion.EstadoEvaluacion;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

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
    private final TipoDocumentoRepository tipoDocumentoRepository;

    public SimuladorController(RiskContextBuilder riskContextBuilder, DroolsService droolsService,
                               PaisRepository paisRepository, MonedaRepository monedaRepository,
                               CanalRepository canalRepository, ProductoRepository productoRepository,
                               TipoDocumentoRepository tipoDocumentoRepository) {
        this.riskContextBuilder = riskContextBuilder;
        this.droolsService = droolsService;
        this.paisRepository = paisRepository;
        this.monedaRepository = monedaRepository;
        this.canalRepository = canalRepository;
        this.productoRepository = productoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
    }

    @PostMapping("/evaluar")
    public ResponseEntity<SimuladorResponse> evaluar(@Valid @RequestBody SimuladorRequest request) {
        log.info("[SIM] POST /api/simulador/evaluar - UUID: {} - Monto: {}",
                "SIMULACION", request.monto());

        Transaccion transaccion = construirTransaccion(request);

        RiskContext context = riskContextBuilder.build(transaccion);
        RiskResult result = droolsService.evaluar(context);

        List<SimuladorResponse.ReglaResultado> reglas = result.reglasDisparadas().stream()
                .map(r -> new SimuladorResponse.ReglaResultado(r.codigo(), r.descripcion(), true, r.score(), r.severidad()))
                .toList();
        List<String> acciones = result.reglasDisparadas().stream()
                .map(RiskResult.ReglaDisparada::accionRecomendada)
                .filter(a -> a != null && !a.isBlank())
                .toList();

        SimuladorResponse response = new SimuladorResponse(
                result.scoreTotal(),
                result.nivelRiesgo(),
                result.requiereAccionInmediata(),
                result.observaciones(),
                transaccion.getEstado(),
                transaccion.getEstadoEvaluacion() != null ? transaccion.getEstadoEvaluacion().name() : null,
                reglas,
                acciones
        );

        log.info("[SIM] Simulacion completada - Score: {} - Nivel: {}",
                result.scoreTotal(), result.nivelRiesgo());
        return ResponseEntity.ok(response);
    }

    private Transaccion construirTransaccion(SimuladorRequest request) {
        UUID uuid = UUID.randomUUID();

        Moneda moneda = request.monedaCodigo() != null
                ? monedaRepository.findByCodigoIso(request.monedaCodigo()).orElse(null) : null;
        Canal canal = request.canalCodigo() != null
                ? canalRepository.findByCodigo(request.canalCodigo()).orElse(null) : null;
        Pais paisOrigen = resolvePais(request.paisOrigenCodigo());
        Pais paisDestino = request.paisDestinoCodigo() != null
                ? paisRepository.findByCodigoIso(request.paisDestinoCodigo()).orElse(null) : null;
        Pais paisDocumento = resolvePais(request.paisEmisorDocumentoCliente());
        TipoDocumento tipoDocumento = request.tipoDocumentoCliente() != null
                ? tipoDocumentoRepository.findByCodigo(request.tipoDocumentoCliente())
                    .or(() -> tipoDocumentoRepository.findByCodigoTecnico(request.tipoDocumentoCliente()))
                    .orElse(null)
                : null;
        validarDocumentoSimulado(request, paisDocumento, tipoDocumento);
        Producto producto = request.productoCodigo() != null
                ? productoRepository.findByCodigo(request.productoCodigo()).orElse(null) : null;

        return Transaccion.builder()
                .transactionUuid(uuid)
                .identificadorDocumento(request.documentoCliente())
                .tipoDocumentoRemitente(tipoDocumento)
                .paisEmisorDocumentoRemitente(paisDocumento)
                .documentoBeneficiario(request.documentoCliente())
                .tipoDocumentoBeneficiario(tipoDocumento)
                .paisEmisorDocumentoBeneficiario(paisDocumento)
                .cuentaOrigen("SIMULACION")
                .cuentaDestino("SIMULACION")
                .monto(request.monto())
                .moneda(request.monedaCodigo())
                .monedaRef(moneda)
                .canal(request.canalCodigo())
                .canalRef(canal)
                .tipoTransaccion("SIMULACION")
                .paisOrigen(request.paisOrigenCodigo())
                .paisOrigenRef(paisOrigen)
                .paisDestinoRef(paisDestino)
                .producto(producto)
                .fechaTransaccion(request.fechaHora() != null ? request.fechaHora() : OffsetDateTime.now())
                .estado("SIMULACION")
                .estadoEvaluacion(EstadoEvaluacion.PENDIENTE)
                .build();
    }

    private Pais resolvePais(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        return paisRepository.findByNombre(nombre)
                .orElseGet(() -> paisRepository.findByCodigoIso(nombre).orElse(null));
    }

    private void validarDocumentoSimulado(SimuladorRequest request, Pais paisDocumento, TipoDocumento tipoDocumento) {
        if (paisDocumento == null) {
            throw new BusinessException("DOCUMENT_COUNTRY_INVALID", "Pais emisor de documento invalido");
        }
        if (tipoDocumento == null) {
            throw new BusinessException("DOCUMENT_TYPE_INVALID", "Tipo de documento invalido");
        }
        if (tipoDocumento.getPaisRelacion() != null
                && !tipoDocumento.getPaisRelacion().getCodigoIso().equalsIgnoreCase(paisDocumento.getCodigoIso())) {
            throw new BusinessException("DOCUMENT_TYPE_COUNTRY_MISMATCH", "El tipo de documento no corresponde al pais indicado");
        }
        if (tipoDocumento.getFormatoRegex() != null && !tipoDocumento.getFormatoRegex().isBlank()
                && !Pattern.matches(tipoDocumento.getFormatoRegex(), request.documentoCliente().trim())) {
            throw new BusinessException("DOCUMENT_FORMAT_INVALID", "El documento no cumple el formato esperado");
        }
    }
}
