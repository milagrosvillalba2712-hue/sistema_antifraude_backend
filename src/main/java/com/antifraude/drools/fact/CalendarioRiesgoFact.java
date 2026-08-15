package com.antifraude.drools.fact;

import java.time.LocalDate;

/**
 * Fact de Calendario de Riesgo (feriados, eventos especiales) para Drools.
 */
public class CalendarioRiesgoFact {

    private Long calendarioId;
    private String descripcion;
    private LocalDate fecha;
    private String tipoEvento; // FERIADO, EVENTO_ESPECIAL
    private int porcentajeExtra;
    private String paisCodigo;

    public Long getCalendarioId() { return calendarioId; }
    public void setCalendarioId(Long calendarioId) { this.calendarioId = calendarioId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public int getPorcentajeExtra() { return porcentajeExtra; }
    public void setPorcentajeExtra(int porcentajeExtra) { this.porcentajeExtra = porcentajeExtra; }
    public String getPaisCodigo() { return paisCodigo; }
    public void setPaisCodigo(String paisCodigo) { this.paisCodigo = paisCodigo; }
}
