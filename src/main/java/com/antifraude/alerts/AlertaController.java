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
import java.util.UUID;

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
    public ResponseEntity<PageResponse<AlertaResponse>> listar(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size,
                                                                @RequestParam(required = false) String search,
                                                                @RequestParam(required = false) String severidad,
                                                                @RequestParam(required = false) String estado,
                                                                @RequestParam(required = false) Long escenarioId,
                                                                @RequestParam(required = false) UUID analistaId,
                                                                @RequestParam(required = false) String rangoFecha,
                                                                @RequestParam(required = false) String desde,
                                                                @RequestParam(required = false) String hasta,
                                                                @RequestParam(defaultValue = "recientes") String sort) {
        log.info("[ALERTS] GET /api/alertas");
        PageResponse<AlertaResponse> response = alertaService.buscarPaginado(search, severidad, estado, escenarioId,
                analistaId, rangoFecha, desde, hasta, sort, page, size);
        log.info("[ALERTS] Retornando pagina {} con {} alertas", response.page(), response.content().size());
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

    @PostMapping("/{id}/cliente/consultar")
    public ResponseEntity<AlertaDetalleResponse> consultarCliente(@PathVariable Long id) {
        log.info("[ALERTS] POST /api/alertas/{}/cliente/consultar", id);
        return ResponseEntity.ok(alertaService.consultarClienteExterno(id));
    }

    @GetMapping("/filtros")
    public ResponseEntity<AlertaFiltrosResponse> filtros() {
        log.info("[ALERTS] GET /api/alertas/filtros");
        return ResponseEntity.ok(alertaService.obtenerFiltros());
    }

    @GetMapping("/{id}/reglas-disparadas")
    public ResponseEntity<List<ReglaAlertaResponse>> reglasDisparadas(@PathVariable Long id) {
        log.info("[ALERTS] GET /api/alertas/{}/reglas-disparadas", id);
        return ResponseEntity.ok(alertaService.obtenerReglasDisparadas(id));
    }

    @GetMapping("/{id}/evidencias")
    public ResponseEntity<List<EvidenciaAlertaResponse>> evidencias(@PathVariable Long id) {
        log.info("[ALERTS] GET /api/alertas/{}/evidencias", id);
        return ResponseEntity.ok(alertaService.listarEvidencias(id));
    }

    @PostMapping("/{id}/evidencias")
    public ResponseEntity<EvidenciaAlertaResponse> crearEvidencia(@PathVariable Long id,
                                                                  @RequestBody EvidenciaAlertaRequest body,
                                                                  Authentication auth,
                                                                  HttpServletRequest request) {
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(alertaService.toEvidenciaResponse(
                alertaService.crearEvidencia(id, body, usuario, request)));
    }

    @PutMapping("/{id}/evidencias/{evidenciaId}")
    public ResponseEntity<EvidenciaAlertaResponse> actualizarEvidencia(@PathVariable Long id,
                                                                       @PathVariable Long evidenciaId,
                                                                       @RequestBody EvidenciaAlertaRequest body,
                                                                       Authentication auth,
                                                                       HttpServletRequest request) {
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(alertaService.toEvidenciaResponse(
                alertaService.actualizarEvidencia(id, evidenciaId, body, usuario, request)));
    }

    @PostMapping("/{id}/evidencias/archivo")
    public ResponseEntity<EvidenciaAlertaResponse> subirEvidenciaArchivo(
            @PathVariable Long id,
            @RequestParam("archivo") org.springframework.web.multipart.MultipartFile archivo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String referenciaArchivo,
            Authentication auth,
            HttpServletRequest request) {
        log.info("[ALERTS] POST /api/alertas/{}/evidencias/archivo - {}", id, archivo.getOriginalFilename());
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(alertaService.toEvidenciaResponse(alertaService.crearEvidenciaConArchivo(
                id, archivo, nombre, descripcion, tipo, referenciaArchivo, usuario, request)));
    }

    @GetMapping("/{id}/evidencias/{evidenciaId}/archivo")
    public ResponseEntity<byte[]> descargarEvidenciaArchivo(@PathVariable Long id, @PathVariable Long evidenciaId) {
        log.info("[ALERTS] GET /api/alertas/{}/evidencias/{}/archivo", id, evidenciaId);
        EvidenciaAlerta evidencia = alertaService.obtenerEvidenciaConArchivo(id, evidenciaId);
        if (evidencia.getContenido() == null || evidencia.getContenido().length == 0) {
            return ResponseEntity.notFound().build();
        }
        String nombre = evidencia.getContenidoNombre() != null ? evidencia.getContenidoNombre() : evidencia.getNombre();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombre.replace("\"", "") + "\"")
                .contentType(evidencia.getMimeType() != null
                        ? org.springframework.http.MediaType.parseMediaType(evidencia.getMimeType())
                        : org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(evidencia.getContenido());
    }

    @DeleteMapping("/{id}/evidencias/{evidenciaId}")
    public ResponseEntity<Void> eliminarEvidencia(@PathVariable Long id,
                                                  @PathVariable Long evidenciaId,
                                                  Authentication auth,
                                                  HttpServletRequest request) {
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        alertaService.eliminarEvidencia(id, evidenciaId, usuario, request);
        return ResponseEntity.noContent().build();
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
        Usuario ejecutor = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(alertaService.toAlertaResponse(alertaService.asignarAlerta(id, analista, ejecutor, request)));
    }

    @PostMapping("/{id}/autoasignarme")
    public ResponseEntity<AlertaResponse> autoasignarme(@PathVariable Long id,
                                                        Authentication auth,
                                                        HttpServletRequest request) {
        Usuario analista = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(alertaService.toAlertaResponse(alertaService.asignarAlerta(id, analista, analista, request)));
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
                alertaService.reasignarAlerta(id, body.analistaId(), body.motivo(), body.observacion(), origen, request)));
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

    @PostMapping("/{id}/aprobar-resolucion")
    public ResponseEntity<AprobacionSupervisorResponse> aprobarResolucion(@PathVariable Long id,
                                                                          @RequestBody(required = false) AprobarResolucionRequest body,
                                                                          Authentication auth,
                                                                          HttpServletRequest request) {
        Usuario supervisor = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(alertaService.toAprobacionResponse(
                alertaService.aprobarResolucion(id, supervisor, body != null ? body.observacion() : null, request)));
    }

    @PostMapping("/{id}/rechazar-resolucion")
    public ResponseEntity<AprobacionSupervisorResponse> rechazarResolucion(@PathVariable Long id,
                                                                           @RequestBody RechazarResolucionRequest body,
                                                                           Authentication auth,
                                                                           HttpServletRequest request) {
        Usuario supervisor = usuarioService.buscarPorEmail(auth.getName());
        return ResponseEntity.ok(alertaService.toAprobacionResponse(
                alertaService.rechazarResolucion(id, supervisor, body.motivo(), body.faltantes(), request)));
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
