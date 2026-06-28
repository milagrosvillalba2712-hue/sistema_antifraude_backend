package com.antifraude.profile;

import com.antifraude.dto.DisponibilidadRequest;
import com.antifraude.dto.DisponibilidadResponse;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availability")
public class DisponibilidadController {

    private static final Logger log = LoggerFactory.getLogger(DisponibilidadController.class);

    private final DisponibilidadService disponibilidadService;
    private final UsuarioService usuarioService;

    public DisponibilidadController(DisponibilidadService disponibilidadService,
                                     UsuarioService usuarioService) {
        this.disponibilidadService = disponibilidadService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<DisponibilidadResponse>> listar(Authentication auth) {
        log.info("[AVAILABILITY] GET /api/availability - Usuario: {}", auth.getName());
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        List<DisponibilidadResponse> response = disponibilidadService.listarPorUsuario(usuario.getId())
                .stream()
                .map(disponibilidadService::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<DisponibilidadResponse> crear(@Valid @RequestBody DisponibilidadRequest request,
                                                         Authentication auth,
                                                         HttpServletRequest httpRequest) {
        log.info("[AVAILABILITY] POST /api/availability - Tipo: {} - Programado: {}",
                request.tipoEstado(), request.esProgramado());
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(disponibilidadService.toResponse(
                disponibilidadService.crear(usuario.getId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DisponibilidadResponse> actualizar(@PathVariable Long id,
                                                              @Valid @RequestBody DisponibilidadRequest request,
                                                              Authentication auth) {
        log.info("[AVAILABILITY] PUT /api/availability/{}", id);
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(disponibilidadService.toResponse(
                disponibilidadService.actualizar(id, usuario.getId(), request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, Authentication auth) {
        log.info("[AVAILABILITY] DELETE /api/availability/{}", id);
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        disponibilidadService.cancelar(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}
