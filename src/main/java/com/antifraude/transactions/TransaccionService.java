package com.antifraude.transactions;

import com.antifraude.dto.TransaccionRequest;
import com.antifraude.drools.DroolsService;
import com.antifraude.exception.BusinessException;
import com.antifraude.exception.ResourceNotFoundException;
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

    public TransaccionService(TransaccionRepository transaccionRepository, DroolsService droolsService) {
        this.transaccionRepository = transaccionRepository;
        this.droolsService = droolsService;
    }

    public Transaccion crearDesdeRequest(TransaccionRequest request) {
        UUID uuid = UUID.fromString(request.transactionUuid());
        log.info("[TX] Creando transaccion UUID: {} - Documento: {} - Monto: {} {}",
                uuid, request.identificadorDocumento(), request.monto(), request.moneda());

        if (transaccionRepository.findByTransactionUuid(uuid).isPresent()) {
            throw new BusinessException("DUPLICATE_TRANSACTION",
                    "La transaccion con UUID " + request.transactionUuid() + " ya existe");
        }

        Transaccion transaccion = Transaccion.builder()
                .transactionUuid(uuid)
                .identificadorDocumento(request.identificadorDocumento())
                .cuentaOrigen(request.cuentaOrigen())
                .cuentaDestino(request.cuentaDestino())
                .monto(request.monto())
                .moneda(request.moneda())
                .canal(request.canal())
                .tipoTransaccion(request.tipoTransaccion())
                .ipOrigen(request.ipOrigen())
                .paisOrigen(request.paisOrigen())
                .fechaTransaccion(request.fechaTransaccion())
                .estado("PENDIENTE")
                .build();
        Transaccion guardada = transaccionRepository.save(transaccion);
        log.info("[TX] Transaccion creada - ID: {} - UUID: {}", guardada.getId(), uuid);
        return guardada;
    }

    public Transaccion procesarTransaccion(Transaccion transaccion) {
        log.info("[TX] Procesando transaccion ID: {} - UUID: {}", transaccion.getId(), transaccion.getTransactionUuid());
        BigDecimal score = droolsService.evaluarTransaccion(transaccion);
        transaccion.setScoreRiesgo(score);

        String estado;
        if (score.compareTo(new BigDecimal("70")) >= 0) {
            estado = "SOSPECHOSA";
            log.warn("[TX] Transaccion SOSPECHOSA - ID: {} - Score: {} - UUID: {}",
                    transaccion.getId(), score, transaccion.getTransactionUuid());
        } else if (score.compareTo(new BigDecimal("40")) >= 0) {
            estado = "REVISION";
            log.info("[TX] Transaccion en REVISION - ID: {} - Score: {} - UUID: {}",
                    transaccion.getId(), score, transaccion.getTransactionUuid());
        } else {
            estado = "APROBADA";
            log.info("[TX] Transaccion APROBADA - ID: {} - Score: {} - UUID: {}",
                    transaccion.getId(), score, transaccion.getTransactionUuid());
        }

        transaccion.setEstado(estado);
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
}
