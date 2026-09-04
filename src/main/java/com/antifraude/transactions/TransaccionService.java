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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional(noRollbackFor = BusinessException.class)
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
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final ReglaRiesgoRepository reglaRiesgoRepository;
    private final NivelRiesgoRepository nivelRiesgoRepository;
    private final EmpresaRepository empresaRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AesGcmCryptoService aesGcmCryptoService;
    private final HmacHashService hmacHashService;
    private final EnforcementService enforcementService;
    private final ConsumoService consumoService;
    private final ObjectMapper objectMapper;

    public TransaccionService(TransaccionRepository transaccionRepository, DroolsService droolsService,
                              RiskContextBuilder riskContextBuilder,
                              PaisRepository paisRepository, MonedaRepository monedaRepository,
                              CanalRepository canalRepository, ProductoRepository productoRepository,
                              PersonaRepository personaRepository, TipoDocumentoRepository tipoDocumentoRepository,
                              ReglaRiesgoRepository reglaRiesgoRepository,
                              EmpresaRepository empresaRepository, NivelRiesgoRepository nivelRiesgoRepository,
                              JdbcTemplate jdbcTemplate,
                              AesGcmCryptoService aesGcmCryptoService, HmacHashService hmacHashService,
                              EnforcementService enforcementService, ConsumoService consumoService,
                              ObjectMapper objectMapper) {
        this.transaccionRepository = transaccionRepository;
        this.droolsService = droolsService;
        this.riskContextBuilder = riskContextBuilder;
        this.paisRepository = paisRepository;
        this.monedaRepository = monedaRepository;
        this.canalRepository = canalRepository;
        this.productoRepository = productoRepository;
        this.personaRepository = personaRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.reglaRiesgoRepository = reglaRiesgoRepository;
        this.nivelRiesgoRepository = nivelRiesgoRepository;
        this.empresaRepository = empresaRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.aesGcmCryptoService = aesGcmCryptoService;
        this.hmacHashService = hmacHashService;
        this.enforcementService = enforcementService;
        this.consumoService = consumoService;
        this.objectMapper = objectMapper;
    }

    public Transaccion crearDesdeRequest(TransaccionRequest request) {
        UUID uuid = UUID.fromString(request.transactionUuid());
        log.info("[TX] Creando transaccion UUID: {} - Monto: {} {}",
                uuid, request.monto(), request.moneda());

        if (transaccionRepository.findByTransactionUuid(uuid).isPresent()) {
            throw new BusinessException("DUPLICATE_TRANSACTION",
                    "La transaccion con UUID " + request.transactionUuid() + " ya existe");
        }

        Moneda moneda = resolveMoneda(request.moneda());
        Long tipoTransaccionId = resolveTipoTransaccionId(request.tipoTransaccion());
        Long canalTransaccionId = resolveCanalTransaccionId(request.canal());
        Pais paisOrigen = resolvePais(request.paisOrigen());
        Pais paisDestino = resolvePais(request.paisDestino());
        Pais paisDocRemitente = requirePais(request.paisEmisorDocumentoRemitente(), "paisEmisorDocumentoRemitente");
        Pais paisDocBeneficiario = requirePais(request.paisEmisorDocumentoBeneficiario(), "paisEmisorDocumentoBeneficiario");
        TipoDocumento tipoDocRemitente = requireTipoDocumento(request.tipoDocumentoRemitenteId(), request.tipoDocumentoRemitente(),
                paisDocRemitente, request.documentoRemitente(), "remitente");
        TipoDocumento tipoDocBeneficiario = requireTipoDocumento(request.tipoDocumentoBeneficiarioId(), request.tipoDocumentoBeneficiario(),
                paisDocBeneficiario, request.documentoBeneficiario(), "beneficiario");
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

        String remitenteNombre = resolveNombreCompleto(remitente, request.nombreCompletoRemitente(),
                request.personaRemitenteId(), "remitente");
        String beneficiarioNombre = resolveNombreCompleto(beneficiario, request.nombreCompletoBeneficiario(),
                request.personaBeneficiarioId(), "beneficiario");

        validarEntidadesPorCanal(request.tipoTransaccion(), request.canal(),
                request.entidadOrigenTipo(), request.entidadDestinoTipo());

        Transaccion transaccion = Transaccion.builder()
                .transactionUuid(uuid)
                .empresa(empresa)
                .codigo("TX-" + uuid.toString().replace("-", "").toUpperCase())
                .tipoTransaccionId(tipoTransaccionId)
                .canalTransaccionId(canalTransaccionId)
                .infraestructuraPago(defaultInfraestructura(request.canal()))
                .subtipoTransaccion(request.tipoTransaccion())
                .identificadorDocumento(request.documentoRemitente())
                .documentoBeneficiario(request.documentoBeneficiario())
                .cuentaOrigen(request.cuentaOrigen())
                .cuentaDestino(request.cuentaDestino())
                .documentoRemitenteEnc(aesGcmCryptoService.encryptToBytes(request.documentoRemitente()))
                .documentoRemitenteHash(hmacHashService.hmacBytes(request.documentoRemitente()))
                .tipoDocumentoRemitente(tipoDocRemitente)
                .paisEmisorDocumentoRemitente(paisDocRemitente)
                .documentoBeneficiarioEnc(aesGcmCryptoService.encryptToBytes(request.documentoBeneficiario()))
                .documentoBeneficiarioHash(hmacHashService.hmacBytes(request.documentoBeneficiario()))
                .tipoDocumentoBeneficiario(tipoDocBeneficiario)
                .paisEmisorDocumentoBeneficiario(paisDocBeneficiario)
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
                .nombreRemitente(remitenteNombre)
                .nombreBeneficiario(beneficiarioNombre)
                .remitenteNombreCompleto(remitenteNombre)
                .beneficiarioNombreCompleto(beneficiarioNombre)
                .entidadOrigenTipo(normalizeUpper(request.entidadOrigenTipo()))
                .entidadOrigenCodigo(trimToNull(request.entidadOrigenCodigo()))
                .entidadOrigenNombre(trimToNull(request.entidadOrigenNombre()))
                .entidadDestinoTipo(normalizeUpper(request.entidadDestinoTipo()))
                .entidadDestinoCodigo(trimToNull(request.entidadDestinoCodigo()))
                .entidadDestinoNombre(trimToNull(request.entidadDestinoNombre()))
                .referenciaExterna(trimToNull(request.referenciaExterna()))
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

    private String resolveNombreCompleto(Persona persona, String nombreCompleto,
                                         Long personaId, String parte) {
        if (persona != null) {
            String derivado = persona.getNombreCompleto();
            if (derivado != null && !derivado.isBlank()) {
                return derivado.trim();
            }
            log.warn("[TX] Persona {} {} sin identidad derivable (ID {}); se usa la del request", parte, personaId, persona.getId());
        }
        return nombreCompleto == null ? "" : nombreCompleto.trim();
    }

    public Transaccion procesarTransaccion(Transaccion transaccion) {
        log.info("[TX] Procesando transaccion ID: {} - UUID: {}", transaccion.getId(), transaccion.getTransactionUuid());

        transaccion.setEstadoEvaluacion(Transaccion.EstadoEvaluacion.EN_PROCESO);

        RiskContext context = riskContextBuilder.build(transaccion);

        RiskResult result = droolsService.evaluar(context);

        transaccion.setScoreRiesgo(result.scoreTotal());

        String nivel = result.nivelRiesgo();
        if (nivel != null) {
            transaccion.setNivelRiesgo(nivelRiesgoRepository.findByCodigo(nivel).orElse(null));
        }

        if (result.requiereAccionInmediata()) {
            droolsService.crearAlertasDesdeResultado(transaccion, result.reglasDisparadas(),
                    result.scoreTotal(), result.nivelRiesgo());
        }

        String estado;
        Transaccion.EstadoEvaluacion estadoEvaluacion;
        if (result.scoreTotal().compareTo(new BigDecimal("70")) >= 0) {
            estado = "OBSERVADA";
            estadoEvaluacion = Transaccion.EstadoEvaluacion.SOSPECHOSA;
            log.warn("[TX] Transaccion SOSPECHOSA - ID: {} - Score: {} - UUID: {}",
                    transaccion.getId(), result.scoreTotal(), transaccion.getTransactionUuid());
        } else if (result.requiereAccionInmediata() || result.scoreTotal().compareTo(new BigDecimal("40")) >= 0) {
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
        transaccion.setReglasDisparadasJson(toJson(result.reglasDisparadas()));
        transaccion.setScreeningResultJson(toJson(result.coincidenciasListas()));
        transaccion.setProcesada(true);
        transaccion.setFechaProcesamiento(OffsetDateTime.now());
        return transaccionRepository.save(transaccion);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("[TX] No se pudo serializar detalle de evaluacion: {}", e.getMessage());
            return "[]";
        }
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
        log.debug("[TX] Buscando transacciones por documento protegido");
        return transaccionRepository.findByDocumentoHash(hmacHashService.hmacBytes(documento));
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

    private Pais requirePais(String codigo, String field) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("DOCUMENT_COUNTRY_REQUIRED", "El campo " + field + " es obligatorio");
        }
        return paisRepository.findByCodigoIso(codigo.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException("DOCUMENT_COUNTRY_INVALID",
                        "El pais emisor de documento " + codigo + " no existe"));
    }

    private TipoDocumento requireTipoDocumento(Long id, String codigo, Pais pais, String numeroDocumento, String parte) {
        if (id == null && (codigo == null || codigo.isBlank())) {
            throw new BusinessException("DOCUMENT_TYPE_REQUIRED",
                    "El tipo de documento del " + parte + " es obligatorio");
        }
        TipoDocumento tipoDocumento = id != null
                ? tipoDocumentoRepository.findById(id).orElseThrow(() -> new BusinessException("DOCUMENT_TYPE_INVALID",
                "El tipo de documento con id " + id + " no existe"))
                : resolveTipoDocumentoByCode(codigo);
        if (Boolean.FALSE.equals(tipoDocumento.getEstadoActivo()) || Boolean.FALSE.equals(tipoDocumento.getActivo())) {
            throw new BusinessException("DOCUMENT_TYPE_INACTIVE",
                    "El tipo de documento " + tipoDocumento.getCodigo() + " no esta activo");
        }
        if (tipoDocumento.getPaisRelacion() != null
                && !tipoDocumento.getPaisRelacion().getCodigoIso().equalsIgnoreCase(pais.getCodigoIso())) {
            throw new BusinessException("DOCUMENT_TYPE_COUNTRY_MISMATCH",
                    "El tipo de documento " + tipoDocumento.getCodigo() + " no corresponde al pais " + pais.getCodigoIso());
        }
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new BusinessException("DOCUMENT_NUMBER_REQUIRED",
                    "El numero de documento del " + parte + " es obligatorio");
        }
        String regex = tipoDocumento.getFormatoRegex();
        if (regex != null && !regex.isBlank() && !Pattern.matches(regex, numeroDocumento.trim())) {
            throw new BusinessException("DOCUMENT_FORMAT_INVALID",
                    "El numero de documento del " + parte + " no cumple el formato esperado para " + tipoDocumento.getCodigo());
        }
        return tipoDocumento;
    }

    private TipoDocumento resolveTipoDocumentoByCode(String codigo) {
        String codigoNormalizado = codigo.trim().toUpperCase(Locale.ROOT);
        if (codigoNormalizado.matches("\\d+")) {
            Long id = Long.valueOf(codigoNormalizado);
            return tipoDocumentoRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("DOCUMENT_TYPE_INVALID",
                            "El tipo de documento con id " + id + " no existe"));
        }
        return tipoDocumentoRepository.findByCodigo(codigoNormalizado)
                .or(() -> tipoDocumentoRepository.findByCodigoTecnico(codigoNormalizado))
                .orElseThrow(() -> new BusinessException("DOCUMENT_TYPE_INVALID",
                        "El tipo de documento " + codigo + " no existe"));
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

    private String normalizeUpper(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private void validarEntidadesPorCanal(String tipoTransaccion, String canal,
                                          String entidadOrigenTipo, String entidadDestinoTipo) {
        String tipo = normalizeUpper(tipoTransaccion);
        String ch = normalizeUpper(canal);
        if (tipo == null) return;

        boolean esDestino = contiene(tipo, "SEND", "ENVIO", "TRANSFER", "P2P", "TOPUP",
                "REMITTANCE_SEND", "PAYMENT", "PAYROLL", "BATCH_CREDIT", "ALIAS_TRANSFER", "HIGH_VALUE");
        boolean esOrigen = contiene(tipo, "RECEIVE", "COBRO", "CASH_IN", "DEPOSITO");
        boolean esExtraccion = contiene(tipo, "WITHDRAWAL", "EXTRACCION", "RETIRO", "ATM");
        boolean esInternacional = contiene(tipo, "REMITTANCE_SEND", "COMEX", "FX", "TRADE");
        if (ch != null) {
            esDestino = esDestino || contiene(ch, "SPI", "ACH", "QR", "EMPE", "REMESA", "COMEX", "CAMBIO");
            esOrigen = esOrigen || contiene(ch, "CAJA", "REMESA");
            esExtraccion = esExtraccion || contiene(ch, "ATM");
        }

        if (esDestino && entidadDestinoTipo == null) {
            throw new BusinessException("ENTIDAD_DESTINO_REQUERIDA",
                    "El tipo " + tipoTransaccion + " requiere entidadDestinoTipo (banco/financiera/intermediario) para completar el envío.");
        }
        if (esOrigen && entidadOrigenTipo == null) {
            throw new BusinessException("ENTIDAD_ORIGEN_REQUERIDA",
                    "El tipo " + tipoTransaccion + " requiere entidadOrigenTipo (banco/sucursal/cajero) para completar la recepción.");
        }
        if (esExtraccion && entidadOrigenTipo == null) {
            throw new BusinessException("ENTIDAD_ORIGEN_REQUERIDA",
                    "El tipo " + tipoTransaccion + " (extracción/retiro) requiere entidadOrigenTipo (sucursal o cajero).");
        }
        if (esInternacional && entidadDestinoTipo == null) {
            throw new BusinessException("ENTIDAD_DESTINO_REQUERIDA",
                    "La operación internacional " + tipoTransaccion + " requiere entidadDestinoTipo + información SWIFT del beneficiario.");
        }
    }

    private boolean contiene(String value, String... tokens) {
        if (value == null) return false;
        for (String token : tokens) {
            if (value.contains(token)) return true;
        }
        return false;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
