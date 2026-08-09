package com.antifraude.assignment;

import com.antifraude.dto.AlertaResponse;
import com.antifraude.dto.WorkloadResponse;
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
@RequestMapping("/api/assignment")
public class AssignmentController {

    private static final Logger log = LoggerFactory.getLogger(AssignmentController.class);

    private final AssignmentEngine assignmentEngine;
    private final WorkloadService workloadService;

    public AssignmentController(AssignmentEngine assignmentEngine, WorkloadService workloadService) {
        this.assignmentEngine = assignmentEngine;
        this.workloadService = workloadService;
    }

    @PostMapping("/run")
    public ResponseEntity<?> ejecutarMotor(@RequestBody Map<String, Long> body,
                                            Authentication auth,
                                            HttpServletRequest request) {
        Long alertaId = body.get("alertaId");
        log.info("[ASSIGNMENT] POST /api/assignment/run - Alerta ID: {} - Usuario: {}",
                alertaId, auth.getName());
        try {
            var alerta = assignmentEngine.asignarAutomaticamente(alertaId, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Alerta asignada exitosamente",
                    "alertaId", alerta.getId(),
                    "asignadoA", alerta.getAsignadoA() != null ? alerta.getAsignadoA().getId() : null));
        } catch (Exception e) {
            log.error("[ASSIGNMENT] Error en asignacion: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rebalance")
    public ResponseEntity<?> rebalancear(@RequestBody(required = false) Map<String, String> body) {
        UUID usuarioId = body != null && body.get("usuarioId") != null ? UUID.fromString(body.get("usuarioId")) : null;
        log.info("[ASSIGNMENT] POST /api/assignment/rebalance - Usuario ID: {}", usuarioId);
        if (usuarioId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "usuarioId es requerido para rebalancear"));
        }
        assignmentEngine.rebalancearAnalista(usuarioId);
        return ResponseEntity.ok(Map.of("message", "Rebalanceo completado"));
    }

    @GetMapping("/workload")
    public ResponseEntity<List<WorkloadResponse>> cargaAnalistas() {
        log.info("[ASSIGNMENT] GET /api/assignment/workload");
        return ResponseEntity.ok(workloadService.obtenerCargaTodos());
    }
}
