package com.antifraude.profile;

import com.antifraude.dto.DisponibilidadRequest;
import com.antifraude.dto.DisponibilidadResponse;
import com.antifraude.exception.BusinessException;
import com.antifraude.exception.ResourceNotFoundException;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DisponibilidadService {

    private static final Logger log = LoggerFactory.getLogger(DisponibilidadService.class);

    private final DisponibilidadRepository disponibilidadRepository;
    private final PerfilService perfilService;
    private final UsuarioService usuarioService;

    public DisponibilidadService(DisponibilidadRepository disponibilidadRepository,
                                  PerfilService perfilService,
                                  UsuarioService usuarioService) {
        this.disponibilidadRepository = disponibilidadRepository;
        this.perfilService = perfilService;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadUsuario> listarPorUsuario(Long usuarioId) {
        return disponibilidadRepository.findByUsuarioIdOrderByFechaInicioDesc(usuarioId);
    }

    public DisponibilidadUsuario crear(Long usuarioId, DisponibilidadRequest request) {
        log.info("[AVAILABILITY] Creando disponibilidad usuario ID: {} - Tipo: {}", usuarioId, request.tipoEstado());
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        DisponibilidadUsuario disponibilidad = DisponibilidadUsuario.builder()
                .usuario(usuario)
                .tipoEstado(request.tipoEstado())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .esProgramado(request.esProgramado() != null ? request.esProgramado() : false)
                .motivo(request.motivo())
                .build();
        DisponibilidadUsuario creada = disponibilidadRepository.save(disponibilidad);

        if (!creada.getEsProgramado() && creada.getFechaInicio().isBefore(LocalDateTime.now().plusMinutes(1))) {
            perfilService.cambiarEstado(usuarioId, request.tipoEstado(), request.motivo());
        }

        log.info("[AVAILABILITY] Disponibilidad creada - ID: {}", creada.getId());
        return creada;
    }

    public DisponibilidadUsuario actualizar(Long id, Long usuarioId, DisponibilidadRequest request) {
        log.info("[AVAILABILITY] Actualizando disponibilidad ID: {}", id);
        DisponibilidadUsuario disponibilidad = disponibilidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidad", "id", id));
        if (!disponibilidad.getUsuario().getId().equals(usuarioId)) {
            throw new BusinessException("UNAUTHORIZED", "No tiene permiso para modificar esta disponibilidad");
        }
        disponibilidad.setTipoEstado(request.tipoEstado());
        disponibilidad.setFechaInicio(request.fechaInicio());
        disponibilidad.setFechaFin(request.fechaFin());
        disponibilidad.setMotivo(request.motivo());
        return disponibilidadRepository.save(disponibilidad);
    }

    public void cancelar(Long id, Long usuarioId) {
        log.info("[AVAILABILITY] Cancelando disponibilidad ID: {}", id);
        DisponibilidadUsuario disponibilidad = disponibilidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidad", "id", id));
        if (!disponibilidad.getUsuario().getId().equals(usuarioId)) {
            throw new BusinessException("UNAUTHORIZED", "No tiene permiso para cancelar esta disponibilidad");
        }
        disponibilidad.setActivo(false);
        disponibilidadRepository.save(disponibilidad);
    }

    public boolean estaDisponible(Long usuarioId) {
        List<String> estadosNoDisponibles = List.of(
                "VACACIONES", "ALMUERZO", "EN_REUNION", "FUERA_OFICINA", "NO_DISPONIBLE");
        return !disponibilidadRepository.existsByUsuarioIdAndActivoTrueAndTipoEstadoIn(
                usuarioId, estadosNoDisponibles);
    }

    public List<DisponibilidadUsuario> findProgramadasRecientes() {
        return disponibilidadRepository.findProgramadasRecientes(
                LocalDateTime.now(), LocalDateTime.now().minusMinutes(2));
    }

    public void procesarProgramacionesPendientes() {
        List<DisponibilidadUsuario> programadas = disponibilidadRepository
                .findProgramadasRecientes(LocalDateTime.now(), LocalDateTime.now().minusMinutes(2));
        for (DisponibilidadUsuario disp : programadas) {
            Long usuarioId = disp.getUsuario().getId();
            perfilService.cambiarEstado(usuarioId, disp.getTipoEstado(), disp.getMotivo());
            log.info("[AVAILABILITY] Estado programado aplicado: {} -> usuario {}", disp.getTipoEstado(), usuarioId);
        }

        List<DisponibilidadUsuario> expiradas = disponibilidadRepository.findAll().stream()
                .filter(d -> d.getActivo() && d.getEsProgramado()
                        && d.getFechaFin() != null && d.getFechaFin().isBefore(LocalDateTime.now()))
                .toList();
        for (DisponibilidadUsuario disp : expiradas) {
            disp.setActivo(false);
            disponibilidadRepository.save(disp);
            Long usuarioId = disp.getUsuario().getId();
            perfilService.cambiarEstado(usuarioId, "DISPONIBLE", null);
            log.info("[AVAILABILITY] Estado programado expirado: usuario {} restaurado a DISPONIBLE", usuarioId);
        }
    }

    public DisponibilidadResponse toResponse(DisponibilidadUsuario d) {
        return new DisponibilidadResponse(
                d.getId(),
                d.getUsuario().getId(),
                d.getTipoEstado(),
                d.getFechaInicio(),
                d.getFechaFin(),
                d.getEsProgramado(),
                d.getMotivo(),
                d.getActivo());
    }
}
