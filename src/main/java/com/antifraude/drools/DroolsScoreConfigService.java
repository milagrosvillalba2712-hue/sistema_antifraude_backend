package com.antifraude.drools;

import com.antifraude.common.entity.ConfiguracionDrools;
import com.antifraude.common.repository.ConfiguracionDroolsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carga los parametros de scoring de Drools desde configuracion_drools.
 * Se lee en cada invocacion (sin cache) para que los cambios que el cliente
 * edita via el Rule Engine surtan efecto de inmediato. Ante claves ausentes o
 * valores invalidos se usan los valores por defecto (los mismos que antes
 * estaban hardcodeados en las reglas y en DroolsService).
 */
@Service
public class DroolsScoreConfigService {

    private static final Logger log = LoggerFactory.getLogger(DroolsScoreConfigService.class);

    private final ConfiguracionDroolsRepository repository;

    public DroolsScoreConfigService(ConfiguracionDroolsRepository repository) {
        this.repository = repository;
    }

    public DroolsScoreConfig getConfig() {
        Map<String, String> raw = repository.findAll().stream()
                .collect(Collectors.toMap(ConfiguracionDrools::getClave, ConfiguracionDrools::getValor, (a, b) -> b));
        DroolsScoreConfig c = new DroolsScoreConfig();
        c.setPepScore(bd(raw, "PEP_SCORE", "40"));
        c.setObservadoScore(bd(raw, "OBSERVADO_SCORE", "60"));
        c.setHorarioRiesgoEmpieza(integer(raw, "HORARIO_RIESGO_EMPIEZA", 23));
        c.setHorarioRiesgoTermina(integer(raw, "HORARIO_RIESGO_TERMINA", 5));
        c.setHorarioScore(bd(raw, "HORARIO_SCORE", "15"));
        c.setPaisInternacionalScore(bd(raw, "PAIS_INTERNACIONAL_SCORE", "20"));
        c.setPaisAltoRiesgoScore(bd(raw, "PAIS_ALTO_RIESGO_SCORE", "15"));
        c.setPaisDestinoAltoRiesgoScore(bd(raw, "PAIS_DESTINO_ALTO_RIESGO_SCORE", "15"));
        c.setPaisDestinoDistintoScore(bd(raw, "PAIS_DESTINO_DISTINTO_SCORE", "10"));
        c.setUmbralCritico(bd(raw, "UMBRAL_CRITICO", "70"));
        c.setUmbralAlto(bd(raw, "UMBRAL_ALTO", "50"));
        c.setUmbralMedio(bd(raw, "UMBRAL_MEDIO", "30"));
        return c;
    }

    private static BigDecimal bd(Map<String, String> raw, String clave, String def) {
        String v = raw.getOrDefault(clave, def);
        try {
            return new BigDecimal(v);
        } catch (Exception e) {
            return new BigDecimal(def);
        }
    }

    private static int integer(Map<String, String> raw, String clave, int def) {
        String v = raw.getOrDefault(clave, String.valueOf(def));
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return def;
        }
    }
}
