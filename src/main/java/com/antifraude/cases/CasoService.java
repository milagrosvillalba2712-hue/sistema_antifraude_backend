package com.antifraude.cases;

import com.antifraude.common.entity.Caso;
import com.antifraude.common.entity.Caso.EstadoCaso;
import com.antifraude.common.repository.CasoRepository;
import com.antifraude.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CasoService {

    private static final Logger log = LoggerFactory.getLogger(CasoService.class);

    private final CasoRepository casoRepository;

    public CasoService(CasoRepository casoRepository) {
        this.casoRepository = casoRepository;
    }

    public Caso crear(Caso caso) {
        log.info("[CASES] Creando caso: {} - Titulo: {}", caso.getCodigo(), caso.getTitulo());
        caso.setEstado(EstadoCaso.NUEVO);
        caso.setFechaApertura(LocalDateTime.now());
        Caso creada = casoRepository.save(caso);
        log.info("[CASES] Caso creado - ID: {} - Codigo: {}", creada.getId(), creada.getCodigo());
        return creada;
    }

    public List<Caso> listarTodos() {
        log.debug("[CASES] Listando todos los casos");
        return casoRepository.findAll();
    }

    public Caso buscarPorId(Long id) {
        log.debug("[CASES] Buscando caso por ID: {}", id);
        return casoRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[CASES] Caso no encontrado con ID: {}", id);
                    return new ResourceNotFoundException("Caso", "id", id);
                });
    }

    public List<Caso> buscarPorEstado(EstadoCaso estado) {
        log.debug("[CASES] Buscando casos por estado: {}", estado);
        return casoRepository.findByEstado(estado);
    }

    public Caso actualizar(Long id, Caso actualizado) {
        log.info("[CASES] Actualizando caso ID: {}", id);
        Caso caso = buscarPorId(id);
        caso.setTitulo(actualizado.getTitulo());
        caso.setDescripcion(actualizado.getDescripcion());
        caso.setPrioridad(actualizado.getPrioridad());
        caso.setScore(actualizado.getScore());
        Caso guardada = casoRepository.save(caso);
        log.info("[CASES] Caso actualizado - ID: {} - Titulo: {}", id, guardada.getTitulo());
        return guardada;
    }

    public Caso cambiarEstado(Long id, EstadoCaso nuevoEstado) {
        log.info("[CASES] Cambiando estado de caso ID: {} a {}", id, nuevoEstado);
        Caso caso = buscarPorId(id);
        caso.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoCaso.CERRADO || nuevoEstado == EstadoCaso.RESUELTO) {
            caso.setFechaCierre(LocalDateTime.now());
        }
        Caso guardada = casoRepository.save(caso);
        log.info("[CASES] Caso ID: {} - Estado: {}", id, guardada.getEstado());
        return guardada;
    }

    public Caso asignarAnalista(Long casoId, UUID analistaId) {
        log.info("[CASES] Asignando caso ID: {} a analista ID: {}", casoId, analistaId);
        Caso caso = buscarPorId(casoId);
        com.antifraude.users.Usuario analista = new com.antifraude.users.Usuario();
        analista.setId(analistaId);
        caso.setUsuarioAnalista(analista);
        caso.setEstado(EstadoCaso.ASIGNADO);
        Caso guardada = casoRepository.save(caso);
        log.info("[CASES] Caso ID: {} asignado a analista ID: {}", casoId, analistaId);
        return guardada;
    }

    public long contarPorEstado(EstadoCaso estado) {
        return casoRepository.countByEstado(estado);
    }
}
