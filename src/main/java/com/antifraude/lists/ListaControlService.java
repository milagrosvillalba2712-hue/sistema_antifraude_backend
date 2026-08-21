package com.antifraude.lists;

import com.antifraude.audit.AuditoriaService;
import com.antifraude.exception.ResourceNotFoundException;
import com.antifraude.security.crypto.HmacHashService;
import com.antifraude.security.tenant.TenantContext;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

@Service
@Transactional
public class ListaControlService {

    private static final List<String> COLUMNAS_IMPORTACION = List.of(
            "tipoEntidad", "tipoIdentificador", "valor", "nombreMostrado", "documentoMostrado",
            "motivo", "observacion", "fuente", "severidad");

    private final ListaControlClienteRepository listaRepository;
    private final ElementoListaControlClienteRepository elementoRepository;
    private final ImportacionListaControlClienteRepository importacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final HmacHashService hmacHashService;
    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;

    public ListaControlService(ListaControlClienteRepository listaRepository,
                               ElementoListaControlClienteRepository elementoRepository,
                               ImportacionListaControlClienteRepository importacionRepository,
                               UsuarioRepository usuarioRepository,
                               HmacHashService hmacHashService,
                               AuditoriaService auditoriaService,
                               ObjectMapper objectMapper) {
        this.listaRepository = listaRepository;
        this.elementoRepository = elementoRepository;
        this.importacionRepository = importacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.hmacHashService = hmacHashService;
        this.auditoriaService = auditoriaService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ListaControlDtos.ListaControlResponse> listar() {
        UUID empresaId = requireEmpresa();
        return listaRepository.findByEmpresaIdOrderByTipoListaAscNombreAsc(empresaId).stream()
                .map(lista -> toResponse(lista, elementoRepository.findByListaIdAndEmpresaIdOrderByIdDesc(lista.getId(), empresaId).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListaControlDtos.ElementoControlResponse> listarElementos(Long listaId) {
        UUID empresaId = requireEmpresa();
        requireLista(listaId, empresaId);
        return elementoRepository.findByListaIdAndEmpresaIdOrderByIdDesc(listaId, empresaId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ListaControlDtos.ListaControlResponse crearLista(ListaControlDtos.ListaControlRequest request) {
        ListaControlCliente lista = new ListaControlCliente();
        aplicar(lista, request);
        lista = listaRepository.save(lista);
        auditoriaService.registrar(currentUserId(), "CREAR_LISTA_CONTROL",
                "Creacion de " + lista.getTipoLista() + " " + lista.getCodigo(), null,
                "lista_control_cliente", lista.getId());
        return toResponse(lista, 0);
    }

    public ListaControlDtos.ListaControlResponse actualizarLista(Long id, ListaControlDtos.ListaControlRequest request) {
        UUID empresaId = requireEmpresa();
        ListaControlCliente lista = requireLista(id, empresaId);
        aplicar(lista, request);
        lista = listaRepository.save(lista);
        auditoriaService.registrar(currentUserId(), "EDITAR_LISTA_CONTROL",
                "Edicion de " + lista.getTipoLista() + " " + lista.getCodigo(), null,
                "lista_control_cliente", lista.getId());
        return toResponse(lista, elementoRepository.findByListaIdAndEmpresaIdOrderByIdDesc(lista.getId(), empresaId).size());
    }

    public ListaControlDtos.ElementoControlResponse crearElemento(Long listaId, ListaControlDtos.ElementoControlRequest request) {
        UUID empresaId = requireEmpresa();
        ListaControlCliente lista = requireLista(listaId, empresaId);
        ElementoListaControlCliente elemento = new ElementoListaControlCliente();
        elemento.setLista(lista);
        aplicar(elemento, request);
        elemento = elementoRepository.save(elemento);
        auditoriaService.registrar(currentUserId(), "CREAR_ELEMENTO_LISTA_CONTROL",
                "Alta de elemento en " + lista.getCodigo(), null, "elemento_lista_control_cliente", elemento.getId());
        return toResponse(elemento);
    }

    public ListaControlDtos.ImportacionListaControlResponse importar(Long listaId, MultipartFile archivo) {
        UUID empresaId = requireEmpresa();
        ListaControlCliente lista = requireLista(listaId, empresaId);
        ImportacionListaControlCliente log = new ImportacionListaControlCliente();
        log.setLista(lista);
        log.setNombreArchivo(archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo");
        log.setTipoArchivo(extension(log.getNombreArchivo()));
        List<Map<String, Object>> errores = new ArrayList<>();
        int total = 0;
        int validos = 0;
        try {
            List<Map<String, String>> rows = parseRows(archivo);
            total = rows.size();
            for (int i = 0; i < rows.size(); i++) {
                Map<String, String> row = rows.get(i);
                try {
                    ElementoListaControlCliente elemento = new ElementoListaControlCliente();
                    elemento.setLista(lista);
                    aplicar(elemento, fromRow(row));
                    elementoRepository.save(elemento);
                    validos++;
                } catch (Exception ex) {
                    errores.add(Map.of("fila", i + 2, "mensaje", ex.getMessage()));
                }
            }
            log.setEstado(errores.isEmpty()
                    ? ImportacionListaControlCliente.EstadoImportacion.PROCESADA
                    : ImportacionListaControlCliente.EstadoImportacion.PROCESADA_CON_ERRORES);
        } catch (Exception ex) {
            errores.add(Map.of("fila", 0, "mensaje", ex.getMessage()));
            log.setEstado(ImportacionListaControlCliente.EstadoImportacion.RECHAZADA);
        }
        log.setTotalRegistros(total);
        log.setRegistrosValidos(validos);
        log.setRegistrosInvalidos(Math.max(0, total - validos));
        log.setErroresJson(toJson(errores));
        log = importacionRepository.save(log);
        auditoriaService.registrar(currentUserId(), "IMPORTAR_LISTA_CONTROL",
                "Importacion " + log.getEstado() + " de " + log.getNombreArchivo(), null,
                "importacion_lista_control_cliente", log.getId());
        return toResponse(log);
    }

    @Transactional(readOnly = true)
    public List<ElementoListaControlCliente> buscarCoincidencias(UUID empresaId, Map<ElementoListaControlCliente.TipoIdentificadorControl, String> valores) {
        if (empresaId == null || valores.isEmpty()) {
            return List.of();
        }
        List<ElementoListaControlCliente.TipoIdentificadorControl> tipos = new ArrayList<>();
        List<String> normalizados = new ArrayList<>();
        valores.forEach((tipo, valor) -> {
            String normalized = normalize(valor);
            if (normalized != null) {
                tipos.add(tipo);
                normalizados.add(normalized);
            }
        });
        if (tipos.isEmpty() || normalizados.isEmpty()) {
            return List.of();
        }
        return elementoRepository.buscarCoincidenciasActivas(empresaId, tipos, normalizados);
    }

    public List<String> columnasImportacion() {
        return COLUMNAS_IMPORTACION;
    }

    private void aplicar(ListaControlCliente lista, ListaControlDtos.ListaControlRequest request) {
        lista.setTipoLista(request.tipoLista());
        lista.setCodigo(request.codigo().trim().toUpperCase(Locale.ROOT));
        lista.setNombre(request.nombre().trim());
        lista.setDescripcion(request.descripcion());
        lista.setEstado(request.estado() != null ? request.estado() : ListaControlCliente.EstadoListaControl.ACTIVA);
        lista.setPrioridad(request.prioridad() != null ? request.prioridad() : 50);
        lista.setFechaVigenciaDesde(request.fechaVigenciaDesde());
        lista.setFechaVigenciaHasta(request.fechaVigenciaHasta());
        Usuario user = currentUser();
        if (lista.getUsuarioCreacion() == null) lista.setUsuarioCreacion(user);
        lista.setUsuarioModificacion(user);
    }

    private void aplicar(ElementoListaControlCliente elemento, ListaControlDtos.ElementoControlRequest request) {
        String valor = requireText(request.valor(), "valor");
        elemento.setTipoEntidad(request.tipoEntidad() != null ? request.tipoEntidad() : ElementoListaControlCliente.TipoEntidadControl.PERSONA);
        elemento.setTipoIdentificador(request.tipoIdentificador());
        elemento.setValorOriginal(valor);
        elemento.setValorNormalizado(requireText(normalize(valor), "valor_normalizado"));
        elemento.setValorHash(hmacHashService.hmacBytes(valor));
        elemento.setNombreMostrado(blankToNull(request.nombreMostrado()));
        elemento.setDocumentoMostrado(blankToNull(request.documentoMostrado()));
        elemento.setMotivo(blankToNull(request.motivo()));
        elemento.setObservacion(blankToNull(request.observacion()));
        elemento.setFuente(blankToNull(request.fuente()) != null ? request.fuente().trim() : "CLIENTE");
        elemento.setSeveridad(blankToNull(request.severidad()) != null ? request.severidad().trim() : defaultSeverity(elemento.getLista()));
        elemento.setEstado(request.estado() != null ? request.estado() : ElementoListaControlCliente.EstadoElementoControl.ACTIVO);
        elemento.setFechaVigenciaDesde(request.fechaVigenciaDesde());
        elemento.setFechaVigenciaHasta(request.fechaVigenciaHasta());
        Usuario user = currentUser();
        if (elemento.getUsuarioCreacion() == null) elemento.setUsuarioCreacion(user);
        elemento.setUsuarioModificacion(user);
    }

    private ListaControlDtos.ElementoControlRequest fromRow(Map<String, String> row) {
        String tipoEntidad = row.getOrDefault("tipoEntidad", "PERSONA");
        String tipoIdentificador = row.getOrDefault("tipoIdentificador", "NOMBRE");
        return new ListaControlDtos.ElementoControlRequest(
                ElementoListaControlCliente.TipoEntidadControl.valueOf(tipoEntidad.trim().toUpperCase(Locale.ROOT)),
                ElementoListaControlCliente.TipoIdentificadorControl.valueOf(tipoIdentificador.trim().toUpperCase(Locale.ROOT)),
                row.get("valor"), row.get("nombreMostrado"), row.get("documentoMostrado"), row.get("motivo"),
                row.get("observacion"), row.get("fuente"), row.get("severidad"), null, null, null);
    }

    private List<Map<String, String>> parseRows(MultipartFile archivo) throws Exception {
        String ext = extension(archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "");
        if ("csv".equals(ext)) {
            return parseCsv(archivo);
        }
        if ("xlsx".equals(ext)) {
            return parseXlsx(archivo);
        }
        throw new IllegalArgumentException("Formato no soportado. Use CSV o XLSX.");
    }

    private List<Map<String, String>> parseCsv(MultipartFile archivo) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("El archivo no tiene cabecera.");
            List<String> headers = splitCsv(headerLine);
            validateHeaders(headers);
            List<Map<String, String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> values = splitCsv(line);
                rows.add(row(headers, values));
            }
            return rows;
        }
    }

    private List<Map<String, String>> parseXlsx(MultipartFile archivo) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) throw new IllegalArgumentException("El archivo no tiene filas.");
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) headers.add(cell.getStringCellValue());
            validateHeaders(headers);
            List<Map<String, String>> rows = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, String> values = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    values.put(headers.get(c), formatter.formatCellValue(row.getCell(c)));
                }
                rows.add(values);
            }
            return rows;
        }
    }

    private void validateHeaders(List<String> headers) {
        if (!headers.contains("tipoIdentificador") || !headers.contains("valor")) {
            throw new IllegalArgumentException("La cabecera debe incluir tipoIdentificador y valor.");
        }
    }

    private List<String> splitCsv(String line) {
        return Arrays.stream(line.split(",", -1)).map(String::trim).toList();
    }

    private Map<String, String> row(List<String> headers, List<String> values) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i), i < values.size() ? values.get(i) : "");
        }
        return row;
    }

    public String normalize(String value) {
        if (value == null) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{Alnum}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private UUID requireEmpresa() {
        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) throw new IllegalStateException("No existe empresa en contexto.");
        return empresaId;
    }

    private ListaControlCliente requireLista(Long id, UUID empresaId) {
        return listaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("ListaControlCliente", "id", id));
    }

    private Usuario currentUser() {
        UUID usuarioId = currentUserId();
        return usuarioId != null ? usuarioRepository.findById(usuarioId).orElse(null) : null;
    }

    private UUID currentUserId() {
        return TenantContext.getUsuarioId();
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).trim().toLowerCase(Locale.ROOT) : "";
    }

    private String requireText(String text, String field) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Campo obligatorio: " + field);
        return text.trim();
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String defaultSeverity(ListaControlCliente lista) {
        return lista != null && lista.getTipoLista() == ListaControlCliente.TipoListaControl.WHITELIST ? "Baja" : "Crítica";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private ListaControlDtos.ListaControlResponse toResponse(ListaControlCliente lista, int totalElementos) {
        return new ListaControlDtos.ListaControlResponse(lista.getId(), lista.getTipoLista().name(), lista.getCodigo(),
                lista.getNombre(), lista.getDescripcion(), lista.getEstado().name(), lista.getPrioridad(),
                lista.getFechaVigenciaDesde(), lista.getFechaVigenciaHasta(), totalElementos);
    }

    private ListaControlDtos.ElementoControlResponse toResponse(ElementoListaControlCliente e) {
        return new ListaControlDtos.ElementoControlResponse(e.getId(), e.getLista().getId(), e.getLista().getTipoLista().name(),
                e.getTipoEntidad().name(), e.getTipoIdentificador().name(), e.getValorOriginal(), e.getValorNormalizado(),
                e.getNombreMostrado(), e.getDocumentoMostrado(), e.getMotivo(), e.getObservacion(), e.getFuente(),
                e.getSeveridad(), e.getEstado().name(), e.getFechaVigenciaDesde(), e.getFechaVigenciaHasta());
    }

    private ListaControlDtos.ImportacionListaControlResponse toResponse(ImportacionListaControlCliente i) {
        return new ListaControlDtos.ImportacionListaControlResponse(i.getId(), i.getLista() != null ? i.getLista().getId() : null,
                i.getNombreArchivo(), i.getTipoArchivo(), i.getEstado().name(), i.getTotalRegistros(),
                i.getRegistrosValidos(), i.getRegistrosInvalidos(), i.getErroresJson());
    }
}

