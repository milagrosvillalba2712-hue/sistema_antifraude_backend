package com.antifraude.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auditoria")
@Transactional(readOnly = true)
public class AuditoriaController {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaController.class);

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<List<Auditoria>> listar(
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fin) {
        log.debug("[AUDIT] GET /api/auditoria - usuario: {} - rango: {} a {}", usuarioId, inicio, fin);
        List<Auditoria> eventos;
        if (usuarioId != null) {
            eventos = auditoriaService.buscarPorUsuario(usuarioId);
        } else if (inicio != null && fin != null) {
            eventos = auditoriaService.buscarPorRangoFechas(inicio, fin);
        } else {
            eventos = auditoriaService.listarTodas();
        }
        return ResponseEntity.ok(eventos);
    }
}