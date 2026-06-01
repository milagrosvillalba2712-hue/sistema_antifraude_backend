package com.antifraude.alerts;

import com.antifraude.dto.AlertaResponse;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private static final Logger log = LoggerFactory.getLogger(AlertaController.class);

    private final AlertaService alertaService;
    private final UsuarioService usuarioService;

    public AlertaController(AlertaService alertaService, UsuarioService usuarioService) {
        this.alertaService = alertaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<AlertaResponse>> listar() {
        log.info("[ALERTS] GET /api/alertas");
        List<AlertaResponse> response = alertaService.listarTodas().stream().map(this::toResponse).toList();
        log.info("[ALERTS] Retornando {} alertas", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertaResponse> buscar(@PathVariable Long id) {
        log.info("[ALERTS] GET /api/alertas/{}", id);
        return ResponseEntity.ok(toResponse(alertaService.buscarPorId(id)));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<AlertaResponse>> buscarPorEstado(@PathVariable String estado) {
        log.info("[ALERTS] GET /api/alertas/estado/{}", estado);
        return ResponseEntity.ok(alertaService.buscarPorEstado(estado).stream().map(this::toResponse).toList());
    }

    @PostMapping("/{id}/asignar")
    public ResponseEntity<AlertaResponse> asignar(@PathVariable Long id, Authentication auth,
                                                   HttpServletRequest request) {
        log.info("[ALERTS] POST /api/alertas/{}/asignar - Usuario: {} - IP: {}", id, auth.getName(), request.getRemoteAddr());
        Usuario analista = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(toResponse(alertaService.asignarAlerta(id, analista, request)));
    }

    @PostMapping("/{id}/resolver")
    public ResponseEntity<AlertaResponse> resolver(@PathVariable Long id,
                                                    @RequestBody Map<String, String> body,
                                                    HttpServletRequest request) {
        String observacion = body.getOrDefault("observacion", "");
        log.info("[ALERTS] POST /api/alertas/{}/resolver - Observacion: {} - IP: {}", id, observacion, request.getRemoteAddr());
        return ResponseEntity.ok(toResponse(alertaService.resolverAlerta(id, observacion, request)));
    }

    private AlertaResponse toResponse(Alerta a) {
        return new AlertaResponse(
                a.getId(),
                a.getTransaccion() != null ? a.getTransaccion().getId() : null,
                a.getRegla() != null ? a.getRegla().getId() : null,
                a.getPrioridad(), a.getEstado(), a.getObservacion(),
                a.getAsignadoA() != null ? a.getAsignadoA().getId() : null,
                a.getFechaGeneracion(), a.getFechaResolucion());
    }
}
