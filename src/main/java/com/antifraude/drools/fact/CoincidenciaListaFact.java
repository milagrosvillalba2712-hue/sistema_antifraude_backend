package com.antifraude.drools.fact;

import java.math.BigDecimal;

public class CoincidenciaListaFact {

    private Long sujetoRiesgoId;
    private String fuenteCodigo;
    private String listaCodigo;
    private String categoria;
    private String tipoSujeto;
    private String severidad;
    private String parteTransaccion;
    private String campoEvaluado;
    private String valorEvaluado;
    private String paisCodigo;
    private String tipoDocumentoCodigo;
    private String nombreSujeto;
    private String descripcion;
    private BigDecimal scoreMatch = BigDecimal.valueOf(100);

    public Long getSujetoRiesgoId() { return sujetoRiesgoId; }
    public void setSujetoRiesgoId(Long sujetoRiesgoId) { this.sujetoRiesgoId = sujetoRiesgoId; }
    public String getFuenteCodigo() { return fuenteCodigo; }
    public void setFuenteCodigo(String fuenteCodigo) { this.fuenteCodigo = fuenteCodigo; }
    public String getListaCodigo() { return listaCodigo; }
    public void setListaCodigo(String listaCodigo) { this.listaCodigo = listaCodigo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getTipoSujeto() { return tipoSujeto; }
    public void setTipoSujeto(String tipoSujeto) { this.tipoSujeto = tipoSujeto; }
    public String getSeveridad() { return severidad; }
    public void setSeveridad(String severidad) { this.severidad = severidad; }
    public String getParteTransaccion() { return parteTransaccion; }
    public void setParteTransaccion(String parteTransaccion) { this.parteTransaccion = parteTransaccion; }
    public String getCampoEvaluado() { return campoEvaluado; }
    public void setCampoEvaluado(String campoEvaluado) { this.campoEvaluado = campoEvaluado; }
    public String getValorEvaluado() { return valorEvaluado; }
    public void setValorEvaluado(String valorEvaluado) { this.valorEvaluado = valorEvaluado; }
    public String getPaisCodigo() { return paisCodigo; }
    public void setPaisCodigo(String paisCodigo) { this.paisCodigo = paisCodigo; }
    public String getTipoDocumentoCodigo() { return tipoDocumentoCodigo; }
    public void setTipoDocumentoCodigo(String tipoDocumentoCodigo) { this.tipoDocumentoCodigo = tipoDocumentoCodigo; }
    public String getNombreSujeto() { return nombreSujeto; }
    public void setNombreSujeto(String nombreSujeto) { this.nombreSujeto = nombreSujeto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getScoreMatch() { return scoreMatch; }
    public void setScoreMatch(BigDecimal scoreMatch) { this.scoreMatch = scoreMatch; }
}
