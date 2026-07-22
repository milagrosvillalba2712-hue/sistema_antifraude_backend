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
import com.antifraude.transactions.Transaccion;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ListScreeningService {

    private final SujetoRiesgoRepository sujetoRiesgoRepository;
    private final SujetoRiesgoAliasRepository sujetoRiesgoAliasRepository;
    private final SujetoRiesgoDocumentoRepository sujetoRiesgoDocumentoRepository;
    private final PaisRiesgoRepository paisRiesgoRepository;

    public ListScreeningService(SujetoRiesgoRepository sujetoRiesgoRepository,
                                SujetoRiesgoAliasRepository sujetoRiesgoAliasRepository,
                                SujetoRiesgoDocumentoRepository sujetoRiesgoDocumentoRepository,
                                PaisRiesgoRepository paisRiesgoRepository) {
        this.sujetoRiesgoRepository = sujetoRiesgoRepository;
        this.sujetoRiesgoAliasRepository = sujetoRiesgoAliasRepository;
        this.sujetoRiesgoDocumentoRepository = sujetoRiesgoDocumentoRepository;
        this.paisRiesgoRepository = paisRiesgoRepository;
    }

    public List<CoincidenciaListaFact> screen(Transaccion transaccion) {
        List<CoincidenciaListaFact> matches = new ArrayList<>();
        if (transaccion == null) {
            return matches;
        }
        addNameMatches(matches, transaccion.getPersonaRemitente() != null ? transaccion.getPersonaRemitente().getNombreCompleto() : null,
                "REMITENTE", "NOMBRE");
        addNameMatches(matches, transaccion.getPersonaBeneficiario() != null ? transaccion.getPersonaBeneficiario().getNombreCompleto() : null,
                "BENEFICIARIO", "NOMBRE");
        addDocumentMatches(matches, transaccion.getIdentificadorDocumento(), "CLIENTE", "DOCUMENTO");
        addCountryRisk(matches, transaccion.getPaisOrigenRef() != null ? transaccion.getPaisOrigenRef().getCodigoIso() : transaccion.getPaisOrigen(),
                "ORIGEN", "PAIS");
        addCountryRisk(matches, transaccion.getPaisDestinoRef() != null ? transaccion.getPaisDestinoRef().getCodigoIso() : null,
                "DESTINO", "PAIS");
        return matches;
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
        sujetoRiesgoRepository.findByNombreNormalizadoAndActivoTrue(normalized)
                .forEach(s -> matches.add(matchFromSubject(s, parte, campo, value)));
        sujetoRiesgoAliasRepository.findByAliasNormalizadoAndActivoTrue(normalized)
                .forEach(a -> matches.add(matchFromAlias(a, parte, campo, value)));
    }

    private void addDocumentMatches(List<CoincidenciaListaFact> matches, String value, String parte, String campo) {
        if (value == null || value.isBlank()) {
            return;
        }
        sujetoRiesgoDocumentoRepository.findByNumeroDocumentoAndActivoTrue(value.trim())
                .forEach(d -> matches.add(matchFromDocument(d, parte, campo, value)));
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

    private CoincidenciaListaFact matchFromDocument(SujetoRiesgoDocumento document, String parte, String campo, String value) {
        CoincidenciaListaFact fact = baseSubjectMatch(document.getSujetoRiesgo(), parte, campo, value);
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
        fact.setNombreSujeto(sujeto.getNombreOriginal());
        fact.setScoreMatch(BigDecimal.valueOf(100));
        return fact;
    }
}
