package com.antifraude.transactions;

import com.antifraude.common.entity.*;
import com.antifraude.common.repository.*;
import com.antifraude.dto.TransaccionRequest;
import com.antifraude.drools.DroolsService;
import com.antifraude.drools.RiskContext;
import com.antifraude.drools.RiskContextBuilder;
import com.antifraude.drools.RiskResult;
import com.antifraude.exception.BusinessException;
import com.antifraude.exception.ResourceNotFoundException;
import com.antifraude.rules.ReglaRiesgo;
import com.antifraude.rules.ReglaRiesgoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransaccionService {

    private static final Logger log = LoggerFactory.getLogger(TransaccionService.class);

    private final TransaccionRepository transaccionRepository;
    private final DroolsService droolsService;
    private final RiskContextBuilder riskContextBuilder;
    private final PaisRepository paisRepository;
    private final MonedaRepository monedaRepository;
    private final CanalRepository canalRepository;
    private final ProductoRepository productoRepository;
    private final PersonaRepository personaRepository;
    private final ReglaRiesgoRepository reglaRiesgoRepository;

    public TransaccionService(TransaccionRepository transaccionRepository, DroolsService droolsService,
                              RiskContextBuilder riskContextBuilder,
                              PaisRepository paisRepository, MonedaRepository monedaRepository,
                              CanalRepository canalRepository, ProductoRepository productoRepository,
                              PersonaRepository personaRepository, ReglaRiesgoRepository reglaRiesgoRepository) {
        this.transaccionRepository = transaccionRepository;
        this.droolsService = droolsService;
        this.riskContextBuilder = riskContextBuilder;
        this.paisRepository = paisRepository;
        this.monedaRepository = monedaRepository;
        this.canalRepository = canalRepository;
        this.productoRepository = productoRepository;
        this.personaRepository = personaRepository;
        this.reglaRiesgoRepository = reglaRiesgoRepository;
    }

    public Transaccion crearDesdeRequest(TransaccionRequest request) {
        UUID uuid = UUID.fromString(request.transactionUuid());
        log.info("[TX] Creando transaccion UUID: {} - Documento: {} - Monto: {} {}",
                uuid, request.identificadorDocumento(), request.monto(), request.moneda());

        if (transaccionRepository.findByTransactionUuid(uuid).isPresent()) {
            throw new BusinessException("DUPLICATE_TRANSACTION",
                    "La transaccion con UUID " + request.transactionUuid() + " ya existe");
        }

        Moneda moneda = resolveMoneda(request.moneda());
        Canal canal = resolveCanal(request.canal());
        Pais paisOrigen = resolvePais(request.paisOrigen());
        Pais paisDestino = resolvePais(request.paisDestino());
        Producto producto = request.productoId() != null
                ? productoRepository.findById(request.productoId()).orElse(null) : null;
        Persona remitente = request.personaRemitenteId() != null
                ? personaRepository.findById(request.personaRemitenteId()).orElse(null) : null;
        Persona beneficiario = request.personaBeneficiarioId() != null
                ? personaRepository.findById(request.personaBeneficiarioId()).orElse(null) : null;

        Transaccion transaccion = Transaccion.builder()
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
                .estado("PENDIENTE")
                .estadoEvaluacion(Transaccion.EstadoEvaluacion.PENDIENTE)
                .build();
        Transaccion guardada = transaccionRepository.save(transaccion);
        log.info("[TX] Transaccion creada - ID: {} - UUID: {}", guardada.getId(), uuid);
        return guardada;
    }

    public Transaccion procesarTransaccion(Transaccion transaccion) {
        log.info("[TX] Procesando transaccion ID: {} - UUID: {}", transaccion.getId(), transaccion.getTransactionUuid());

        transaccion.setEstadoEvaluacion(Transaccion.EstadoEvaluacion.EN_PROCESO);

        RiskContext context = riskContextBuilder.build(transaccion);

        RiskResult result = droolsService.evaluar(context);

        transaccion.setScoreRiesgo(result.scoreTotal());

        String estado;
        Transaccion.EstadoEvaluacion estadoEvaluacion;
        if (result.scoreTotal().compareTo(new BigDecimal("70")) >= 0) {
            estado = "SOSPECHOSA";
            estadoEvaluacion = Transaccion.EstadoEvaluacion.SOSPECHOSA;
            log.warn("[TX] Transaccion SOSPECHOSA - ID: {} - Score: {} - UUID: {}",
                    transaccion.getId(), result.scoreTotal(), transaccion.getTransactionUuid());
        } else if (result.scoreTotal().compareTo(new BigDecimal("40")) >= 0) {
            estado = "REVISION";
            estadoEvaluacion = Transaccion.EstadoEvaluacion.REVISION_MANUAL;
            log.info("[TX] Transaccion en REVISION - ID: {} - Score: {} - UUID: {}",
                    transaccion.getId(), result.scoreTotal(), transaccion.getTransactionUuid());
        } else {
            estado = "APROBADA";
            estadoEvaluacion = Transaccion.EstadoEvaluacion.APROBADA;
            log.info("[TX] Transaccion APROBADA - ID: {} - Score: {} - UUID: {}",
                    transaccion.getId(), result.scoreTotal(), transaccion.getTransactionUuid());
        }

        transaccion.setEstado(estado);
        transaccion.setEstadoEvaluacion(estadoEvaluacion);
        transaccion.setProcesada(true);
        transaccion.setFechaProcesamiento(LocalDateTime.now());
        return transaccionRepository.save(transaccion);
    }

    public List<Transaccion> listarTodas() {
        log.debug("[TX] Listando todas las transacciones");
        List<Transaccion> transacciones = transaccionRepository.findAll();
        log.debug("[TX] Total transacciones: {}", transacciones.size());
        return transacciones;
    }

    public Transaccion buscarPorId(Long id) {
        log.debug("[TX] Buscando transaccion por ID: {}", id);
        return transaccionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[TX] Transaccion no encontrada con ID: {}", id);
                    return new ResourceNotFoundException("Transaccion", "id", id);
                });
    }

    public List<Transaccion> buscarPorDocumento(String documento) {
        log.debug("[TX] Buscando transacciones por documento: {}", documento);
        return transaccionRepository.findByIdentificadorDocumento(documento);
    }

    public List<Transaccion> buscarPorEstado(String estado) {
        log.debug("[TX] Buscando transacciones por estado: {}", estado);
        return transaccionRepository.findByEstado(estado);
    }

    private Moneda resolveMoneda(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        return monedaRepository.findByCodigoIso(codigo).orElse(null);
    }

    private Canal resolveCanal(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        return canalRepository.findByCodigo(codigo).orElse(null);
    }

    private Pais resolvePais(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        return paisRepository.findByNombre(nombre)
                .orElseGet(() -> paisRepository.findByCodigoIso(nombre).orElse(null));
    }
}
