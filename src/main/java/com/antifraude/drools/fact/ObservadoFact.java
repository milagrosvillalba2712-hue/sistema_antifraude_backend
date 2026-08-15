package com.antifraude.drools.fact;

import java.math.BigDecimal;

/**
 * Fact de Cliente Observado para Drools.
 */
public class ObservadoFact {

    private Long clienteId;
    private String nombreCompleto;
    private String documentoIdentidad;
    private String motivo;
    private String fuente;
    private BigDecimal scoreConfianza;

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public void setDocumentoIdentidad(String documentoIdentidad) { this.documentoIdentidad = documentoIdentidad; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }
    public BigDecimal getScoreConfianza() { return scoreConfianza; }
    public void setScoreConfianza(BigDecimal scoreConfianza) { this.scoreConfianza = scoreConfianza; }
}
