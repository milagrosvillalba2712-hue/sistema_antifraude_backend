package com.antifraude.rules;

import com.antifraude.dto.ReglaRiesgoRequest;
import com.antifraude.dto.ReglaRiesgoResponse;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reglas")
public class ReglaRiesgoController {

    private static final Logger log = LoggerFactory.getLogger(ReglaRiesgoController.class);

    private final ReglaRiesgoService reglaRiesgoService;
    private final UsuarioService usuarioService;

    public ReglaRiesgoController(ReglaRiesgoService reglaRiesgoService, UsuarioService usuarioService) {
        this.reglaRiesgoService = reglaRiesgoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<ReglaRiesgoResponse> crear(@Valid @RequestBody ReglaRiesgoRequest request,
                                                      Authentication auth, HttpServletRequest httpRequest) {
        log.info("[RULES] POST /api/reglas - Nombre: {} - IP: {}", request.nombre(), httpRequest.getRemoteAddr());
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        ReglaRiesgo regla = ReglaRiesgo.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .tipoRegla(request.tipoRegla())
                .severidad(request.severidad())
                .condicion(request.condicion())
                .activa(request.activa() != null ? request.activa() : true)
                .creadaPor(usuario)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(reglaRiesgoService.crear(regla)));
    }

    @GetMapping
    public ResponseEntity<List<ReglaRiesgoResponse>> listar() {
        log.info("[RULES] GET /api/reglas");
        List<ReglaRiesgoResponse> response = reglaRiesgoService.listarTodas().stream().map(this::toResponse).toList();
        log.info("[RULES] Retornando {} reglas", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReglaRiesgoResponse> buscar(@PathVariable Long id) {
        log.info("[RULES] GET /api/reglas/{}", id);
        return ResponseEntity.ok(toResponse(reglaRiesgoService.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReglaRiesgoResponse> actualizar(@PathVariable Long id,
                                                           @Valid @RequestBody ReglaRiesgoRequest request) {
        log.info("[RULES] PUT /api/reglas/{} - Nombre: {}", id, request.nombre());
        ReglaRiesgo actualizada = ReglaRiesgo.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .tipoRegla(request.tipoRegla())
                .severidad(request.severidad())
                .condicion(request.condicion())
                .activa(request.activa() != null ? request.activa() : true)
                .build();
        return ResponseEntity.ok(toResponse(reglaRiesgoService.actualizar(id, actualizada)));
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleActiva(@PathVariable Long id) {
        log.info("[RULES] POST /api/reglas/{}/toggle", id);
        reglaRiesgoService.toggleActiva(id);
        return ResponseEntity.ok().build();
    }

    private ReglaRiesgoResponse toResponse(ReglaRiesgo r) {
        return new ReglaRiesgoResponse(
                r.getId(), r.getNombre(), r.getDescripcion(), r.getTipoRegla(),
                r.getSeveridad(), r.getCondicion(), r.getActiva(),
                r.getCreadaPor() != null ? r.getCreadaPor().getId() : null,
                r.getFechaCreacion(), r.getFechaModificacion());
    }
}
