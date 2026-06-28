package com.antifraude.profile;

import com.antifraude.dto.*;
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
public class PerfilService {

    private static final Logger log = LoggerFactory.getLogger(PerfilService.class);

    private final PerfilUsuarioRepository perfilRepository;
    private final UsuarioService usuarioService;

    public PerfilService(PerfilUsuarioRepository perfilRepository, UsuarioService usuarioService) {
        this.perfilRepository = perfilRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public PerfilUsuario obtenerPorUsuarioId(Long usuarioId) {
        return perfilRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPerfilPorDefecto(usuarioId));
    }

    public PerfilUsuario actualizarPerfil(Long usuarioId, String nombreVisible, String imagenPerfil) {
        log.info("[PROFILE] Actualizando perfil usuario ID: {}", usuarioId);
        PerfilUsuario perfil = obtenerPorUsuarioId(usuarioId);
        if (nombreVisible != null) {
            perfil.setNombreVisible(nombreVisible);
        }
        if (imagenPerfil != null) {
            perfil.setImagenPerfil(imagenPerfil);
        }
        return perfilRepository.save(perfil);
    }

    public PerfilUsuario cambiarEstado(Long usuarioId, String estado, String estadoPersonalizado) {
        log.info("[PROFILE] Cambiando estado usuario ID: {} a {}", usuarioId, estado);
        PerfilUsuario perfil = obtenerPorUsuarioId(usuarioId);
        perfil.setEstado(estado);
        perfil.setEstadoPersonalizado(estadoPersonalizado);
        perfil.setUltimaActualizacionEstado(LocalDateTime.now());
        return perfilRepository.save(perfil);
    }

    public PerfilUsuario actualizarImagen(Long usuarioId, String imagenBase64) {
        log.info("[PROFILE] Actualizando imagen usuario ID: {}", usuarioId);
        PerfilUsuario perfil = obtenerPorUsuarioId(usuarioId);
        perfil.setImagenPerfil(imagenBase64);
        return perfilRepository.save(perfil);
    }

    private PerfilUsuario crearPerfilPorDefecto(Long usuarioId) {
        log.info("[PROFILE] Creando perfil por defecto para usuario ID: {}", usuarioId);
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        PerfilUsuario perfil = PerfilUsuario.builder()
                .usuario(usuario)
                .nombreVisible(usuario.getNombre())
                .estado("DISPONIBLE")
                .ultimaActualizacionEstado(LocalDateTime.now())
                .build();
        return perfilRepository.save(perfil);
    }

    public PerfilResponse toResponse(PerfilUsuario p) {
        return new PerfilResponse(
                p.getId(),
                p.getUsuario().getId(),
                p.getNombreVisible(),
                p.getImagenPerfil(),
                p.getEstado(),
                p.getEstadoPersonalizado(),
                p.getUltimaActualizacionEstado());
    }
}
