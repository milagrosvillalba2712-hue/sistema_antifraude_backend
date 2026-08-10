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
import com.antifraude.licensing.Empresa;
import com.antifraude.licensing.EmpresaRepository;
import com.antifraude.licensing.ConsumoService;
import com.antifraude.licensing.EnforcementService;
import com.antifraude.rules.ReglaRiesgo;
import com.antifraude.rules.ReglaRiesgoRepository;
import com.antifraude.security.crypto.AesGcmCryptoService;
import com.antifraude.security.crypto.HmacHashService;
import com.antifraude.security.tenant.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
    private final EmpresaRepository empresaRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AesGcmCryptoService aesGcmCryptoService;
    private final HmacHashService hmacHashService;
    private final EnforcementService enforcementService;
    private final ConsumoService consumoService;

    public TransaccionService(TransaccionRepository transaccionRepository, DroolsService droolsService,
                              RiskContextBuilder riskContextBuilder,
                              PaisRepository paisRepository, MonedaRepository monedaRepository,
                              CanalRepository canalRepository, ProductoRepository productoRepository,
                              PersonaRepository personaRepository, ReglaRiesgoRepository reglaRiesgoRepository,
                              EmpresaRepository empresaRepository, JdbcTemplate jdbcTemplate,
                              AesGcmCryptoService aesGcmCryptoService, HmacHashService hmacHashService,
                              EnforcementService enforcementService, ConsumoService consumoService) {
        this.transaccionRepository = transaccionRepository;
        this.droolsService = droolsService;
        this.riskContextBuilder = riskContextBuilder;
        this.paisRepository = paisRepository;
        this.monedaRepository = monedaRepository;
        this.canalRepository = canalRepository;
        this.productoRepository = productoRepository;
        this.personaRepository = personaRepository;
        this.reglaRiesgoRepository = reglaRiesgoRepository;
        this.empresaRepository = empresaRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.aesGcmCryptoService = aesGcmCryptoService;
        this.hmacHashService = hmacHashService;
        this.enforcementService = enforcementService;
        this.consumoService = consumoService;
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
        Long tipoTransaccionId = resolveTipoTransaccionId(request.tipoTransaccion());
        Long canalTransaccionId = resolveCanalTransaccionId(request.canal());
        Pais paisOrigen = resolvePais(request.paisOrigen());
        Pais paisDestino = resolvePais(request.paisDestino());
        Empresa empresa = resolveEmpresa();
        UUID empresaId = empresa.getId();
        enforcementService.verificarSuscripcionVigente(empresaId);
        enforcementService.verificarModulo(empresaId, "TRANSACCIONES");
        enforcementService.verificarLimiteTransacciones(empresaId);
        Producto producto = request.productoId() != null
                ? productoRepository.findById(request.productoId()).orElse(null) : null;
        Persona remitente = request.personaRemitenteId() != null
                ? personaRepository.findById(request.personaRemitenteId()).orElse(null) : null;
        Persona beneficiario = request.personaBeneficiarioId() != null
                ? personaRepository.findById(request.personaBeneficiarioId()).orElse(null) : null;

        Transaccion transaccion = Transaccion.builder()
                .transactionUuid(uuid)
                .empresa(empresa)
                .codigo("TX-" + uuid.toString().substring(0, 12).toUpperCase())
                .tipoTransaccionId(tipoTransaccionId)
                .canalTransaccionId(canalTransaccionId)
                .infraestructuraPago(defaultInfraestructura(request.canal()))
                .subtipoTransaccion(request.tipoTransaccion())
                .identificadorDocumento(request.identificadorDocumento())
                .cuentaOrigen(request.cuentaOrigen())
                .cuentaDestino(request.cuentaDestino())
                .documentoRemitenteEnc(aesGcmCryptoService.encryptToBytes(request.identificadorDocumento()))
                .documentoRemitenteHash(hmacHashService.hmacBytes(request.identificadorDocumento()))
                .cuentaOrigenEnc(aesGcmCryptoService.encryptToBytes(request.cuentaOrigen()))
                .cuentaOrigenHash(hmacHashService.hmacBytes(request.cuentaOrigen()))
                .cuentaDestinoEnc(aesGcmCryptoService.encryptToBytes(request.cuentaDestino()))
                .cuentaDestinoHash(hmacHashService.hmacBytes(request.cuentaDestino()))
                .monto(request.monto())
                .moneda(request.moneda())
                .monedaRef(moneda)
                .canal(request.canal())
                .tipoTransaccion(request.tipoTransaccion())
                .ipOrigen(request.ipOrigen())
                .paisOrigen(request.paisOrigen())
                .paisOrigenRef(paisOrigen)
                .paisDestinoRef(paisDestino)
                .personaRemitente(remitente)
                .personaBeneficiario(beneficiario)
                .producto(producto)
                .fechaTransaccion(request.fechaTransaccion() != null ? request.fechaTransaccion() : OffsetDateTime.now())
                .estado("PENDIENTE")
                .estadoEvaluacion(Transaccion.EstadoEvaluacion.PENDIENTE)
                .datosEspecificos("{}")
                .riesgoParaguayJson("{}")
                .screeningResultJson("{}")
                .reglasDisparadasJson("[]")
                .build();
        Transaccion guardada = transaccionRepository.save(transaccion);
        consumoService.registrarTransaccion(empresaId);
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
            estado = "OBSERVADA";
            estadoEvaluacion = Transaccion.EstadoEvaluacion.SOSPECHOSA;
            log.warn("[TX] Transaccion SOSPECHOSA - ID: {} - Score: {} - UUID: {}",
                    transaccion.getId(), result.scoreTotal(), transaccion.getTransactionUuid());
        } else if (result.scoreTotal().compareTo(new BigDecimal("40")) >= 0) {
            estado = "OBSERVADA";
            estadoEvaluacion = Transaccion.EstadoEvaluacion.REVISION_MANUAL;
            log.info("[TX] Transaccion en REVISION - ID: {} - Score: {} - UUID: {}",
                    transaccion.getId(), result.scoreTotal(), transaccion.getTransactionUuid());
        } else {
            estado = "COMPLETADA";
            estadoEvaluacion = Transaccion.EstadoEvaluacion.APROBADA;
            log.info("[TX] Transaccion APROBADA - ID: {} - Score: {} - UUID: {}",
                    transaccion.getId(), result.scoreTotal(), transaccion.getTransactionUuid());
        }

        transaccion.setEstado(estado);
        transaccion.setEstadoEvaluacion(estadoEvaluacion);
        transaccion.setProcesada(true);
        transaccion.setFechaProcesamiento(OffsetDateTime.now());
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
        return transaccionRepository.findFirstByIdOrderByFechaTransaccionDesc(id)
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

    private Empresa resolveEmpresa() {
        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            return empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", empresaId));
        }
        return empresaRepository.findByCodigo("BANCO_REGULA")
                .or(() -> empresaRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new BusinessException("EMPRESA_REQUERIDA", "No existe empresa para registrar la transaccion"));
    }

    private Long resolveTipoTransaccionId(String codigo) {
        String value = codigo == null || codigo.isBlank() ? "PY_SPI_ALIAS_TRANSFER" : codigo;
        List<Long> ids = jdbcTemplate.queryForList(
                "select id from tipo_transaccion where codigo = ? limit 1",
                Long.class,
                value);
        if (!ids.isEmpty()) return ids.get(0);
        return jdbcTemplate.queryForObject("select id from tipo_transaccion where codigo = 'PY_SPI_ALIAS_TRANSFER'", Long.class);
    }

    private Long resolveCanalTransaccionId(String codigo) {
        String value = defaultInfraestructura(codigo);
        List<Long> ids = jdbcTemplate.queryForList(
                "select id from canal_transaccion where codigo = ? limit 1",
                Long.class,
                value);
        if (!ids.isEmpty()) return ids.get(0);
        return jdbcTemplate.queryForObject("select id from canal_transaccion where codigo = 'SPI'", Long.class);
    }

    private String defaultInfraestructura(String canal) {
        if (canal == null || canal.isBlank()) return "SPI";
        String normalized = canal.trim().toUpperCase();
        return switch (normalized) {
            case "PY_SPI_ALIAS_TRANSFER", "SPI_ALIAS", "TRANSFERENCIA_SPI" -> "SPI";
            case "PY_LBTR_HIGH_VALUE", "LBTR" -> "LBTR";
            case "PY_PAYROLL_ACH", "ACH" -> "ACH";
            case "PY_CASH_IN_BRANCH", "PY_ATM_WITHDRAWAL", "ATM", "CAJA" -> "CAJA";
            case "PY_EMPE_WALLET_P2P", "EMPE" -> "EMPE";
            case "PY_QR_EMV_PAYMENT", "QR" -> "QR";
            case "PY_REMITTANCE_RECEIVE", "REMESA" -> "REMESA";
            case "PY_FX_EXCHANGE", "CAMBIO" -> "CAMBIO";
            case "PY_TRADE_FINANCE_PAYMENT", "COMEX" -> "COMEX";
            default -> normalized;
        };
    }
}
