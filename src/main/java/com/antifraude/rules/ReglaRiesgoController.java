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
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(reglaRiesgoService.crearDesdeRequest(request, usuario)));
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
        return ResponseEntity.ok(toResponse(reglaRiesgoService.actualizarDesdeRequest(id, request)));
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleActiva(@PathVariable Long id) {
        log.info("[RULES] POST /api/reglas/{}/toggle", id);
        reglaRiesgoService.toggleActiva(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        log.info("[RULES] POST /api/reglas/{}/activar", id);
        reglaRiesgoService.activar(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        log.info("[RULES] POST /api/reglas/{}/desactivar", id);
        reglaRiesgoService.desactivar(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/version")
    public ResponseEntity<ReglaRiesgoResponse> crearVersion(@PathVariable Long id) {
        log.info("[RULES] POST /api/reglas/{}/version", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(reglaRiesgoService.crearNuevaVersion(id)));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<ReglaRiesgoResponse>> historial(@PathVariable Long id) {
        log.info("[RULES] GET /api/reglas/{}/historial", id);
        List<ReglaRiesgoResponse> response = reglaRiesgoService.listarHistorial(id).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/escenario/{escenarioId}")
    public ResponseEntity<List<ReglaRiesgoResponse>> listarPorEscenario(@PathVariable Long escenarioId) {
        log.info("[RULES] GET /api/reglas/escenario/{}", escenarioId);
        List<ReglaRiesgoResponse> response = reglaRiesgoService.listarPorEscenario(escenarioId).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ReglaRiesgoResponse>> listarPorEstado(@PathVariable String estado) {
        log.info("[RULES] GET /api/reglas/estado/{}", estado);
        List<ReglaRiesgoResponse> response = reglaRiesgoService.listarPorEstado(estado).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    private ReglaRiesgoResponse toResponse(ReglaRiesgo r) {
        return new ReglaRiesgoResponse(
                r.getId(),
                r.getEscenario() != null ? r.getEscenario().getId() : null,
                r.getEscenario() != null ? r.getEscenario().getNombre() : null,
                r.getCodigo(),
                r.getNombre(), r.getDescripcion(), r.getTipoRegla(),
                r.getSeveridad(), r.getPrioridad(), r.getScoreBase(), r.getVersion(), r.getEstado(),
                r.getCondicion(), r.getCondicionesJson(), r.getAccionesJson(), r.getActiva(),
                r.getCreadaPor() != null ? r.getCreadaPor().getId() : null,
                r.getFechaCreacion(), r.getFechaModificacion());
    }
}
