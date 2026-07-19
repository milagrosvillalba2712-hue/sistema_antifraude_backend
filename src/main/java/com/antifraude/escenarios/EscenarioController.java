package com.antifraude.escenarios;

import com.antifraude.common.entity.Escenario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/escenarios")
public class EscenarioController {

    private final EscenarioService escenarioService;

    public EscenarioController(EscenarioService escenarioService) {
        this.escenarioService = escenarioService;
    }

    @GetMapping
    public ResponseEntity<List<Escenario>> listar() {
        return ResponseEntity.ok(escenarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Escenario> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(escenarioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Escenario> crear(@Valid @RequestBody Escenario escenario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(escenarioService.crear(escenario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Escenario> actualizar(@PathVariable Long id, @Valid @RequestBody Escenario escenario) {
        return ResponseEntity.ok(escenarioService.actualizar(id, escenario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        escenarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
