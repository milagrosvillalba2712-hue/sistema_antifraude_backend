package com.antifraude.common.repository;

import com.antifraude.common.entity.SujetoRiesgoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SujetoRiesgoDocumentoRepository extends JpaRepository<SujetoRiesgoDocumento, Long> {
    List<SujetoRiesgoDocumento> findByNumeroDocumentoAndActivoTrue(String numeroDocumento);
}
