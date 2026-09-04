package com.antifraude.drools;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parametros de scoring del motor Drools, cargados desde la tabla
 * configuracion_drools. Se precargan en el RiskContext para que las reglas
 * de dominio (DRLs) y el calculo de nivel/severidad no dependan de valores
 * hardcodeados ni accedan a BD.
 */
@Data
@NoArgsConstructor
public class DroolsScoreConfig {

    private BigDecimal pepScore;
    private BigDecimal observadoScore;
    private int horarioRiesgoEmpieza;
    private int horarioRiesgoTermina;
    private BigDecimal horarioScore;
    private BigDecimal paisInternacionalScore;
    private BigDecimal paisAltoRiesgoScore;
    private BigDecimal paisDestinoAltoRiesgoScore;
    private BigDecimal paisDestinoDistintoScore;
    private BigDecimal umbralCritico;
    private BigDecimal umbralAlto;
    private BigDecimal umbralMedio;
}
