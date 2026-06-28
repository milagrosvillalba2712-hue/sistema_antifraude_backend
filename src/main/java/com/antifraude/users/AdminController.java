package com.antifraude.users;

import com.antifraude.audit.AuditoriaService;
import com.antifraude.dto.UsuarioRequest;
import com.antifraude.dto.UsuarioResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public AdminController(UsuarioService usuarioService, AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        log.info("[ADMIN] GET /api/admin/users");
        List<UsuarioResponse> response = usuarioService.listarTodos().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscar(@PathVariable Long id) {
        log.info("[ADMIN] GET /api/admin/users/{}", id);
        return ResponseEntity.ok(toResponse(usuarioService.buscarPorId(id)));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request,
                                                  HttpServletRequest httpRequest) {
        log.info("[ADMIN] POST /api/admin/users - Email: {}", request.email());
        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .email(request.email())
                .passwordHash(request.password())
                .rol(request.rol())
                .build();
        Usuario creado = usuarioService.crearUsuario(usuario);
        auditoriaService.registrar(creado.getId(), "CREAR_USUARIO",
                "Usuario creado: " + creado.getEmail(),
                httpRequest.getRemoteAddr(), "usuarios", creado.getId());
        return ResponseEntity.ok(toResponse(creado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody UsuarioRequest request,
                                                       HttpServletRequest httpRequest) {
        log.info("[ADMIN] PUT /api/admin/users/{}", id);
        Usuario actualizado = Usuario.builder()
                .nombre(request.nombre())
                .email(request.email())
                .passwordHash(request.password())
                .rol(request.rol())
                .build();
        Usuario guardado = usuarioService.actualizar(id, actualizado);
        auditoriaService.registrar(guardado.getId(), "ACTUALIZAR_USUARIO",
                "Usuario actualizado: " + guardado.getEmail(),
                httpRequest.getRemoteAddr(), "usuarios", id);
        return ResponseEntity.ok(toResponse(guardado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id,
                                            HttpServletRequest httpRequest) {
        log.info("[ADMIN] DELETE /api/admin/users/{}", id);
        Usuario usuario = usuarioService.buscarPorId(id);
        usuarioService.desactivar(id);
        auditoriaService.registrar(usuario.getId(), "DESACTIVAR_USUARIO",
                "Usuario desactivado: " + usuario.getEmail(),
                httpRequest.getRemoteAddr(), "usuarios", id);
        return ResponseEntity.noContent().build();
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getNombre(), u.getEmail(), u.getRol(),
                u.getActivo(), u.getIntentosFallidos(), u.getFechaCreacion());
    }
}
