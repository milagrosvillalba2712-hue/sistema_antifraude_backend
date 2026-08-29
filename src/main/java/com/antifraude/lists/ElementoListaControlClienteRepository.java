package com.antifraude.lists;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ElementoListaControlClienteRepository extends JpaRepository<ElementoListaControlCliente, Long> {
    List<ElementoListaControlCliente> findByListaIdAndEmpresaIdOrderByIdDesc(Long listaId, UUID empresaId);

    @Query("""
            select e from ElementoListaControlCliente e
            where e.empresaId = :empresaId
              and e.tipoIdentificador in :tipos
              and e.valorNormalizado in :valores
              and e.estado = com.antifraude.lists.ElementoListaControlCliente$EstadoElementoControl.ACTIVO
              and e.lista.estado = com.antifraude.lists.ListaControlCliente$EstadoListaControl.ACTIVA
              and (e.fechaVigenciaDesde is null or e.fechaVigenciaDesde <= CURRENT_DATE)
              and (e.fechaVigenciaHasta is null or e.fechaVigenciaHasta >= CURRENT_DATE)
              and (e.lista.fechaVigenciaDesde is null or e.lista.fechaVigenciaDesde <= CURRENT_DATE)
              and (e.lista.fechaVigenciaHasta is null or e.lista.fechaVigenciaHasta >= CURRENT_DATE)
            """)
    List<ElementoListaControlCliente> buscarCoincidenciasActivas(@Param("empresaId") UUID empresaId,
                                                                  @Param("tipos") Collection<ElementoListaControlCliente.TipoIdentificadorControl> tipos,
                                                                  @Param("valores") Collection<String> valores);

    @Query("""
            select e from ElementoListaControlCliente e
            where e.empresaId = :empresaId
              and e.tipoIdentificador = :tipo
              and e.estado = com.antifraude.lists.ElementoListaControlCliente$EstadoElementoControl.ACTIVO
              and e.lista.estado = com.antifraude.lists.ListaControlCliente$EstadoListaControl.ACTIVA
              and e.lista.tipoLista <> com.antifraude.lists.ListaControlCliente$TipoListaControl.WHITELIST
              and (e.fechaVigenciaDesde is null or e.fechaVigenciaDesde <= CURRENT_DATE)
              and (e.fechaVigenciaHasta is null or e.fechaVigenciaHasta >= CURRENT_DATE)
              and (e.lista.fechaVigenciaDesde is null or e.lista.fechaVigenciaDesde <= CURRENT_DATE)
              and (e.lista.fechaVigenciaHasta is null or e.lista.fechaVigenciaHasta >= CURRENT_DATE)
            """)
    List<ElementoListaControlCliente> buscarActivosNoWhitelist(@Param("empresaId") UUID empresaId,
                                                               @Param("tipo") ElementoListaControlCliente.TipoIdentificadorControl tipo);
}
