package com.antifraude.transactions;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "estadisticas_cliente")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadisticasCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identificador_documento", length = 30, unique = true)
    private String identificadorDocumento;

    @Column(name = "monto_promedio_semanal", precision = 18, scale = 2)
    private BigDecimal montoPromedioSemanal;

    @Column(name = "frecuencia_diaria", precision = 10, scale = 2)
    private BigDecimal frecuenciaDiaria;

    @Column(name = "horario_habitual_inicio")
    private LocalTime horarioHabitualInicio;

    @Column(name = "horario_habitual_fin")
    private LocalTime horarioHabitualFin;

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion;
}
