package com.antifraude.drools.fact;

import java.math.BigDecimal;

/**
 * Fact de Control de Importe para Drools.
 */
public class ControlImporteFact {

    private Long controlId;
    private String productoCodigo;
    private String tipoTransaccion;
    private BigDecimal montoMaximo;
    private BigDecimal porcentajeAlerta; // e.g. 0.8 = alerta si monto > 80% del máximo

    public Long getControlId() { return controlId; }
    public void setControlId(Long controlId) { this.controlId = controlId; }
    public String getProductoCodigo() { return productoCodigo; }
    public void setProductoCodigo(String productoCodigo) { this.productoCodigo = productoCodigo; }
    public String getTipoTransaccion() { return tipoTransaccion; }
    public void setTipoTransaccion(String tipoTransaccion) { this.tipoTransaccion = tipoTransaccion; }
    public BigDecimal getMontoMaximo() { return montoMaximo; }
    public void setMontoMaximo(BigDecimal montoMaximo) { this.montoMaximo = montoMaximo; }
    public BigDecimal getPorcentajeAlerta() { return porcentajeAlerta; }
    public void setPorcentajeAlerta(BigDecimal porcentajeAlerta) { this.porcentajeAlerta = porcentajeAlerta; }
}
