package com.antifraude.drools;

import com.antifraude.drools.fact.*;
import com.antifraude.transactions.Transaccion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contexto completo de evaluación de riesgo para Drools.
 * Contiene la transaccion y todos los datos pre-calculados por RiskContextBuilder.
 * Drools NUNCA accede a BD ni APIs — solo consume este objeto.
 */
@Data
@NoArgsConstructor
public class RiskContext {

    private Transaccion transaccion;
    private TransaccionFact transaccionFact;
    private ClienteFact cliente;
    private List<TransaccionFact> historialTransacciones = new ArrayList<>();
    private List<ListaFact> listasNegras = new ArrayList<>();
    private List<ListaFact> listasGrises = new ArrayList<>();
    private List<ListaFact> listasBlancas = new ArrayList<>();
    private List<CoincidenciaListaFact> coincidenciasListas = new ArrayList<>();
    private List<PeptFact> registrosPEP = new ArrayList<>();
    private List<ObservadoFact> registrosObservados = new ArrayList<>();
    private List<HorarioRiesgoFact> horariosRiesgo = new ArrayList<>();
    private List<CalendarioRiesgoFact> calendarioRiesgo = new ArrayList<>();
    private List<ControlImporteFact> controlesImporte = new ArrayList<>();
    private List<ControlFrecuenciaFact> controlesFrecuencia = new ArrayList<>();
    private LocalDateTime fechaHoraActual;
    private boolean remitenteEnLista;
    private boolean beneficiarioEnLista;
    private boolean documentoEnLista;
    private boolean cuentaEnLista;
    private boolean paisOrigenAltoRiesgo;
    private boolean paisDestinoAltoRiesgo;
    private boolean paisOrigenMonitoreado;
    private boolean paisDestinoMonitoreado;
    private boolean canalAltoRiesgo;
    private DroolsScoreConfig config;

}
