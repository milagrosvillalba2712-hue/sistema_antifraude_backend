package com.antifraude.cases;

import com.antifraude.common.entity.Caso;
import com.antifraude.common.entity.Caso.EstadoCaso;
import com.antifraude.dto.CasoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/casos")
public class CasoController {

    private static final Logger log = LoggerFactory.getLogger(CasoController.class);

    private final CasoService casoService;

    public CasoController(CasoService casoService) {
        this.casoService = casoService;
    }

    @PostMapping
    public ResponseEntity<CasoResponse> crear(@RequestBody Caso caso) {
        log.info("[CASES] POST /api/casos - Codigo: {} - Titulo: {}", caso.getCodigo(), caso.getTitulo());
        Caso creada = casoService.crear(caso);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(creada));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<CasoResponse>> listar() {
        log.info("[CASES] GET /api/casos");
        return ResponseEntity.ok(casoService.listarTodos().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<CasoResponse> buscar(@PathVariable Long id) {
        log.info("[CASES] GET /api/casos/{}", id);
        return ResponseEntity.ok(toResponse(casoService.buscarPorId(id)));
    }

    @GetMapping("/estado/{estado}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CasoResponse>> buscarPorEstado(@PathVariable EstadoCaso estado) {
        log.info("[CASES] GET /api/casos/estado/{}", estado);
        return ResponseEntity.ok(casoService.buscarPorEstado(estado).stream().map(this::toResponse).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CasoResponse> actualizar(@PathVariable Long id, @RequestBody Caso caso) {
        log.info("[CASES] PUT /api/casos/{}", id);
        return ResponseEntity.ok(toResponse(casoService.actualizar(id, caso)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CasoResponse> cambiarEstado(@PathVariable Long id, @RequestParam EstadoCaso estado) {
        log.info("[CASES] PATCH /api/casos/{}/estado?={}", id, estado);
        return ResponseEntity.ok(toResponse(casoService.cambiarEstado(id, estado)));
    }

    @PatchMapping("/{id}/asignar")
    public ResponseEntity<CasoResponse> asignarAnalista(@PathVariable Long id, @RequestParam UUID analistaId) {
        log.info("[CASES] PATCH /api/casos/{}/asignar?analistaId={}", id, analistaId);
        return ResponseEntity.ok(toResponse(casoService.asignarAnalista(id, analistaId)));
    }

    private CasoResponse toResponse(Caso caso) {
        var analista = caso.getUsuarioAnalista();
        return new CasoResponse(caso.getId(), caso.getCodigo(), caso.getTitulo(), caso.getDescripcion(),
                caso.getEstado(), caso.getPrioridad(), caso.getScore(),
                analista != null ? analista.getId() : null,
                analista != null ? analista.getNombre() : null,
                caso.getFechaApertura(), caso.getFechaCierre(), caso.getResultado(), caso.getObservaciones());
    }
}
