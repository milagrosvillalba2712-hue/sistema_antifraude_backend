package com.antifraude.rules;

import com.antifraude.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReglaRiesgoService {

    private static final Logger log = LoggerFactory.getLogger(ReglaRiesgoService.class);

    private final ReglaRiesgoRepository reglaRiesgoRepository;

    public ReglaRiesgoService(ReglaRiesgoRepository reglaRiesgoRepository) {
        this.reglaRiesgoRepository = reglaRiesgoRepository;
    }

    public ReglaRiesgo crear(ReglaRiesgo regla) {
        log.info("[RULES] Creando regla: {} - Tipo: {} - Severidad: {}",
                regla.getNombre(), regla.getTipoRegla(), regla.getSeveridad());
        ReglaRiesgo creada = reglaRiesgoRepository.save(regla);
        log.info("[RULES] Regla creada - ID: {} - Nombre: {}", creada.getId(), creada.getNombre());
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
        return reglaRiesgoRepository.findByActivaTrue();
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
        regla.setFechaModificacion(LocalDateTime.now());
        ReglaRiesgo guardada = reglaRiesgoRepository.save(regla);
        log.info("[RULES] Regla actualizada - ID: {} - Nombre: {}", id, guardada.getNombre());
        return guardada;
    }

    public void toggleActiva(Long id) {
        log.info("[RULES] Cambiando estado de regla ID: {}", id);
        ReglaRiesgo regla = buscarPorId(id);
        regla.setActiva(!regla.getActiva());
        regla.setFechaModificacion(LocalDateTime.now());
        reglaRiesgoRepository.save(regla);
        log.info("[RULES] Regla ID: {} - Activa: {}", id, regla.getActiva());
    }
}
