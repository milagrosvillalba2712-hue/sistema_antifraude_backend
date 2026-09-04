package com.antifraude.lists;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public final class ListaControlDtos {
    private ListaControlDtos() {}

    public record ListaControlRequest(
            @NotNull ListaControlCliente.TipoListaControl tipoLista,
            @NotBlank String codigo,
            @NotBlank String nombre,
            String descripcion,
            ListaControlCliente.EstadoListaControl estado,
            Integer prioridad,
            LocalDate fechaVigenciaDesde,
            LocalDate fechaVigenciaHasta) {}

    public record ElementoControlRequest(
            ElementoListaControlCliente.TipoEntidadControl tipoEntidad,
            @NotNull ElementoListaControlCliente.TipoIdentificadorControl tipoIdentificador,
            @NotBlank String valor,
            String nombreMostrado,
            String documentoMostrado,
            String paisCodigo,
            String tipoDocumentoCodigo,
            String motivo,
            String observacion,
            String fuente,
            String severidad,
            ElementoListaControlCliente.EstadoElementoControl estado,
            LocalDate fechaVigenciaDesde,
            LocalDate fechaVigenciaHasta) {}

    public record ListaControlResponse(
            Long id,
            String tipoLista,
            String codigo,
            String nombre,
            String descripcion,
            String estado,
            Integer prioridad,
            LocalDate fechaVigenciaDesde,
            LocalDate fechaVigenciaHasta,
            Integer totalElementos) {}

    public record ElementoControlResponse(
            Long id,
            Long listaId,
            String tipoLista,
            String tipoEntidad,
            String tipoIdentificador,
            String valorOriginal,
            String valorNormalizado,
            String valorMostrado,
            String nombreMostrado,
            String documentoMostrado,
            String paisCodigo,
            String paisNombre,
            String tipoDocumentoCodigo,
            String tipoDocumentoNombre,
            String motivo,
            String observacion,
            String fuente,
            String severidad,
            String estado,
            LocalDate fechaVigenciaDesde,
            LocalDate fechaVigenciaHasta) {}

    public record ImportacionListaControlResponse(
            Long id,
            Long listaId,
            String nombreArchivo,
            String tipoArchivo,
            String estado,
            Integer totalRegistros,
            Integer registrosValidos,
            Integer registrosInvalidos,
            String erroresJson) {}

    public record ImportPreview(List<String> columnasEsperadas) {}
}
