package com.antifraude.drools;

import com.antifraude.common.entity.PaisRiesgo;
import com.antifraude.common.entity.SujetoRiesgo;
import com.antifraude.common.entity.SujetoRiesgoAlias;
import com.antifraude.common.entity.SujetoRiesgoDocumento;
import com.antifraude.common.repository.PaisRiesgoRepository;
import com.antifraude.common.repository.SujetoRiesgoAliasRepository;
import com.antifraude.common.repository.SujetoRiesgoDocumentoRepository;
import com.antifraude.common.repository.SujetoRiesgoRepository;
import com.antifraude.drools.fact.CoincidenciaListaFact;
import com.antifraude.drools.similarity.NameSimilarity;
import com.antifraude.lists.ElementoListaControlCliente;
import com.antifraude.lists.ElementoListaControlClienteRepository;
import com.antifraude.lists.ListaControlCliente;
import com.antifraude.lists.ListaControlService;
import com.antifraude.transactions.Transaccion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ListScreeningService {

    private final SujetoRiesgoRepository sujetoRiesgoRepository;
    private final SujetoRiesgoAliasRepository sujetoRiesgoAliasRepository;
    private final SujetoRiesgoDocumentoRepository sujetoRiesgoDocumentoRepository;
    private final PaisRiesgoRepository paisRiesgoRepository;
    private final ListaControlService listaControlService;
    private final ElementoListaControlClienteRepository elementoListaControlClienteRepository;

    public ListScreeningService(SujetoRiesgoRepository sujetoRiesgoRepository,
                                SujetoRiesgoAliasRepository sujetoRiesgoAliasRepository,
                                SujetoRiesgoDocumentoRepository sujetoRiesgoDocumentoRepository,
                                PaisRiesgoRepository paisRiesgoRepository,
                                ListaControlService listaControlService,
                                ElementoListaControlClienteRepository elementoListaControlClienteRepository) {
        this.sujetoRiesgoRepository = sujetoRiesgoRepository;
        this.sujetoRiesgoAliasRepository = sujetoRiesgoAliasRepository;
        this.sujetoRiesgoDocumentoRepository = sujetoRiesgoDocumentoRepository;
        this.paisRiesgoRepository = paisRiesgoRepository;
        this.listaControlService = listaControlService;
        this.elementoListaControlClienteRepository = elementoListaControlClienteRepository;
    }

    @Value("${app.screening.name-fuzzy-threshold:70}")
    private int nameFuzzyThreshold;

    @Value("${app.screening.name-fuzzy-enabled:true}")
    private boolean nameFuzzyEnabled;

    public List<CoincidenciaListaFact> screen(Transaccion transaccion) {
        List<CoincidenciaListaFact> matches = new ArrayList<>();
        if (transaccion == null) {
            return matches;
        }
        addNameMatches(matches, resolveRemitenteNombre(transaccion), "REMITENTE", "NOMBRE");
        addNameMatches(matches, resolveBeneficiarioNombre(transaccion), "BENEFICIARIO", "NOMBRE");
        addDocumentMatches(matches, transaccion.getDocumentoRemitente(), "REMITENTE", "DOCUMENTO",
                transaccion.getPaisEmisorDocumentoRemitenteCodigo(), transaccion.getTipoDocumentoRemitenteCodigo());
        addDocumentMatches(matches, transaccion.getDocumentoBeneficiario(), "BENEFICIARIO", "DOCUMENTO",
                transaccion.getPaisEmisorDocumentoBeneficiarioCodigo(), transaccion.getTipoDocumentoBeneficiarioCodigo());
        addCountryRisk(matches, transaccion.getPaisOrigenRef() != null ? transaccion.getPaisOrigenRef().getCodigoIso() : transaccion.getPaisOrigen(),
                "ORIGEN", "PAIS");
        addCountryRisk(matches, transaccion.getPaisDestinoRef() != null ? transaccion.getPaisDestinoRef().getCodigoIso() : null,
                "DESTINO", "PAIS");
        addClientControlListMatches(matches, transaccion);
        return matches;
    }

    private String resolveRemitenteNombre(Transaccion transaccion) {
        if (transaccion.getRemitenteNombreCompleto() != null && !transaccion.getRemitenteNombreCompleto().isBlank()) {
            return transaccion.getRemitenteNombreCompleto();
        }
        if (transaccion.getPersonaRemitente() != null) {
            return transaccion.getPersonaRemitente().getNombreCompleto();
        }
        return transaccion.getNombreRemitente();
    }

    private String resolveBeneficiarioNombre(Transaccion transaccion) {
        if (transaccion.getBeneficiarioNombreCompleto() != null && !transaccion.getBeneficiarioNombreCompleto().isBlank()) {
            return transaccion.getBeneficiarioNombreCompleto();
        }
        if (transaccion.getPersonaBeneficiario() != null) {
            return transaccion.getPersonaBeneficiario().getNombreCompleto();
        }
        return transaccion.getNombreBeneficiario();
    }

    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{Alnum}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private void addNameMatches(List<CoincidenciaListaFact> matches, String value, String parte, String campo) {
        String normalized = normalize(value);
        if (normalized == null) {
            return;
        }
        Set<Long> sujetosConCoincidencia = new HashSet<>();
        sujetoRiesgoRepository.findByNombreNormalizadoAndActivoTrue(normalized)
                .forEach(s -> {
                    matches.add(matchFromSubject(s, parte, campo, value));
                    sujetosConCoincidencia.add(s.getId());
                });
        sujetoRiesgoAliasRepository.findByAliasNormalizadoAndActivoTrue(normalized)
                .forEach(a -> matches.add(matchFromAlias(a, parte, campo, value)));
        addNameFuzzyMatches(matches, normalized, parte, campo, value, sujetosConCoincidencia);
    }

    private void addNameFuzzyMatches(List<CoincidenciaListaFact> matches, String normalized, String parte,
                                     String campo, String value, Set<Long> sujetosConCoincidencia) {
        if (!nameFuzzyEnabled || nameFuzzyThreshold <= 0) {
            return;
        }
        Map<Long, CoincidenciaListaFact> mejoresPorSujeto = new LinkedHashMap<>();
        acumularFuzzySujetos(mejoresPorSujeto, normalized, parte, campo, value, sujetosConCoincidencia,
                sujetoRiesgoRepository.findByActivoTrue());
        for (SujetoRiesgoAlias alias : sujetoRiesgoAliasRepository.findByActivoTrue()) {
            Long id = alias.getSujetoRiesgo() != null ? alias.getSujetoRiesgo().getId() : null;
            if (id == null || sujetosConCoincidencia.contains(id)) {
                continue;
            }
            double sim = NameSimilarity.similarity(normalized, alias.getAliasNormalizado());
            if (sim < nameFuzzyThreshold) {
                continue;
            }
            CoincidenciaListaFact candidato = matchFromAliasFuzzy(alias, parte, campo, value, (int) Math.round(sim));
            mejoresPorSujeto.merge(id, candidato, (existente, nuevo) ->
                    existente.getScoreMatch().compareTo(nuevo.getScoreMatch()) >= 0 ? existente : nuevo);
        }
        matches.addAll(mejoresPorSujeto.values());
    }

    private void acumularFuzzySujetos(Map<Long, CoincidenciaListaFact> resultados, String normalized, String parte,
                        String campo, String value, Set<Long> sujetosConCoincidencia,
                        List<SujetoRiesgo> sujetos) {
        for (SujetoRiesgo sujeto : sujetos) {
            if (sujetosConCoincidencia.contains(sujeto.getId())) {
                continue;
            }
            double sim = NameSimilarity.similarity(normalized, sujeto.getNombreNormalizado());
            if (sim < nameFuzzyThreshold) {
                continue;
            }
            CoincidenciaListaFact candidato = matchFromSubjectFuzzy(sujeto, parte, campo, value, (int) Math.round(sim));
            resultados.merge(sujeto.getId(), candidato, (existente, nuevo) ->
                    existente.getScoreMatch().compareTo(nuevo.getScoreMatch()) >= 0 ? existente : nuevo);
        }
    }

    private void addDocumentMatches(List<CoincidenciaListaFact> matches, String value, String parte, String campo,
                                    String paisCodigo, String tipoDocumentoCodigo) {
        if (value == null || value.isBlank()) {
            return;
        }
        sujetoRiesgoDocumentoRepository.findByNumeroDocumentoAndActivoTrue(value.trim())
                .stream()
                .filter(d -> documentMetadataMatches(d.getPaisEmision() != null ? d.getPaisEmision().getCodigoIso() : null,
                        d.getTipoDocumento(), paisCodigo, tipoDocumentoCodigo))
                .forEach(d -> matches.add(matchFromDocument(d, parte, campo, value, paisCodigo, tipoDocumentoCodigo)));
    }

    private void addCountryRisk(List<CoincidenciaListaFact> matches, String value, String parte, String campo) {
        if (value == null || value.isBlank()) {
            return;
        }
        String code = value.trim().toUpperCase(Locale.ROOT);
        if (code.length() > 2) {
            return;
        }
        paisRiesgoRepository.findActiveByPaisCodigoIso(code)
                .forEach(p -> matches.add(matchFromCountry(p, parte, campo, code)));
    }

    private void addClientControlListMatches(List<CoincidenciaListaFact> matches, Transaccion transaccion) {
        UUID empresaId = transaccion.getEmpresa() != null ? transaccion.getEmpresa().getId() : null;
        if (empresaId == null) {
            return;
        }
        addClientControlValue(matches, empresaId, ElementoListaControlCliente.TipoIdentificadorControl.NOMBRE,
                resolveRemitenteNombre(transaccion));
        addClientControlValue(matches, empresaId, ElementoListaControlCliente.TipoIdentificadorControl.NOMBRE,
                resolveBeneficiarioNombre(transaccion));
        addClientControlValue(matches, empresaId, ElementoListaControlCliente.TipoIdentificadorControl.DOCUMENTO,
                transaccion.getDocumentoRemitente(), transaccion.getPaisEmisorDocumentoRemitenteCodigo(),
                transaccion.getTipoDocumentoRemitenteCodigo());
        addClientControlValue(matches, empresaId, ElementoListaControlCliente.TipoIdentificadorControl.DOCUMENTO,
                transaccion.getDocumentoBeneficiario(), transaccion.getPaisEmisorDocumentoBeneficiarioCodigo(),
                transaccion.getTipoDocumentoBeneficiarioCodigo());
        addClientControlValue(matches, empresaId, ElementoListaControlCliente.TipoIdentificadorControl.CUENTA, transaccion.getCuentaOrigen());
        addClientControlValue(matches, empresaId, ElementoListaControlCliente.TipoIdentificadorControl.CUENTA, transaccion.getCuentaDestino());
    }

    private void addClientControlValue(List<CoincidenciaListaFact> matches,
                                       UUID empresaId,
                                       ElementoListaControlCliente.TipoIdentificadorControl type,
                                       String value) {
        addClientControlValue(matches, empresaId, type, value, null, null);
    }

    private void addClientControlValue(List<CoincidenciaListaFact> matches,
                                       UUID empresaId,
                                       ElementoListaControlCliente.TipoIdentificadorControl type,
                                       String value,
                                       String paisCodigo,
                                       String tipoDocumentoCodigo) {
        if (value == null || value.isBlank()) {
            return;
        }
        Set<Long> elementosExactos = new HashSet<>();
        listaControlService.buscarCoincidencias(empresaId, Map.of(type, value))
                .stream()
                .filter(e -> type != ElementoListaControlCliente.TipoIdentificadorControl.DOCUMENTO
                        || documentMetadataMatches(
                        e.getPais() != null ? e.getPais().getCodigoIso() : null,
                        e.getTipoDocumento() != null ? e.getTipoDocumento().getCodigo() : null,
                        paisCodigo,
                        tipoDocumentoCodigo))
                .forEach(e -> {
                    matches.add(matchFromClientControl(e, paisCodigo, tipoDocumentoCodigo));
                    elementosExactos.add(e.getId());
                });
        if (!nameFuzzyEnabled || nameFuzzyThreshold <= 0
                || type != ElementoListaControlCliente.TipoIdentificadorControl.NOMBRE) {
            return;
        }
        String normalized = normalize(value);
        if (normalized == null) {
            return;
        }
        for (ElementoListaControlCliente e : elementoListaControlClienteRepository.buscarActivosNoWhitelist(empresaId, type)) {
            if (elementosExactos.contains(e.getId()) || e.getValorNormalizado() == null) {
                continue;
            }
            double sim = NameSimilarity.similarity(normalized, e.getValorNormalizado());
            if (sim < nameFuzzyThreshold) {
                continue;
            }
            matches.add(matchFromClientControlFuzzy(e, (int) Math.round(sim)));
        }
    }

    private CoincidenciaListaFact matchFromSubject(SujetoRiesgo sujeto, String parte, String campo, String value) {
        CoincidenciaListaFact fact = baseSubjectMatch(sujeto, parte, campo, value);
        fact.setDescripcion("Coincidencia exacta por " + campo.toLowerCase(Locale.ROOT)
                + " contra sujeto de riesgo: " + sujeto.getNombreOriginal());
        return fact;
    }

    private CoincidenciaListaFact matchFromAlias(SujetoRiesgoAlias alias, String parte, String campo, String value) {
        CoincidenciaListaFact fact = baseSubjectMatch(alias.getSujetoRiesgo(), parte, campo, value);
        fact.setDescripcion("Coincidencia exacta por alias contra sujeto de riesgo: " + alias.getAliasOriginal());
        return fact;
    }

    private CoincidenciaListaFact matchFromSubjectFuzzy(SujetoRiesgo sujeto, String parte, String campo, String value, int similitud) {
        CoincidenciaListaFact fact = baseSubjectMatch(sujeto, parte, campo, value);
        fact.setScoreMatch(BigDecimal.valueOf(similitud));
        fact.setDescripcion("Coincidencia difusa (" + similitud + "%) por " + campo.toLowerCase(Locale.ROOT)
                + " contra sujeto de riesgo: "
                + (sujeto.getNombreOriginal() != null ? sujeto.getNombreOriginal() : sujeto.getNombreNormalizado()));
        return fact;
    }

    private CoincidenciaListaFact matchFromAliasFuzzy(SujetoRiesgoAlias alias, String parte, String campo, String value, int similitud) {
        CoincidenciaListaFact fact = baseSubjectMatch(alias.getSujetoRiesgo(), parte, campo, value);
        fact.setScoreMatch(BigDecimal.valueOf(similitud));
        fact.setDescripcion("Coincidencia difusa (" + similitud + "%) por alias contra sujeto de riesgo: "
                + alias.getAliasOriginal());
        return fact;
    }

    private CoincidenciaListaFact matchFromDocument(SujetoRiesgoDocumento document, String parte, String campo, String value,
                                                    String paisCodigo, String tipoDocumentoCodigo) {
        CoincidenciaListaFact fact = baseSubjectMatch(document.getSujetoRiesgo(), parte, campo, value);
        fact.setPaisCodigo(paisCodigo);
        fact.setTipoDocumentoCodigo(tipoDocumentoCodigo);
        fact.setDescripcion("Coincidencia exacta por documento contra sujeto de riesgo.");
        return fact;
    }

    private CoincidenciaListaFact matchFromCountry(PaisRiesgo paisRiesgo, String parte, String campo, String value) {
        CoincidenciaListaFact fact = new CoincidenciaListaFact();
        fact.setParteTransaccion(parte);
        fact.setCampoEvaluado(campo);
        fact.setValorEvaluado(value);
        fact.setCategoria("PAIS_RIESGO");
        fact.setTipoSujeto("PAIS");
        fact.setNombreSujeto(paisRiesgo.getPais() != null ? paisRiesgo.getPais().getNombre() : value);
        fact.setListaCodigo(paisRiesgo.getListaRegulatoria() != null ? paisRiesgo.getListaRegulatoria().getCodigo() : null);
        fact.setFuenteCodigo(paisRiesgo.getListaRegulatoria() != null ? paisRiesgo.getListaRegulatoria().getCodigo() : null);
        fact.setSeveridad(paisRiesgo.getNivelRiesgo() != null ? paisRiesgo.getNivelRiesgo().getNombre() : "Alta");
        fact.setScoreMatch(BigDecimal.valueOf(100));
        fact.setDescripcion(paisRiesgo.getMotivo());
        return fact;
    }

    private CoincidenciaListaFact matchFromClientControl(ElementoListaControlCliente elemento) {
        return matchFromClientControl(elemento, null, null);
    }

    private CoincidenciaListaFact matchFromClientControl(ElementoListaControlCliente elemento,
                                                        String paisEvaluado,
                                                        String tipoDocumentoEvaluado) {
        ListaControlCliente lista = elemento.getLista();
        CoincidenciaListaFact fact = new CoincidenciaListaFact();
        fact.setListaCodigo(lista != null ? lista.getCodigo() : "LISTA_CLIENTE");
        fact.setFuenteCodigo("CLIENTE");
        fact.setCategoria(lista != null ? lista.getTipoLista().name() : "LISTA_CLIENTE");
        fact.setTipoSujeto(elemento.getTipoEntidad().name());
        fact.setSeveridad(elemento.getSeveridad());
        fact.setParteTransaccion("CLIENTE");
        fact.setCampoEvaluado(elemento.getTipoIdentificador().name());
        fact.setValorEvaluado(elemento.getValorOriginal());
        fact.setPaisCodigo(elemento.getPais() != null ? elemento.getPais().getCodigoIso() : paisEvaluado);
        fact.setTipoDocumentoCodigo(elemento.getTipoDocumento() != null ? elemento.getTipoDocumento().getCodigo() : tipoDocumentoEvaluado);
        fact.setNombreSujeto(elemento.getNombreMostrado() != null ? elemento.getNombreMostrado() : elemento.getValorOriginal());
        fact.setScoreMatch(BigDecimal.valueOf(lista != null && lista.getTipoLista() == ListaControlCliente.TipoListaControl.WHITELIST ? 100 : 100));
        fact.setDescripcion((lista != null ? lista.getTipoLista().name() : "LISTA") + " propia del cliente: "
                + (elemento.getMotivo() != null ? elemento.getMotivo() : "coincidencia exacta"));
        return fact;
    }

    private CoincidenciaListaFact matchFromClientControlFuzzy(ElementoListaControlCliente elemento, int similitud) {
        CoincidenciaListaFact fact = matchFromClientControl(elemento);
        fact.setScoreMatch(BigDecimal.valueOf(similitud));
        fact.setDescripcion("Coincidencia difusa (" + similitud + "%) en lista propia del cliente: "
                + (elemento.getMotivo() != null ? elemento.getMotivo() : "nombre similar"));
        return fact;
    }

    private CoincidenciaListaFact baseSubjectMatch(SujetoRiesgo sujeto, String parte, String campo, String value) {
        CoincidenciaListaFact fact = new CoincidenciaListaFact();
        fact.setSujetoRiesgoId(sujeto.getId());
        fact.setFuenteCodigo(sujeto.getFuenteDatosRiesgo() != null ? sujeto.getFuenteDatosRiesgo().getCodigo() : null);
        fact.setListaCodigo(sujeto.getListaRegulatoria() != null ? sujeto.getListaRegulatoria().getCodigo() : null);
        fact.setCategoria(sujeto.getCategoria());
        fact.setTipoSujeto(sujeto.getTipoSujeto());
        fact.setSeveridad(sujeto.getSeveridad());
        fact.setParteTransaccion(parte);
        fact.setCampoEvaluado(campo);
        fact.setValorEvaluado(value);
        fact.setNombreSujeto(sujeto.getNombreOriginal() != null ? sujeto.getNombreOriginal()
                : sujeto.getNombreNormalizado());
        fact.setScoreMatch(BigDecimal.valueOf(100));
        return fact;
    }

    private boolean documentMetadataMatches(String storedPais, String storedTipoDocumento,
                                            String evaluatedPais, String evaluatedTipoDocumento) {
        if (storedPais != null && evaluatedPais != null && !storedPais.equalsIgnoreCase(evaluatedPais)) {
            return false;
        }
        if (storedTipoDocumento != null && evaluatedTipoDocumento != null
                && !storedTipoDocumento.equalsIgnoreCase(evaluatedTipoDocumento)) {
            return false;
        }
        return true;
    }
}
