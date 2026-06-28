package com.antifraude.profile;

import com.antifraude.dto.DisponibilidadRequest;
import com.antifraude.dto.DisponibilidadResponse;
import com.antifraude.dto.PerfilResponse;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class PerfilController {

    private static final Logger log = LoggerFactory.getLogger(PerfilController.class);

    private final PerfilService perfilService;
    private final DisponibilidadService disponibilidadService;
    private final UsuarioService usuarioService;

    public PerfilController(PerfilService perfilService,
                            DisponibilidadService disponibilidadService,
                            UsuarioService usuarioService) {
        this.perfilService = perfilService;
        this.disponibilidadService = disponibilidadService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<PerfilResponse> obtener(Authentication auth) {
        log.info("[PROFILE] GET /api/profile - Usuario: {}", auth.getName());
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(perfilService.toResponse(perfilService.obtenerPorUsuarioId(usuario.getId())));
    }

    @PutMapping
    public ResponseEntity<PerfilResponse> actualizar(@RequestBody Map<String, String> body,
                                                      Authentication auth) {
        log.info("[PROFILE] PUT /api/profile - Usuario: {}", auth.getName());
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        String nombreVisible = body.get("nombreVisible");
        String imagenPerfil = body.get("imagenPerfil");
        return ResponseEntity.ok(perfilService.toResponse(
                perfilService.actualizarPerfil(usuario.getId(), nombreVisible, imagenPerfil)));
    }

    @PutMapping("/status")
    public ResponseEntity<PerfilResponse> cambiarEstado(@RequestBody Map<String, String> body,
                                                         Authentication auth) {
        log.info("[PROFILE] PUT /api/profile/status - Estado: {}", body.get("estado"));
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        String estado = body.get("estado");
        String estadoPersonalizado = body.get("estadoPersonalizado");
        return ResponseEntity.ok(perfilService.toResponse(
                perfilService.cambiarEstado(usuario.getId(), estado, estadoPersonalizado)));
    }

    @PutMapping("/image")
    public ResponseEntity<PerfilResponse> actualizarImagen(@RequestBody Map<String, String> body,
                                                            Authentication auth) {
        log.info("[PROFILE] PUT /api/profile/image");
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        String imagen = body.get("imagen");
        return ResponseEntity.ok(perfilService.toResponse(
                perfilService.actualizarImagen(usuario.getId(), imagen)));
    }
}
