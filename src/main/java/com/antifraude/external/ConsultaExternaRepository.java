package com.antifraude.external;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsultaExternaRepository extends JpaRepository<ConsultaExterna, Long> {

    List<ConsultaExterna> findByDocumentoHash(String documentoHash);

    List<ConsultaExterna> findByTipoConsulta(String tipoConsulta);

    List<ConsultaExterna> findTop50ByEmpresaIdOrderByFechaConsultaDesc(UUID empresaId);
}
