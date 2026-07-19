package com.antifraude.common.entity;

import com.antifraude.users.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "servicio_externo")
public class ServicioExterno extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "url_base", nullable = false, length = 300)
    private String urlBase;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs = 5000;

    @Column(nullable = false)
    private Short reintentos = 2;
}
