package com.antifraude.external;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultaExternaRepository extends JpaRepository<ConsultaExterna, Long> {

    List<ConsultaExterna> findByIdentificadorDocumento(String identificadorDocumento);

    List<ConsultaExterna> findByTipoConsulta(String tipoConsulta);
}
