package com.antifraude.common.repository;

import com.antifraude.common.entity.HorarioLaboralUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioLaboralUsuarioRepository extends JpaRepository<HorarioLaboralUsuario, Long> {
    List<HorarioLaboralUsuario> findByUsuarioId(Long usuarioId);
}
