package com.antifraude.alerts;

import com.antifraude.dto.*;
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
        List<AlertaResponse> response = alertaService.listarTodas().stream().map(alertaService::toAlertaResponse).toList();
        log.info("[ALERTS] Retornando {} alertas", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertaResponse> buscar(@PathVariable Long id) {
        log.info("[ALERTS] GET /api/alertas/{}", id);
        return ResponseEntity.ok(alertaService.toAlertaResponse(alertaService.buscarPorId(id)));
    }

    @GetMapping("/{id}/detalle")
    public ResponseEntity<AlertaDetalleResponse> detalle(@PathVariable Long id) {
        log.info("[ALERTS] GET /api/alertas/{}/detalle", id);
        return ResponseEntity.ok(alertaService.obtenerDetalleFormal(id));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<AlertaResponse>> buscarPorEstado(@PathVariable String estado) {
        log.info("[ALERTS] GET /api/alertas/estado/{}", estado);
        return ResponseEntity.ok(alertaService.buscarPorEstado(estado).stream().map(alertaService::toAlertaResponse).toList());
    }

    @GetMapping("/sin-asignar/count")
    public ResponseEntity<Map<String, Long>> contarSinAsignar() {
        log.info("[ALERTS] GET /api/alertas/sin-asignar/count");
        return ResponseEntity.ok(Map.of("count", alertaService.contarSinAsignar()));
    }

    @GetMapping("/analistas-disponibles")
    public ResponseEntity<List<AnalistaDisponibleResponse>> analistasDisponibles() {
        log.info("[ALERTS] GET /api/alertas/analistas-disponibles");
        return ResponseEntity.ok(alertaService.listarAnalistasDisponibles());
    }

    @PostMapping("/{id}/asignar")
    public ResponseEntity<AlertaResponse> asignar(@PathVariable Long id,
                                                    @RequestBody(required = false) AsignarAlertaRequest body,
                                                    Authentication auth,
                                                    HttpServletRequest request) {
        log.info("[ALERTS] POST /api/alertas/{}/asignar - Usuario: {} - IP: {}", id, auth.getName(), request.getRemoteAddr());
        Usuario analista;
        if (body != null && body.analistaId() != null) {
            analista = usuarioService.buscarPorId(body.analistaId());
        } else {
            analista = usuarioService.buscarPorEmail(auth.getName());
        }
        return ResponseEntity.ok(alertaService.toAlertaResponse(alertaService.asignarAlerta(id, analista, request)));
    }

    @PostMapping("/{id}/autoasignarme")
    public ResponseEntity<AlertaResponse> autoasignarme(@PathVariable Long id,
                                                        Authentication auth,
                                                        HttpServletRequest request) {
        Usuario analista = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(alertaService.toAlertaResponse(alertaService.asignarAlerta(id, analista, request)));
    }

    @PostMapping("/{id}/reassign")
    public ResponseEntity<AlertaResponse> reasignar(@PathVariable Long id,
                                                      @RequestBody ReasignarAlertaRequest body,
                                                      Authentication auth,
                                                      HttpServletRequest request) {
        log.info("[ALERTS] POST /api/alertas/{}/reassign - Nuevo analista: {} - Motivo: {}",
                id, body.analistaId(), body.motivo());
        Usuario origen = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(alertaService.toAlertaResponse(
                alertaService.reasignarAlerta(id, body.analistaId(), body.motivo(), origen, request)));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<AlertaResponse> cerrar(@PathVariable Long id) {
        log.info("[ALERTS] POST /api/alertas/{}/cerrar", id);
        return ResponseEntity.ok(alertaService.toAlertaResponse(alertaService.cerrarAlerta(id)));
    }

    @PostMapping("/{id}/resolver")
    public ResponseEntity<AlertaResponse> resolver(@PathVariable Long id,
                                                    @RequestBody Map<String, String> body,
                                                    HttpServletRequest request) {
        String observacion = body.getOrDefault("observacion", "");
        log.info("[ALERTS] POST /api/alertas/{}/resolver - Observacion: {} - IP: {}", id, observacion, request.getRemoteAddr());
        return ResponseEntity.ok(alertaService.toAlertaResponse(alertaService.resolverAlerta(id, observacion, request)));
    }

    @PostMapping("/{id}/resolver-formal")
    public ResponseEntity<ResolucionAlertaResponse> resolverFormal(@PathVariable Long id,
                                                                    @RequestBody ResolucionAlertaRequest body,
                                                                    Authentication auth,
                                                                    HttpServletRequest request) {
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(alertaService.toResolucionResponse(
                alertaService.resolverFormalmente(id, usuario, body, request)));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<HistorialAsignacionResponse>> historial(@PathVariable Long id) {
        log.info("[ALERTS] GET /api/alertas/{}/history", id);
        List<HistorialAsignacionResponse> response = alertaService.obtenerHistorial(id).stream()
                .map(h -> new HistorialAsignacionResponse(
                        h.getId(),
                        h.getAlerta().getId(),
                        h.getUsuarioOrigen() != null ? h.getUsuarioOrigen().getId() : null,
                        h.getUsuarioOrigen() != null ? h.getUsuarioOrigen().getNombre() : null,
                        h.getUsuarioDestino().getId(),
                        h.getUsuarioDestino().getNombre(),
                        h.getFecha(),
                        h.getMotivo(),
                        h.getTipo()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<TimelineEventResponse>> timeline(@PathVariable Long id) {
        log.info("[ALERTS] GET /api/alertas/{}/timeline", id);
        List<TimelineEventResponse> response = alertaService.obtenerTimeline(id).stream()
                .map(e -> new TimelineEventResponse(
                        null, e.tipo(), e.descripcion(), e.fecha(), e.usuario()))
                .toList();
        return ResponseEntity.ok(response);
    }
}
