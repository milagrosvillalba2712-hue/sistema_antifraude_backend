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
    private String infraestructuraPago;
    private String moduloSipap;
    private String subtipoTransaccion;
    private String endToEndId;
    private String spiReference;
    private String aliasEmisorTipo;
    private String aliasReceptorTipo;
    private boolean requiereDeclaracionFondos;
    private boolean depositanteTercero;
    private String empeOperador;
    private String tipoCheque;
    private String estadoClearing;
    private String procesadoraTarjeta;
    private String mcc;
    private String canalTarjeta;
    private String panLast4;
    private String qrStandard;
    private String qrHubReference;
    private String remittancePayoutMethod;
    private String paisCorredorRemesa;
    private String swiftBicOrigen;
    private String swiftBicDestino;

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
    public String getInfraestructuraPago() { return infraestructuraPago; }
    public void setInfraestructuraPago(String infraestructuraPago) { this.infraestructuraPago = infraestructuraPago; }
    public String getModuloSipap() { return moduloSipap; }
    public void setModuloSipap(String moduloSipap) { this.moduloSipap = moduloSipap; }
    public String getSubtipoTransaccion() { return subtipoTransaccion; }
    public void setSubtipoTransaccion(String subtipoTransaccion) { this.subtipoTransaccion = subtipoTransaccion; }
    public String getEndToEndId() { return endToEndId; }
    public void setEndToEndId(String endToEndId) { this.endToEndId = endToEndId; }
    public String getSpiReference() { return spiReference; }
    public void setSpiReference(String spiReference) { this.spiReference = spiReference; }
    public String getAliasEmisorTipo() { return aliasEmisorTipo; }
    public void setAliasEmisorTipo(String aliasEmisorTipo) { this.aliasEmisorTipo = aliasEmisorTipo; }
    public String getAliasReceptorTipo() { return aliasReceptorTipo; }
    public void setAliasReceptorTipo(String aliasReceptorTipo) { this.aliasReceptorTipo = aliasReceptorTipo; }
    public boolean isRequiereDeclaracionFondos() { return requiereDeclaracionFondos; }
    public void setRequiereDeclaracionFondos(boolean requiereDeclaracionFondos) { this.requiereDeclaracionFondos = requiereDeclaracionFondos; }
    public boolean isDepositanteTercero() { return depositanteTercero; }
    public void setDepositanteTercero(boolean depositanteTercero) { this.depositanteTercero = depositanteTercero; }
    public String getEmpeOperador() { return empeOperador; }
    public void setEmpeOperador(String empeOperador) { this.empeOperador = empeOperador; }
    public String getTipoCheque() { return tipoCheque; }
    public void setTipoCheque(String tipoCheque) { this.tipoCheque = tipoCheque; }
    public String getEstadoClearing() { return estadoClearing; }
    public void setEstadoClearing(String estadoClearing) { this.estadoClearing = estadoClearing; }
    public String getProcesadoraTarjeta() { return procesadoraTarjeta; }
    public void setProcesadoraTarjeta(String procesadoraTarjeta) { this.procesadoraTarjeta = procesadoraTarjeta; }
    public String getMcc() { return mcc; }
    public void setMcc(String mcc) { this.mcc = mcc; }
    public String getCanalTarjeta() { return canalTarjeta; }
    public void setCanalTarjeta(String canalTarjeta) { this.canalTarjeta = canalTarjeta; }
    public String getPanLast4() { return panLast4; }
    public void setPanLast4(String panLast4) { this.panLast4 = panLast4; }
    public String getQrStandard() { return qrStandard; }
    public void setQrStandard(String qrStandard) { this.qrStandard = qrStandard; }
    public String getQrHubReference() { return qrHubReference; }
    public void setQrHubReference(String qrHubReference) { this.qrHubReference = qrHubReference; }
    public String getRemittancePayoutMethod() { return remittancePayoutMethod; }
    public void setRemittancePayoutMethod(String remittancePayoutMethod) { this.remittancePayoutMethod = remittancePayoutMethod; }
    public String getPaisCorredorRemesa() { return paisCorredorRemesa; }
    public void setPaisCorredorRemesa(String paisCorredorRemesa) { this.paisCorredorRemesa = paisCorredorRemesa; }
    public String getSwiftBicOrigen() { return swiftBicOrigen; }
    public void setSwiftBicOrigen(String swiftBicOrigen) { this.swiftBicOrigen = swiftBicOrigen; }
    public String getSwiftBicDestino() { return swiftBicDestino; }
    public void setSwiftBicDestino(String swiftBicDestino) { this.swiftBicDestino = swiftBicDestino; }
}
