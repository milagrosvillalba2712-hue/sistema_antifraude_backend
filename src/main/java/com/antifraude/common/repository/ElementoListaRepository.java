package com.antifraude.common.repository;

import com.antifraude.common.entity.ElementoLista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElementoListaRepository extends JpaRepository<ElementoLista, Long> {
    List<ElementoLista> findByListaRegulatoriaIdAndValorIdentificador(Long listaRegulatoriaId, String valorIdentificador);
    List<ElementoLista> findByListaRegulatoriaId(Long listaRegulatoriaId);
}
