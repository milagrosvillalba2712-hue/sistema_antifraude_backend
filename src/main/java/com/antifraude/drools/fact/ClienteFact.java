package com.antifraude.drools.fact;

import java.time.LocalDate;

/**
 * Fact de cliente/usuario para Drools.
 */
public class ClienteFact {

    private Long usuarioId;
    private String nombre;
    private String email;
    private String documentoIdentidad;
    private String rol;
    private String segmento;
    private boolean esPEP;
    private boolean esObservado;
    private boolean activo;
    private LocalDate fechaCreacion;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public void setDocumentoIdentidad(String documentoIdentidad) { this.documentoIdentidad = documentoIdentidad; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getSegmento() { return segmento; }
    public void setSegmento(String segmento) { this.segmento = segmento; }
    public boolean isEsPEP() { return esPEP; }
    public void setEsPEP(boolean esPEP) { this.esPEP = esPEP; }
    public boolean isEsObservado() { return esObservado; }
    public void setEsObservado(boolean esObservado) { this.esObservado = esObservado; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
