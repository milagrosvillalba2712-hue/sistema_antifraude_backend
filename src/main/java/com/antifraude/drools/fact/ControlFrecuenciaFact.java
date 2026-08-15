package com.antifraude.drools.fact;

import java.math.BigDecimal;

/**
 * Fact de Control de Frecuencia para Drools.
 */
public class ControlFrecuenciaFact {

    private Long controlFrecuenciaId;
    private String productoCodigo;
    private String tipoTransaccion;
    private int cantidadMaxima;
    private int ventanaHoras; // e.g. 24 = en las últimas 24 horas
    private BigDecimal porcentajeAlerta;

    public Long getControlFrecuenciaId() { return controlFrecuenciaId; }
    public void setControlFrecuenciaId(Long controlFrecuenciaId) { this.controlFrecuenciaId = controlFrecuenciaId; }
    public String getProductoCodigo() { return productoCodigo; }
    public void setProductoCodigo(String productoCodigo) { this.productoCodigo = productoCodigo; }
    public String getTipoTransaccion() { return tipoTransaccion; }
    public void setTipoTransaccion(String tipoTransaccion) { this.tipoTransaccion = tipoTransaccion; }
    public int getCantidadMaxima() { return cantidadMaxima; }
    public void setCantidadMaxima(int cantidadMaxima) { this.cantidadMaxima = cantidadMaxima; }
    public int getVentanaHoras() { return ventanaHoras; }
    public void setVentanaHoras(int ventanaHoras) { this.ventanaHoras = ventanaHoras; }
    public BigDecimal getPorcentajeAlerta() { return porcentajeAlerta; }
    public void setPorcentajeAlerta(BigDecimal porcentajeAlerta) { this.porcentajeAlerta = porcentajeAlerta; }
}
