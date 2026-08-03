package com.antifraude.cases;

import com.antifraude.common.entity.Caso;
import com.antifraude.common.entity.Caso.EstadoCaso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Caso> crear(@RequestBody Caso caso) {
        log.info("[CASES] POST /api/casos - Codigo: {} - Titulo: {}", caso.getCodigo(), caso.getTitulo());
        Caso creada = casoService.crear(caso);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    public ResponseEntity<List<Caso>> listar() {
        log.info("[CASES] GET /api/casos");
        return ResponseEntity.ok(casoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Caso> buscar(@PathVariable Long id) {
        log.info("[CASES] GET /api/casos/{}", id);
        return ResponseEntity.ok(casoService.buscarPorId(id));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Caso>> buscarPorEstado(@PathVariable EstadoCaso estado) {
        log.info("[CASES] GET /api/casos/estado/{}", estado);
        return ResponseEntity.ok(casoService.buscarPorEstado(estado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Caso> actualizar(@PathVariable Long id, @RequestBody Caso caso) {
        log.info("[CASES] PUT /api/casos/{}", id);
        return ResponseEntity.ok(casoService.actualizar(id, caso));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Caso> cambiarEstado(@PathVariable Long id, @RequestParam EstadoCaso estado) {
        log.info("[CASES] PATCH /api/casos/{}/estado?={}", id, estado);
        return ResponseEntity.ok(casoService.cambiarEstado(id, estado));
    }

    @PatchMapping("/{id}/asignar")
    public ResponseEntity<Caso> asignarAnalista(@PathVariable Long id, @RequestParam UUID analistaId) {
        log.info("[CASES] PATCH /api/casos/{}/asignar?analistaId={}", id, analistaId);
        return ResponseEntity.ok(casoService.asignarAnalista(id, analistaId));
    }
}
