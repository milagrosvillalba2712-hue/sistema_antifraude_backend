package com.antifraude.rules;

import com.antifraude.exception.ResourceNotFoundException;
import com.antifraude.common.repository.EscenarioRepository;
import com.antifraude.dto.ReglaRiesgoRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class ReglaRiesgoService {

    private static final Logger log = LoggerFactory.getLogger(ReglaRiesgoService.class);

    private final ReglaRiesgoRepository reglaRiesgoRepository;
    private final EscenarioRepository escenarioRepository;
    private final ObjectMapper objectMapper;

    public ReglaRiesgoService(ReglaRiesgoRepository reglaRiesgoRepository,
                              EscenarioRepository escenarioRepository,
                              ObjectMapper objectMapper) {
        this.reglaRiesgoRepository = reglaRiesgoRepository;
        this.escenarioRepository = escenarioRepository;
        this.objectMapper = objectMapper;
    }

    public ReglaRiesgo crearDesdeRequest(ReglaRiesgoRequest request, com.antifraude.users.Usuario usuario) {
        ReglaRiesgo regla = new ReglaRiesgo();
        aplicarRequest(regla, request);
        regla.setCreadaPor(usuario);
        return crear(regla);
    }

    public ReglaRiesgo crear(ReglaRiesgo regla) {
        log.info("[RULES] Creando regla: {} - Tipo: {} - Severidad: {}",
                regla.getNombre(), regla.getTipoRegla(), regla.getSeveridad());
        if (regla.getVersion() == null) {
            regla.setVersion(1);
        }
        if (regla.getCodigo() == null || regla.getCodigo().isBlank()) {
            regla.setCodigo(generarCodigo(regla.getNombre(), regla.getVersion()));
        }
        if (regla.getCondicion() == null || regla.getCondicion().isBlank()) {
            regla.setCondicion(generarCondicionLegible(regla));
        }
        if (regla.getEstado() == null || regla.getEstado().isBlank()) {
            regla.setEstado(regla.getActiva() ? "ACTIVA" : "BORRADOR");
        }
        ReglaRiesgo creada = reglaRiesgoRepository.save(regla);
        log.info("[RULES] Regla creada - ID: {} - Nombre: {} - Version: {}",
                creada.getId(), creada.getNombre(), creada.getVersion());
        return creada;
    }

    public List<ReglaRiesgo> listarTodas() {
        log.debug("[RULES] Listando todas las reglas");
        List<ReglaRiesgo> reglas = reglaRiesgoRepository.findAll();
        log.debug("[RULES] Total reglas: {}", reglas.size());
        return reglas;
    }

    public ReglaRiesgo buscarPorId(Long id) {
        log.debug("[RULES] Buscando regla por ID: {}", id);
        return reglaRiesgoRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[RULES] Regla no encontrada con ID: {}", id);
                    return new ResourceNotFoundException("Regla de riesgo", "id", id);
                });
    }

    public List<ReglaRiesgo> listarActivas() {
        log.debug("[RULES] Listando reglas activas");
        return reglaRiesgoRepository.findByActivaTrueAndEstado("ACTIVA");
    }

    public List<ReglaRiesgo> listarPorEscenario(Long escenarioId) {
        log.debug("[RULES] Listando reglas por escenario: {}", escenarioId);
        return reglaRiesgoRepository.findByEscenarioId(escenarioId);
    }

    public ReglaRiesgo actualizar(Long id, ReglaRiesgo actualizada) {
        log.info("[RULES] Actualizando regla ID: {}", id);
        ReglaRiesgo regla = buscarPorId(id);
        regla.setNombre(actualizada.getNombre());
        regla.setDescripcion(actualizada.getDescripcion());
        regla.setTipoRegla(actualizada.getTipoRegla());
        regla.setSeveridad(actualizada.getSeveridad());
        regla.setCondicion(actualizada.getCondicion());
        regla.setActiva(actualizada.getActiva());
        regla.setEscenario(actualizada.getEscenario());
        regla.setScoreBase(actualizada.getScoreBase());
        regla.setParametros(actualizada.getParametros());
        regla.setEstado(actualizada.getEstado());
        regla.setFechaModificacion(OffsetDateTime.now());
        ReglaRiesgo guardada = reglaRiesgoRepository.save(regla);
        log.info("[RULES] Regla actualizada - ID: {} - Nombre: {} - Version: {}",
                id, guardada.getNombre(), guardada.getVersion());
        return guardada;
    }

    public ReglaRiesgo actualizarDesdeRequest(Long id, ReglaRiesgoRequest request) {
        log.info("[RULES] Actualizando regla ID: {}", id);
        ReglaRiesgo regla = buscarPorId(id);
        aplicarRequest(regla, request);
        regla.setFechaModificacion(OffsetDateTime.now());
        return reglaRiesgoRepository.save(regla);
    }

    public ReglaRiesgo crearNuevaVersion(Long reglaId) {
        log.info("[RULES] Creando nueva version de regla ID: {}", reglaId);
        ReglaRiesgo original = buscarPorId(reglaId);

        ReglaRiesgo nuevaVersion = ReglaRiesgo.builder()
                .nombre(original.getNombre())
                .codigo(generarCodigo(original.getCodigo() != null ? original.getCodigo() : original.getNombre(), original.getVersion() + 1))
                .descripcion(original.getDescripcion())
                .tipoRegla(original.getTipoRegla())
                .severidad(original.getSeveridad())
                .prioridad(original.getPrioridad())
                .condicion(original.getCondicion())
                .condicionesJson(original.getCondicionesJson())
                .accionesJson(original.getAccionesJson())
                .escenario(original.getEscenario())
                .scoreBase(original.getScoreBase())
                .parametros(original.getParametros())
                .version(original.getVersion() + 1)
                .versionAnteriorId(original.getId())
                .activa(false)
                .estado("BORRADOR")
                .creadaPor(original.getCreadaPor())
                .build();

        ReglaRiesgo creada = reglaRiesgoRepository.save(nuevaVersion);
        log.info("[RULES] Nueva version creada - ID: {} - Version: {}", creada.getId(), creada.getVersion());
        return creada;
    }

    public void toggleActiva(Long id) {
        log.info("[RULES] Cambiando estado de regla ID: {}", id);
        ReglaRiesgo regla = buscarPorId(id);
        regla.setActiva(!regla.getActiva());
        regla.setFechaModificacion(OffsetDateTime.now());
        reglaRiesgoRepository.save(regla);
        log.info("[RULES] Regla ID: {} - Activa: {}", id, regla.getActiva());
    }

    public void activar(Long id) {
        log.info("[RULES] Activando regla ID: {}", id);
        ReglaRiesgo regla = buscarPorId(id);
        regla.setActiva(true);
        regla.setEstado("ACTIVA");
        regla.setFechaModificacion(OffsetDateTime.now());
        reglaRiesgoRepository.save(regla);
        log.info("[RULES] Regla ID: {} activada", id);
    }

    public void desactivar(Long id) {
        log.info("[RULES] Desactivando regla ID: {}", id);
        ReglaRiesgo regla = buscarPorId(id);
        regla.setActiva(false);
        regla.setEstado("INACTIVA");
        regla.setFechaModificacion(OffsetDateTime.now());
        reglaRiesgoRepository.save(regla);
        log.info("[RULES] Regla ID: {} desactivada", id);
    }

    public List<ReglaRiesgo> listarPorEstado(String estado) {
        log.debug("[RULES] Listando reglas por estado: {}", estado);
        return reglaRiesgoRepository.findByEstado(estado);
    }

    public List<ReglaRiesgo> listarHistorial(Long id) {
        log.debug("[RULES] Listando historial de versiones para regla ID: {}", id);
        ReglaRiesgo regla = buscarPorId(id);
        return reglaRiesgoRepository.findByNombreOrderByVersionDesc(regla.getNombre());
    }

    private void aplicarRequest(ReglaRiesgo regla, ReglaRiesgoRequest request) {
        regla.setNombre(request.nombre());
        regla.setCodigo(blankToNull(request.codigo()));
        regla.setDescripcion(request.descripcion());
        regla.setTipoRegla(request.tipoRegla() != null ? request.tipoRegla() : "GUIADA");
        regla.setSeveridad(request.severidad() != null ? request.severidad() : "MEDIA");
        regla.setPrioridad(request.prioridad() != null ? request.prioridad() : 0);
        regla.setScoreBase(request.scoreBase() != null ? request.scoreBase() : request.score() != null ? request.score() : BigDecimal.ZERO);
        regla.setCondicion(blankToNull(request.condicion()));
        regla.setEstado(request.estado() != null ? request.estado() : "BORRADOR");
        regla.setActiva(request.activa() != null ? request.activa() : "ACTIVA".equalsIgnoreCase(regla.getEstado()));
        if (request.escenarioId() != null) {
            regla.setEscenario(escenarioRepository.findById(request.escenarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Escenario", "id", request.escenarioId())));
        }
        try {
            if (request.condiciones() != null) {
                regla.setCondicionesJson(objectMapper.writeValueAsString(request.condiciones()));
            }
            if (request.acciones() != null) {
                regla.setAccionesJson(objectMapper.writeValueAsString(request.acciones()));
            } else if (request.accionIds() != null) {
                regla.setAccionesJson(objectMapper.writeValueAsString(request.accionIds()));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo serializar la definicion JSON de la regla", e);
        }
        if (regla.getCondicion() == null || regla.getCondicion().isBlank()) {
            regla.setCondicion(generarCondicionLegible(regla));
        }
    }

    private String generarCondicionLegible(ReglaRiesgo regla) {
        if (regla.getCondicionesJson() == null || regla.getCondicionesJson().isBlank()) {
            return "Condicion guiada pendiente de definir";
        }
        return "Condicion guiada JSON para " + regla.getNombre();
    }

    private String generarCodigo(String nombre, Integer version) {
        String base = nombre == null ? "REGLA" : nombre.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        return base + "_V" + (version != null ? version : 1);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
