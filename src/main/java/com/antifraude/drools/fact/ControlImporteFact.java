package com.antifraude.drools.fact;

import java.math.BigDecimal;

/**
 * Fact de Control de Importe para Drools.
 */
public class ControlImporteFact {

    private Long controlId;
    private String productoCodigo;
    private String tipoTransaccion;
    private String monedaCodigo;
    private BigDecimal montoMinimo;
    private BigDecimal montoMaximo;
    private String severidad;
    private BigDecimal porcentajeAlerta; // e.g. 0.8 = alerta si monto > 80% del máximo

    public Long getControlId() { return controlId; }
    public void setControlId(Long controlId) { this.controlId = controlId; }
    public String getProductoCodigo() { return productoCodigo; }
    public void setProductoCodigo(String productoCodigo) { this.productoCodigo = productoCodigo; }
    public String getTipoTransaccion() { return tipoTransaccion; }
    public void setTipoTransaccion(String tipoTransaccion) { this.tipoTransaccion = tipoTransaccion; }
    public String getMonedaCodigo() { return monedaCodigo; }
    public void setMonedaCodigo(String monedaCodigo) { this.monedaCodigo = monedaCodigo; }
    public BigDecimal getMontoMinimo() { return montoMinimo; }
    public void setMontoMinimo(BigDecimal montoMinimo) { this.montoMinimo = montoMinimo; }
    public BigDecimal getMontoMaximo() { return montoMaximo; }
    public void setMontoMaximo(BigDecimal montoMaximo) { this.montoMaximo = montoMaximo; }
    public String getSeveridad() { return severidad; }
    public void setSeveridad(String severidad) { this.severidad = severidad; }
    public BigDecimal getPorcentajeAlerta() { return porcentajeAlerta; }
    public void setPorcentajeAlerta(BigDecimal porcentajeAlerta) { this.porcentajeAlerta = porcentajeAlerta; }
}
