package com.antifraude.common.entity;

import com.antifraude.alerts.Alerta;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "caso_alerta", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"caso_id", "alerta_id"})
})
public class CasoAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caso_id", nullable = false)
    private Caso caso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;
}
