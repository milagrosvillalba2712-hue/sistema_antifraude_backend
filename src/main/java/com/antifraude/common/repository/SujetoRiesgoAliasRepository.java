package com.antifraude.common.repository;

import com.antifraude.common.entity.SujetoRiesgoAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SujetoRiesgoAliasRepository extends JpaRepository<SujetoRiesgoAlias, Long> {
    List<SujetoRiesgoAlias> findByAliasNormalizadoAndActivoTrue(String aliasNormalizado);
}
