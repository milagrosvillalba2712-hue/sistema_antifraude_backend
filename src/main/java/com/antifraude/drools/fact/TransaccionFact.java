package com.antifraude.drools.fact;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fact de transaccion para Drools — POJO plano sin dependencias JPA.
 */
public class TransaccionFact {

    private Long id;
    private String transactionUuid;
    private String codigo;
    private String identificadorDocumento;
    private String cuentaOrigen;
    private String cuentaDestino;
    private BigDecimal monto;
    private String monedaCodigo;
    private String canalCodigo;
    private String tipoTransaccion;
    private String ipOrigen;
    private String paisOrigenCodigo;
    private String paisOrigenNombre;
    private String paisDestinoCodigo;
    private String paisDestinoNombre;
    private LocalDateTime fechaTransaccion;
    private boolean esInternacional;
    private boolean esHorarioInhabil;
    private boolean coincideFeriado;
    private Long productoId;
    private String productoNombre;
    private Long personaRemitenteId;
    private String personaRemitenteNombre;
    private Long personaBeneficiarioId;
    private String personaBeneficiarioNombre;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTransactionUuid() { return transactionUuid; }
    public void setTransactionUuid(String transactionUuid) { this.transactionUuid = transactionUuid; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getIdentificadorDocumento() { return identificadorDocumento; }
    public void setIdentificadorDocumento(String identificadorDocumento) { this.identificadorDocumento = identificadorDocumento; }
    public String getCuentaOrigen() { return cuentaOrigen; }
    public void setCuentaOrigen(String cuentaOrigen) { this.cuentaOrigen = cuentaOrigen; }
    public String getCuentaDestino() { return cuentaDestino; }
    public void setCuentaDestino(String cuentaDestino) { this.cuentaDestino = cuentaDestino; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getMonedaCodigo() { return monedaCodigo; }
    public void setMonedaCodigo(String monedaCodigo) { this.monedaCodigo = monedaCodigo; }
    public String getCanalCodigo() { return canalCodigo; }
    public void setCanalCodigo(String canalCodigo) { this.canalCodigo = canalCodigo; }
    public String getTipoTransaccion() { return tipoTransaccion; }
    public void setTipoTransaccion(String tipoTransaccion) { this.tipoTransaccion = tipoTransaccion; }
    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }
    public String getPaisOrigenCodigo() { return paisOrigenCodigo; }
    public void setPaisOrigenCodigo(String paisOrigenCodigo) { this.paisOrigenCodigo = paisOrigenCodigo; }
    public String getPaisOrigenNombre() { return paisOrigenNombre; }
    public void setPaisOrigenNombre(String paisOrigenNombre) { this.paisOrigenNombre = paisOrigenNombre; }
    public String getPaisDestinoCodigo() { return paisDestinoCodigo; }
    public void setPaisDestinoCodigo(String paisDestinoCodigo) { this.paisDestinoCodigo = paisDestinoCodigo; }
    public String getPaisDestinoNombre() { return paisDestinoNombre; }
    public void setPaisDestinoNombre(String paisDestinoNombre) { this.paisDestinoNombre = paisDestinoNombre; }
    public LocalDateTime getFechaTransaccion() { return fechaTransaccion; }
    public void setFechaTransaccion(LocalDateTime fechaTransaccion) { this.fechaTransaccion = fechaTransaccion; }
    public boolean isEsInternacional() { return esInternacional; }
    public void setEsInternacional(boolean esInternacional) { this.esInternacional = esInternacional; }
    public boolean isEsHorarioInhabil() { return esHorarioInhabil; }
    public void setEsHorarioInhabil(boolean esHorarioInhabil) { this.esHorarioInhabil = esHorarioInhabil; }
    public boolean isCoincideFeriado() { return coincideFeriado; }
    public void setCoincideFeriado(boolean coincideFeriado) { this.coincideFeriado = coincideFeriado; }
    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
    public Long getPersonaRemitenteId() { return personaRemitenteId; }
    public void setPersonaRemitenteId(Long personaRemitenteId) { this.personaRemitenteId = personaRemitenteId; }
    public String getPersonaRemitenteNombre() { return personaRemitenteNombre; }
    public void setPersonaRemitenteNombre(String personaRemitenteNombre) { this.personaRemitenteNombre = personaRemitenteNombre; }
    public Long getPersonaBeneficiarioId() { return personaBeneficiarioId; }
    public void setPersonaBeneficiarioId(Long personaBeneficiarioId) { this.personaBeneficiarioId = personaBeneficiarioId; }
    public String getPersonaBeneficiarioNombre() { return personaBeneficiarioNombre; }
    public void setPersonaBeneficiarioNombre(String personaBeneficiarioNombre) { this.personaBeneficiarioNombre = personaBeneficiarioNombre; }
}
