package com.antifraude.licensing;

import com.antifraude.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "uso_suscripcion", uniqueConstraints = {
        @UniqueConstraint(name = "uk_uso_empresa_periodo", columnNames = {"empresa_id", "anio", "mes"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsoSuscripcion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Builder.Default
    private Integer usuariosActivos = 0;

    @Builder.Default
    private Integer transaccionesProcesadas = 0;

    @Builder.Default
    private Integer consultasKyc = 0;

    @Builder.Default
    private Integer alertasGeneradas = 0;

    @Builder.Default
    private Integer reportesGenerados = 0;
}
