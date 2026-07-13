package com.antifraude.drools.fact;

import java.math.BigDecimal;

/**
 * Fact de lista regulatoria para Drools (negra, gris, blanca).
 */
public class ListaFact {

    private Long listaId;
    private String nombreLista;
    private String tipoLista; // NEGRA, GRIS, BLANCA
    private String fuente;
    private String paisCodigo;
    private String nombreCompleto;
    private String documentoIdentidad;
    private BigDecimal scoreConfianza;

    public Long getListaId() { return listaId; }
    public void setListaId(Long listaId) { this.listaId = listaId; }
    public String getNombreLista() { return nombreLista; }
    public void setNombreLista(String nombreLista) { this.nombreLista = nombreLista; }
    public String getTipoLista() { return tipoLista; }
    public void setTipoLista(String tipoLista) { this.tipoLista = tipoLista; }
    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }
    public String getPaisCodigo() { return paisCodigo; }
    public void setPaisCodigo(String paisCodigo) { this.paisCodigo = paisCodigo; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public void setDocumentoIdentidad(String documentoIdentidad) { this.documentoIdentidad = documentoIdentidad; }
    public BigDecimal getScoreConfianza() { return scoreConfianza; }
    public void setScoreConfianza(BigDecimal scoreConfianza) { this.scoreConfianza = scoreConfianza; }
}
