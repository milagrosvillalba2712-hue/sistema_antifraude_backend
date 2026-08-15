package com.antifraude.drools.fact;

import java.time.LocalTime;

/**
 * Fact de Horario de Riesgo para Drools.
 */
public class HorarioRiesgoFact {

    private Long horarioId;
    private String descripcion;
    private LocalTime horaDesde;
    private LocalTime horaHasta;
    private int porcentajeExtra; // e.g. 150 = 150% del monto normal
    private boolean aplicaFinDeSemana;
    private String diasSemana; // comma-separated: "L,M,MI,J,V"

    public Long getHorarioId() { return horarioId; }
    public void setHorarioId(Long horarioId) { this.horarioId = horarioId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalTime getHoraDesde() { return horaDesde; }
    public void setHoraDesde(LocalTime horaDesde) { this.horaDesde = horaDesde; }
    public LocalTime getHoraHasta() { return horaHasta; }
    public void setHoraHasta(LocalTime horaHasta) { this.horaHasta = horaHasta; }
    public int getPorcentajeExtra() { return porcentajeExtra; }
    public void setPorcentajeExtra(int porcentajeExtra) { this.porcentajeExtra = porcentajeExtra; }
    public boolean isAplicaFinDeSemana() { return aplicaFinDeSemana; }
    public void setAplicaFinDeSemana(boolean aplicaFinDeSemana) { this.aplicaFinDeSemana = aplicaFinDeSemana; }
    public String getDiasSemana() { return diasSemana; }
    public void setDiasSemana(String diasSemana) { this.diasSemana = diasSemana; }
}
