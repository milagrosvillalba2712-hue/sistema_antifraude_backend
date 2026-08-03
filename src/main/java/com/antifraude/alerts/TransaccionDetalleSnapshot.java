package com.antifraude.alerts;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaccion_detalle_snapshot")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionDetalleSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false, unique = true)
    private Alerta alerta;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle_json", columnDefinition = "jsonb")
    private String detalleJson;

    @Column(name = "fecha_registro")
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
