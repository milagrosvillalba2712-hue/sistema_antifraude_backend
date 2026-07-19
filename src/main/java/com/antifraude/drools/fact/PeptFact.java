package com.antifraude.drools.fact;

import java.math.BigDecimal;

/**
 * Fact de Persona Expuesta Políticamente para Drools.
 */
public class PeptFact {

    private Long personaId;
    private String nombreCompleto;
    private String documentoIdentidad;
    private String cargo;
    private String entidad;
    private String paisOrigen;
    private String fuente;
    private BigDecimal scoreConfianza;

    public Long getPersonaId() { return personaId; }
    public void setPersonaId(Long personaId) { this.personaId = personaId; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public void setDocumentoIdentidad(String documentoIdentidad) { this.documentoIdentidad = documentoIdentidad; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public String getPaisOrigen() { return paisOrigen; }
    public void setPaisOrigen(String paisOrigen) { this.paisOrigen = paisOrigen; }
    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }
    public BigDecimal getScoreConfianza() { return scoreConfianza; }
    public void setScoreConfianza(BigDecimal scoreConfianza) { this.scoreConfianza = scoreConfianza; }
}
