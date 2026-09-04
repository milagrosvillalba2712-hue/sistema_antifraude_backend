package com.antifraude.transactions;

import com.antifraude.common.entity.Moneda;
import com.antifraude.common.entity.NivelRiesgo;
import com.antifraude.common.entity.Pais;
import com.antifraude.common.entity.Persona;
import com.antifraude.common.entity.Producto;
import com.antifraude.common.entity.Canal;
import com.antifraude.common.entity.TipoDocumento;
import com.antifraude.licensing.Empresa;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@IdClass(TransaccionId.class)
@Table(name = "transacciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transacciones_id_seq_generator")
    @SequenceGenerator(
            name = "transacciones_id_seq_generator",
            sequenceName = "transacciones_id_seq",
            allocationSize = 1
    )
    private Long id;

    @Id
    @Column(name = "fecha_transaccion", nullable = false)
    private OffsetDateTime fechaTransaccion;

    @Column(name = "transaction_uuid", nullable = false)
    private UUID transactionUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 60)
    private String codigo;

    @Column(name = "fecha_procesamiento")
    private OffsetDateTime fechaProcesamiento;

    @Column(name = "fecha_liquidacion")
    private OffsetDateTime fechaLiquidacion;

    @Column(length = 30)
    private String estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_evaluacion", length = 30)
    @Builder.Default
    private EstadoEvaluacion estadoEvaluacion = EstadoEvaluacion.PENDIENTE;

    @Builder.Default
    private Boolean procesada = false;

    @Column(name = "tipo_transaccion_id", nullable = false)
    private Long tipoTransaccionId;

    @Column(name = "canal_transaccion_id", nullable = false)
    private Long canalTransaccionId;

    @Column(name = "infraestructura_pago", nullable = false, length = 30)
    private String infraestructuraPago;

    @Column(name = "modulo_sipap", length = 30)
    private String moduloSipap;

    @Column(name = "subtipo_transaccion", length = 60)
    private String subtipoTransaccion;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda monedaRef;

    @Column(name = "monto_destino", precision = 18, scale = 2)
    private BigDecimal montoDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moneda_destino_id")
    private Moneda monedaDestinoRef;

    @Column(name = "tipo_cambio", precision = 18, scale = 8)
    private BigDecimal tipoCambio;

    @Column(precision = 18, scale = 2)
    private BigDecimal comision;

    @Column(precision = 18, scale = 2)
    private BigDecimal impuesto;

    @Column(name = "monto_total_debitado", precision = 18, scale = 2)
    private BigDecimal montoTotalDebitado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_remitente_id")
    private Persona personaRemitente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_beneficiario_id")
    private Persona personaBeneficiario;

    @Column(name = "nombre_remitente", length = 180)
    private String nombreRemitente;

    @Column(name = "nombre_beneficiario", length = 180)
    private String nombreBeneficiario;

    @Column(name = "remitente_nombre_completo", nullable = false, length = 220)
    private String remitenteNombreCompleto;

    @Column(name = "beneficiario_nombre_completo", nullable = false, length = 220)
    private String beneficiarioNombreCompleto;

    @Column(name = "documento_remitente_enc")
    private byte[] documentoRemitenteEnc;

    @Column(name = "documento_remitente_hash")
    private byte[] documentoRemitenteHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_remitente_id", nullable = false)
    private TipoDocumento tipoDocumentoRemitente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_emisor_documento_remitente_id", nullable = false)
    private Pais paisEmisorDocumentoRemitente;

    @Column(name = "documento_beneficiario_enc")
    private byte[] documentoBeneficiarioEnc;

    @Column(name = "documento_beneficiario_hash")
    private byte[] documentoBeneficiarioHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_beneficiario_id", nullable = false)
    private TipoDocumento tipoDocumentoBeneficiario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_emisor_documento_beneficiario_id", nullable = false)
    private Pais paisEmisorDocumentoBeneficiario;

    @Column(name = "cuenta_origen_enc")
    private byte[] cuentaOrigenEnc;

    @Column(name = "cuenta_origen_hash")
    private byte[] cuentaOrigenHash;

    @Column(name = "cuenta_destino_enc")
    private byte[] cuentaDestinoEnc;

    @Column(name = "cuenta_destino_hash")
    private byte[] cuentaDestinoHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_origen_id")
    private Pais paisOrigenRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_destino_id")
    private Pais paisDestinoRef;

    @Column(name = "end_to_end_id", length = 80)
    private String endToEndId;

    @Column(name = "spi_reference", length = 80)
    private String spiReference;

    @Column(name = "numero_comprobante", length = 80)
    private String numeroComprobante;

    @Column(name = "requiere_declaracion_fondos")
    @Builder.Default
    private Boolean requiereDeclaracionFondos = false;

    @Column(name = "depositante_tercero")
    @Builder.Default
    private Boolean depositanteTercero = false;

    @Column(name = "terminal_id", length = 80)
    private String terminalId;

    @Column(name = "sucursal_codigo", length = 60)
    private String sucursalCodigo;

    @Column(name = "mcc", length = 4)
    private String mcc;

    @Column(name = "nombre_comercio", length = 180)
    private String nombreComercio;

    @Column(name = "pan_last4", length = 4)
    private String panLast4;

    @Column(name = "remesadora_id", length = 80)
    private String remesadoraId;

    @Column(name = "swift_bic_origen", length = 11)
    private String swiftBicOrigen;

    @Column(name = "swift_bic_destino", length = 11)
    private String swiftBicDestino;

    @Column(name = "entidad_origen_tipo", length = 40)
    private String entidadOrigenTipo;

    @Column(name = "entidad_origen_codigo", length = 80)
    private String entidadOrigenCodigo;

    @Column(name = "entidad_origen_nombre", length = 180)
    private String entidadOrigenNombre;

    @Column(name = "entidad_destino_tipo", length = 40)
    private String entidadDestinoTipo;

    @Column(name = "entidad_destino_codigo", length = 80)
    private String entidadDestinoCodigo;

    @Column(name = "entidad_destino_nombre", length = 180)
    private String entidadDestinoNombre;

    @Column(name = "referencia_externa", length = 120)
    private String referenciaExterna;

    @Column(name = "score_riesgo", precision = 8, scale = 2)
    private BigDecimal scoreRiesgo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_riesgo_id")
    private NivelRiesgo nivelRiesgo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_especificos", columnDefinition = "jsonb")
    private String datosEspecificos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "riesgo_paraguay_json", columnDefinition = "jsonb")
    private String riesgoParaguayJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "screening_result_json", columnDefinition = "jsonb")
    private String screeningResultJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reglas_disparadas_json", columnDefinition = "jsonb")
    private String reglasDisparadasJson;

    @Transient
    private String identificadorDocumento;

    @Transient
    private String documentoBeneficiario;

    @Transient
    private String cuentaOrigen;

    @Transient
    private String cuentaDestino;

    @Transient
    private String moneda;

    @Transient
    private String canal;

    @Transient
    private Canal canalRef;

    @Transient
    private String tipoTransaccion;

    @Transient
    private String ipOrigen;

    @Transient
    private String paisOrigen;

    @Transient
    private Producto producto;

    public enum EstadoEvaluacion {
        PENDIENTE, EN_PROCESO, EVALUADA, APROBADA, RECHAZADA, REVISION_MANUAL, SOSPECHOSA
    }

    public String getIdentificadorDocumento() {
        if (identificadorDocumento != null) return identificadorDocumento;
        return null;
    }

    public String getDocumentoRemitente() {
        return identificadorDocumento;
    }

    public String getTipoDocumentoRemitenteCodigo() {
        return tipoDocumentoRemitente != null ? tipoDocumentoRemitente.getCodigo() : null;
    }

    public String getTipoDocumentoBeneficiarioCodigo() {
        return tipoDocumentoBeneficiario != null ? tipoDocumentoBeneficiario.getCodigo() : null;
    }

    public String getPaisEmisorDocumentoRemitenteCodigo() {
        return paisEmisorDocumentoRemitente != null ? paisEmisorDocumentoRemitente.getCodigoIso() : null;
    }

    public String getPaisEmisorDocumentoBeneficiarioCodigo() {
        return paisEmisorDocumentoBeneficiario != null ? paisEmisorDocumentoBeneficiario.getCodigoIso() : null;
    }

    public String getIdentificadorDocumentoEnmascarado() {
        return maskDocument(identificadorDocumento, documentoRemitenteHash);
    }

    public String getDocumentoBeneficiarioEnmascarado() {
        return maskDocument(documentoBeneficiario, documentoBeneficiarioHash);
    }

    public String getCuentaOrigen() {
        return cuentaOrigen != null ? cuentaOrigen : masked(cuentaOrigenHash, "Cuenta Protegida");
    }

    public String getCuentaDestino() {
        return cuentaDestino != null ? cuentaDestino : masked(cuentaDestinoHash, "Cuenta Protegida");
    }

    public String getMoneda() {
        return moneda != null ? moneda : (monedaRef != null ? monedaRef.getCodigoIso() : null);
    }

    public String getCanal() {
        return canal != null ? canal : infraestructuraPago;
    }

    public String getTipoTransaccion() {
        return tipoTransaccion != null ? tipoTransaccion : subtipoTransaccion;
    }

    public String getPaisOrigen() {
        return paisOrigen != null ? paisOrigen : (paisOrigenRef != null ? paisOrigenRef.getCodigoIso() : null);
    }

    private String masked(byte[] hash, String label) {
        return hash == null ? null : label;
    }

    private String maskDocument(String value, byte[] hash) {
        if (value != null && !value.isBlank()) {
            String digits = value.replaceAll("\\D", "");
            if (digits.length() >= 4) {
                return "***" + digits.substring(digits.length() - 4);
            }
            return "***";
        }
        return hash == null ? null : "Documento Protegido";
    }
}
