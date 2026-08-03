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
    private final ListScreeningService listScreeningService;

    public RiskContextBuilder(TransaccionRepository transaccionRepository,
                             ClienteObservadoRepository clienteObservadoRepository,
                             ClientePEPRepository clientePEPRepository,
                             ListaRegulatoriaRepository listaRegulatoriaRepository,
                             ElementoListaRepository elementoListaRepository,
                             HorarioRiesgoRepository horarioRiesgoRepository,
                             CalendarioRiesgoRepository calendarioRiesgoRepository,
                             ControlImporteRepository controlImporteRepository,
                             ControlFrecuenciaRepository controlFrecuenciaRepository,
                             ListScreeningService listScreeningService) {
        this.transaccionRepository = transaccionRepository;
        this.clienteObservadoRepository = clienteObservadoRepository;
        this.clientePEPRepository = clientePEPRepository;
        this.listaRegulatoriaRepository = listaRegulatoriaRepository;
        this.elementoListaRepository = elementoListaRepository;
        this.horarioRiesgoRepository = horarioRiesgoRepository;
        this.calendarioRiesgoRepository = calendarioRiesgoRepository;
        this.controlImporteRepository = controlImporteRepository;
        this.controlFrecuenciaRepository = controlFrecuenciaRepository;
        this.listScreeningService = listScreeningService;
    }

    public RiskContext build(Transaccion transaccion) {
        RiskContext context = new RiskContext();
        context.setTransaccion(transaccion);
        context.setTransaccionFact(toTransaccionFact(transaccion));
        context.setFechaHoraActual(LocalDateTime.now());

        String documento = transaccion.getIdentificadorDocumento();

        log.debug("[CTX] Cargando historial para documento: {}", documento);
        List<Transaccion> historial = transaccionRepository.findUltimasPorDocumento(documento);
        context.setHistorialTransacciones(
                historial.stream().map(this::toTransaccionFact).toList());

        log.debug("[CTX] Cargando listas regulatorias");
        cargarListas(context);
        cargarScreeningListas(context, transaccion);

        log.debug("[CTX] Cargando registros PEP y observados para documento: {}", documento);
        cargarPEP(context, documento);
        cargarObservados(context, documento);

        log.debug("[CTX] Cargando horarios y calendario de riesgo");
        cargarHorariosYCalendario(context);

        log.debug("[CTX] Cargando controles de importe y frecuencia");
        cargarControles(context, transaccion);

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
            fact.setTipoLista(c.getCategoria());
            fact.setFuente(c.getFuenteCodigo());
            fact.setNombreCompleto(c.getNombreSujeto());
            fact.setDocumentoIdentidad(c.getValorEvaluado());
            fact.setScoreConfianza(c.getScoreMatch());
            if (isCritical(c)) {
                context.getListasNegras().add(fact);
            } else if (isLowRisk(c)) {
                context.getListasBlancas().add(fact);
            } else {
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
            if (obs.getPersona() != null && documento.equals(obs.getPersona().getId().toString())) {
                ObservadoFact fact = new ObservadoFact();
                fact.setClienteId(obs.getPersona().getId());
                fact.setNombreCompleto(obs.getPersona().getNombreCompleto());
                fact.setDocumentoIdentidad(documento);
                fact.setMotivo(obs.getMotivo() != null ? obs.getMotivo().name() : null);
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
            ControlImporteFact fact = new ControlImporteFact();
            fact.setControlId(ci.getId());
            fact.setProductoCodigo(ci.getProducto() != null ? ci.getProducto().getCodigo() : null);
            fact.setMontoMaximo(ci.getMontoMaximo());
            importeFacts.add(fact);
        }
        context.setControlesImporte(importeFacts);

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

    private TransaccionFact toTransaccionFact(Transaccion t) {
        TransaccionFact fact = new TransaccionFact();
        fact.setId(t.getId());
        fact.setTransactionUuid(t.getTransactionUuid() != null ? t.getTransactionUuid().toString() : null);
        fact.setCodigo(t.getCodigo());
        fact.setIdentificadorDocumento(t.getIdentificadorDocumento());
        fact.setCuentaOrigen(t.getCuentaOrigen());
        fact.setCuentaDestino(t.getCuentaDestino());
        fact.setMonto(t.getMonto());
        fact.setMonedaCodigo(t.getMoneda());
        fact.setCanalCodigo(t.getCanal());
        fact.setTipoTransaccion(t.getTipoTransaccion());
        fact.setInfraestructuraPago(t.getCanal());
        fact.setModuloSipap(t.getCanal() != null && t.getCanal().toUpperCase().contains("SPI") ? "SPI" : null);
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
        fact.setEsInternacional(t.getPaisOrigenRef() != null && !t.getPaisOrigen().equalsIgnoreCase("PRY"));
        return fact;
    }
}
