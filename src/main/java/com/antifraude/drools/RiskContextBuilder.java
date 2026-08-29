package com.antifraude.drools;

import com.antifraude.common.entity.*;
import com.antifraude.common.repository.*;
import com.antifraude.drools.fact.*;
import com.antifraude.transactions.Transaccion;
import com.antifraude.transactions.TransaccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RiskContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(RiskContextBuilder.class);

    private final TransaccionRepository transaccionRepository;
    private final ClienteObservadoRepository clienteObservadoRepository;
    private final ClientePEPRepository clientePEPRepository;
    private final ListaRegulatoriaRepository listaRegulatoriaRepository;
    private final ElementoListaRepository elementoListaRepository;
    private final HorarioRiesgoRepository horarioRiesgoRepository;
    private final CalendarioRiesgoRepository calendarioRiesgoRepository;
    private final ControlImporteRepository controlImporteRepository;
    private final ControlFrecuenciaRepository controlFrecuenciaRepository;
    private final CanalRepository canalRepository;
    private final ListScreeningService listScreeningService;
    private final DroolsScoreConfigService droolsScoreConfigService;

    public RiskContextBuilder(TransaccionRepository transaccionRepository,
                             ClienteObservadoRepository clienteObservadoRepository,
                             ClientePEPRepository clientePEPRepository,
                             ListaRegulatoriaRepository listaRegulatoriaRepository,
                             ElementoListaRepository elementoListaRepository,
                             HorarioRiesgoRepository horarioRiesgoRepository,
                             CalendarioRiesgoRepository calendarioRiesgoRepository,
                             ControlImporteRepository controlImporteRepository,
                             ControlFrecuenciaRepository controlFrecuenciaRepository,
                              CanalRepository canalRepository,
                              ListScreeningService listScreeningService,
                              DroolsScoreConfigService droolsScoreConfigService) {
        this.transaccionRepository = transaccionRepository;
        this.clienteObservadoRepository = clienteObservadoRepository;
        this.clientePEPRepository = clientePEPRepository;
        this.listaRegulatoriaRepository = listaRegulatoriaRepository;
        this.elementoListaRepository = elementoListaRepository;
        this.horarioRiesgoRepository = horarioRiesgoRepository;
        this.calendarioRiesgoRepository = calendarioRiesgoRepository;
        this.controlImporteRepository = controlImporteRepository;
        this.controlFrecuenciaRepository = controlFrecuenciaRepository;
        this.canalRepository = canalRepository;
        this.listScreeningService = listScreeningService;
        this.droolsScoreConfigService = droolsScoreConfigService;
    }

    public RiskContext build(Transaccion transaccion) {
        RiskContext context = new RiskContext();
        context.setTransaccion(transaccion);
        context.setTransaccionFact(toTransaccionFact(transaccion));
        context.setFechaHoraActual(LocalDateTime.now());
        context.setConfig(droolsScoreConfigService.getConfig());
        String documento = transaccion.getDocumentoRemitente();

        log.debug("[CTX] Cargando historial para documento: {}", documento);
        List<Transaccion> historial = transaccionRepository.findUltimasPorDocumento(documento);
        context.setHistorialTransacciones(
                historial.stream().map(this::toTransaccionFact).toList());

        // Una lista configurada no es una coincidencia. Solo el screening de la
        // transacción puede incorporar elementos coincidentes al contexto.
        log.debug("[CTX] Ejecutando screening de listas regulatorias");
        cargarScreeningListas(context, transaccion);

        log.debug("[CTX] Cargando registros PEP y observados para documento: {}", documento);
        cargarPEP(context, documento);
        cargarObservados(context, documento);

        log.debug("[CTX] Cargando horarios y calendario de riesgo");
        cargarHorariosYCalendario(context);

        log.debug("[CTX] Cargando controles de importe y frecuencia");
        cargarControles(context, transaccion);

        log.debug("[CTX] Resolviendo canal de alto riesgo");
        cargarCanalAltoRiesgo(context, transaccion);

        log.debug("[CTX] RiskContext construido - Historial: {}, Listas Negras: {}, PEP: {}, Observados: {}",
                context.getHistorialTransacciones().size(),
                context.getListasNegras().size(),
                context.getRegistrosPEP().size(),
                context.getRegistrosObservados().size());

        return context;
    }

    private void cargarScreeningListas(RiskContext context, Transaccion transaccion) {
        List<CoincidenciaListaFact> coincidencias = listScreeningService.screen(transaccion);
        context.setCoincidenciasListas(coincidencias);
        context.setRemitenteEnLista(coincidencias.stream().anyMatch(c -> "REMITENTE".equalsIgnoreCase(c.getParteTransaccion())));
        context.setBeneficiarioEnLista(coincidencias.stream().anyMatch(c -> "BENEFICIARIO".equalsIgnoreCase(c.getParteTransaccion())));
        context.setDocumentoEnLista(coincidencias.stream().anyMatch(c -> "DOCUMENTO".equalsIgnoreCase(c.getCampoEvaluado())));
        context.setCuentaEnLista(coincidencias.stream().anyMatch(c -> "CUENTA".equalsIgnoreCase(c.getCampoEvaluado())));
        context.setPaisOrigenAltoRiesgo(coincidencias.stream().anyMatch(c -> "ORIGEN".equalsIgnoreCase(c.getParteTransaccion()) && isHighRiskCountry(c)));
        context.setPaisDestinoAltoRiesgo(coincidencias.stream().anyMatch(c -> "DESTINO".equalsIgnoreCase(c.getParteTransaccion()) && isHighRiskCountry(c)));
        context.setPaisOrigenMonitoreado(coincidencias.stream().anyMatch(c -> "ORIGEN".equalsIgnoreCase(c.getParteTransaccion()) && isMonitoredCountry(c)));
        context.setPaisDestinoMonitoreado(coincidencias.stream().anyMatch(c -> "DESTINO".equalsIgnoreCase(c.getParteTransaccion()) && isMonitoredCountry(c)));

        for (CoincidenciaListaFact c : coincidencias) {
            ListaFact fact = new ListaFact();
            fact.setNombreLista(c.getListaCodigo());
            fact.setFuente(c.getFuenteCodigo());
            fact.setNombreCompleto(c.getNombreSujeto());
            fact.setDocumentoIdentidad(c.getValorEvaluado());
            fact.setScoreConfianza(c.getScoreMatch());
            // El tipoLista debe coincidir con el bucket de la DRL ("NEGRA"/"GRIS"/"BLANCA").
            if ("WHITELIST".equalsIgnoreCase(c.getCategoria())) {
                fact.setTipoLista("BLANCA");
                context.getListasBlancas().add(fact);
            } else if (isCritical(c) || "BLACKLIST".equalsIgnoreCase(c.getCategoria())) {
                fact.setTipoLista("NEGRA");
                context.getListasNegras().add(fact);
            } else if (isLowRisk(c)) {
                fact.setTipoLista("BLANCA");
                context.getListasBlancas().add(fact);
            } else {
                fact.setTipoLista("GRIS");
                context.getListasGrises().add(fact);
            }
        }
    }

    private boolean isHighRiskCountry(CoincidenciaListaFact c) {
        return "PAIS_RIESGO".equalsIgnoreCase(c.getCategoria()) && isCritical(c);
    }

    private boolean isMonitoredCountry(CoincidenciaListaFact c) {
        return "PAIS_RIESGO".equalsIgnoreCase(c.getCategoria()) && !isCritical(c);
    }

    private boolean isCritical(CoincidenciaListaFact c) {
        return c.getSeveridad() != null && (c.getSeveridad().equalsIgnoreCase("CRITICO")
                || c.getSeveridad().equalsIgnoreCase("CRÍTICO")
                || c.getSeveridad().equalsIgnoreCase("CRITICA")
                || c.getSeveridad().equalsIgnoreCase("CRÍTICA")
                || c.getSeveridad().equalsIgnoreCase("Critico")
                || c.getSeveridad().equalsIgnoreCase("Crítico"));
    }

    private boolean isLowRisk(CoincidenciaListaFact c) {
        return c.getSeveridad() != null && c.getSeveridad().equalsIgnoreCase("Baja");
    }

    private void cargarListas(RiskContext context) {
        List<ListaRegulatoria> listas = listaRegulatoriaRepository.findAll();
        for (ListaRegulatoria lista : listas) {
            List<ElementoLista> elementos = elementoListaRepository
                    .findByListaRegulatoriaIdAndValorIdentificador(lista.getId(), null);

            if (elementos.isEmpty()) {
                elementos = elementoListaRepository.findByListaRegulatoriaId(lista.getId());
            }

            for (ElementoLista elem : elementos) {
                ListaFact fact = new ListaFact();
                fact.setListaId(lista.getId());
                fact.setNombreLista(lista.getNombre());
                fact.setTipoLista("LISTA");
                fact.setFuente(lista.getFuente() != null ? lista.getFuente().name() : null);
                fact.setNombreCompleto(elem.getValorIdentificador());
                fact.setDocumentoIdentidad(elem.getValorIdentificador());

                String tipoLista = "LISTA";
                if (lista.getNombre() != null) {
                    String nombreLower = lista.getNombre().toLowerCase();
                    if (nombreLower.contains("negra") || nombreLower.contains("black")) {
                        tipoLista = "NEGRA";
                    } else if (nombreLower.contains("gris") || nombreLower.contains("grey")) {
                        tipoLista = "GRIS";
                    } else if (nombreLower.contains("blanca") || nombreLower.contains("white")) {
                        tipoLista = "BLANCA";
                    }
                }
                fact.setTipoLista(tipoLista);

                switch (tipoLista) {
                    case "NEGRA" -> context.getListasNegras().add(fact);
                    case "GRIS" -> context.getListasGrises().add(fact);
                    case "BLANCA" -> context.getListasBlancas().add(fact);
                    default -> context.getListasGrises().add(fact);
                }
            }
        }
    }

    private void cargarPEP(RiskContext context, String documento) {
        List<ClientePEP> pepList = clientePEPRepository.findByNumeroDocumento(documento);
        List<PeptFact> facts = new ArrayList<>();
        for (ClientePEP pep : pepList) {
            PeptFact fact = new PeptFact();
            if (pep.getPersona() != null) {
                fact.setPersonaId(pep.getPersona().getId());
                fact.setNombreCompleto(pep.getPersona().getNombreCompleto());
            }
            fact.setDocumentoIdentidad(pep.getNumeroDocumento());
            fact.setCargo(pep.getCargo());
            fact.setEntidad(pep.getInstitucion());
            fact.setFuente(pep.getFuente() != null ? pep.getFuente().name() : null);
            facts.add(fact);
        }
        context.setRegistrosPEP(facts);
    }

    private void cargarObservados(RiskContext context, String documento) {
        List<ClienteObservado> observados = clienteObservadoRepository.findAll();
        List<ObservadoFact> facts = new ArrayList<>();
        for (ClienteObservado obs : observados) {
            if (obs.getPersona() != null && documento != null && documento.equals(obs.getPersona().getId().toString())) {
                ObservadoFact fact = new ObservadoFact();
                fact.setClienteId(obs.getPersona().getId());
                fact.setNombreCompleto(obs.getPersona().getNombreCompleto());
                fact.setDocumentoIdentidad(documento);
                fact.setMotivo(obs.getMotivo());
                facts.add(fact);
            }
        }
        context.setRegistrosObservados(facts);
    }

    private void cargarHorariosYCalendario(RiskContext context) {
        List<HorarioRiesgo> horarios = horarioRiesgoRepository.findAllActive();
        List<HorarioRiesgoFact> horarioFacts = new ArrayList<>();
        for (HorarioRiesgo h : horarios) {
            HorarioRiesgoFact fact = new HorarioRiesgoFact();
            fact.setHorarioId(h.getId());
            fact.setDescripcion(h.getNombre());
            fact.setHoraDesde(h.getHoraDesde());
            fact.setHoraHasta(h.getHoraHasta());
            fact.setPorcentajeExtra(150);
            horarioFacts.add(fact);
        }
        context.setHorariosRiesgo(horarioFacts);

        List<CalendarioRiesgo> calendarios = calendarioRiesgoRepository.findAll();
        List<CalendarioRiesgoFact> calendarioFacts = new ArrayList<>();
        for (CalendarioRiesgo c : calendarios) {
            CalendarioRiesgoFact fact = new CalendarioRiesgoFact();
            fact.setCalendarioId(c.getId());
            fact.setDescripcion(c.getDescripcion());
            fact.setFecha(c.getFecha());
            fact.setTipoEvento(c.getTipoDia() != null ? c.getTipoDia().name() : null);
            calendarioFacts.add(fact);
        }
        context.setCalendarioRiesgo(calendarioFacts);
    }

    private void cargarControles(RiskContext context, Transaccion transaccion) {
        String productoCodigo = transaccion.getProducto() != null ? transaccion.getProducto().getCodigo() : null;

        List<ControlImporte> controlesImporte;
        if (productoCodigo != null) {
            controlesImporte = controlImporteRepository.findByProductoCodigo(productoCodigo);
        } else {
            controlesImporte = controlImporteRepository.findAll();
        }
        List<ControlImporteFact> importeFacts = new ArrayList<>();
        for (ControlImporte ci : controlesImporte) {
            if (!aplicaControlImporte(ci, transaccion)) {
                continue;
            }
            ControlImporteFact fact = new ControlImporteFact();
            fact.setControlId(ci.getId());
            fact.setProductoCodigo(ci.getProducto() != null ? ci.getProducto().getCodigo() : null);
            fact.setTipoTransaccion(transaccion.getTipoTransaccion());
            fact.setMonedaCodigo(ci.getMoneda() != null ? ci.getMoneda().getCodigoIso() : null);
            fact.setMontoMinimo(ci.getMontoMinimo());
            fact.setMontoMaximo(ci.getMontoMaximo());
            fact.setSeveridad(ci.getSeveridad());
            importeFacts.add(fact);
        }
        context.setControlesImporte(mejorControlImporte(importeFacts));

        List<ControlFrecuencia> controlesFrecuencia;
        if (productoCodigo != null) {
            controlesFrecuencia = controlFrecuenciaRepository.findByProductoCodigo(productoCodigo);
        } else {
            controlesFrecuencia = controlFrecuenciaRepository.findAll();
        }
        List<ControlFrecuenciaFact> frecuenciaFacts = new ArrayList<>();
        for (ControlFrecuencia cf : controlesFrecuencia) {
            ControlFrecuenciaFact fact = new ControlFrecuenciaFact();
            fact.setControlFrecuenciaId(cf.getId());
            fact.setProductoCodigo(cf.getProducto() != null ? cf.getProducto().getCodigo() : null);
            fact.setCantidadMaxima(cf.getCantidadOperaciones());
            fact.setVentanaHoras(cf.getVentanaTiempo());
            frecuenciaFacts.add(fact);
        }
        context.setControlesFrecuencia(frecuenciaFacts);
    }

    private void cargarCanalAltoRiesgo(RiskContext context, Transaccion transaccion) {
        boolean altoRiesgo = false;
        if (transaccion != null) {
            Canal canal = null;
            if (transaccion.getCanalTransaccionId() != null) {
                canal = canalRepository.findById(transaccion.getCanalTransaccionId()).orElse(null);
            }
            if (canal == null && transaccion.getCanal() != null) {
                canal = canalRepository.findByCodigo(transaccion.getCanal()).orElse(null);
            }
            altoRiesgo = canal != null && Boolean.TRUE.equals(canal.getAltoRiesgo());
        }
        context.setCanalAltoRiesgo(altoRiesgo);
    }

    private boolean aplicaControlImporte(ControlImporte control, Transaccion transaccion) {
        if (control == null || transaccion == null || transaccion.getMonto() == null
                || control.getMontoMaximo() == null || control.getMoneda() == null) {
            return false;
        }
        String monedaTransaccion = transaccion.getMoneda();
        if (monedaTransaccion == null || !control.getMoneda().getCodigoIso().equalsIgnoreCase(monedaTransaccion)) {
            return false;
        }
        if (control.getTipoTransaccionId() != null && !control.getTipoTransaccionId().equals(transaccion.getTipoTransaccionId())) {
            return false;
        }
        return transaccion.getMonto().compareTo(control.getMontoMaximo()) > 0;
    }

    private List<ControlImporteFact> mejorControlImporte(List<ControlImporteFact> controles) {
        return controles.stream()
                .max(Comparator
                        .comparingInt((ControlImporteFact c) -> prioridadSeveridad(c.getSeveridad()))
                        .thenComparing(ControlImporteFact::getMontoMaximo, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(List::of)
                .orElseGet(List::of);
    }

    private int prioridadSeveridad(String severidad) {
        if (severidad == null) {
            return 0;
        }
        return switch (severidad.toUpperCase(java.util.Locale.ROOT)) {
            case "CRITICA", "CRÍTICA", "CRITICO", "CRÍTICO" -> 3;
            case "ALTA", "ALTO" -> 2;
            case "MEDIA", "MEDIO" -> 1;
            default -> 0;
        };
    }

    private TransaccionFact toTransaccionFact(Transaccion t) {
        TransaccionFact fact = new TransaccionFact();
        fact.setId(t.getId());
        fact.setTransactionUuid(t.getTransactionUuid() != null ? t.getTransactionUuid().toString() : null);
        fact.setCodigo(t.getCodigo());
        fact.setIdentificadorDocumento(t.getDocumentoRemitente());
        fact.setCuentaOrigen(t.getCuentaOrigen());
        fact.setCuentaDestino(t.getCuentaDestino());
        fact.setMonto(t.getMonto());
        fact.setMonedaCodigo(t.getMoneda());
        fact.setCanalCodigo(t.getCanal());
        fact.setTipoTransaccion(t.getTipoTransaccion());
        fact.setInfraestructuraPago(t.getInfraestructuraPago() != null ? t.getInfraestructuraPago() : t.getCanal());
        fact.setModuloSipap(t.getModuloSipap() != null ? t.getModuloSipap()
                : (t.getCanal() != null && t.getCanal().toUpperCase().contains("SPI") ? "SPI" : null));
        fact.setSubtipoTransaccion(t.getSubtipoTransaccion());
        fact.setEndToEndId(t.getEndToEndId());
        fact.setSpiReference(t.getSpiReference());
        fact.setRequiereDeclaracionFondos(Boolean.TRUE.equals(t.getRequiereDeclaracionFondos()));
        fact.setDepositanteTercero(Boolean.TRUE.equals(t.getDepositanteTercero()));
        fact.setMcc(t.getMcc());
        fact.setNombreComercio(t.getNombreComercio());
        fact.setPanLast4(t.getPanLast4());
        fact.setSwiftBicOrigen(t.getSwiftBicOrigen());
        fact.setSwiftBicDestino(t.getSwiftBicDestino());
        fact.setIpOrigen(t.getIpOrigen());
        fact.setPaisOrigenCodigo(t.getPaisOrigen());
        fact.setFechaTransaccion(t.getFechaTransaccion());
        if (t.getPaisOrigenRef() != null) {
            fact.setPaisOrigenNombre(t.getPaisOrigenRef().getNombre());
        }
        if (t.getPaisDestinoRef() != null) {
            fact.setPaisDestinoCodigo(t.getPaisDestinoRef().getCodigoIso());
            fact.setPaisDestinoNombre(t.getPaisDestinoRef().getNombre());
        }
        if (t.getProducto() != null) {
            fact.setProductoId(t.getProducto().getId());
            fact.setProductoNombre(t.getProducto().getNombre());
        }
        if (t.getPersonaRemitente() != null) {
            fact.setPersonaRemitenteId(t.getPersonaRemitente().getId());
            fact.setPersonaRemitenteNombre(t.getPersonaRemitente().getNombreCompleto());
        }
        if (t.getPersonaBeneficiario() != null) {
            fact.setPersonaBeneficiarioId(t.getPersonaBeneficiario().getId());
            fact.setPersonaBeneficiarioNombre(t.getPersonaBeneficiario().getNombreCompleto());
        }
        fact.setEsInternacional(t.getPaisOrigenRef() != null
                && t.getPaisDestinoRef() != null
                && !t.getPaisOrigenRef().getCodigoIso().equalsIgnoreCase(t.getPaisDestinoRef().getCodigoIso()));
        return fact;
    }
}
