package com.antifraude.common.repository;

import com.antifraude.common.entity.HorarioLaboralUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HorarioLaboralUsuarioRepository extends JpaRepository<HorarioLaboralUsuario, Long> {
    List<HorarioLaboralUsuario> findByUsuarioId(UUID usuarioId);
}
