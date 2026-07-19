package com.antifraude.motor;

import com.antifraude.dto.EjecucionReglaResponse;
import com.antifraude.rules.EjecucionReglaRepository;
import com.antifraude.transactions.Transaccion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/motor")
public class MotorController {

    private final EjecucionReglaRepository ejecucionReglaRepository;

    public MotorController(EjecucionReglaRepository ejecucionReglaRepository) {
        this.ejecucionReglaRepository = ejecucionReglaRepository;
    }

    @GetMapping("/historial")
    public ResponseEntity<List<EjecucionReglaResponse>> listarHistorial() {
        return ResponseEntity.ok(ejecucionReglaRepository.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/historial/transaccion/{transaccionId}")
    public ResponseEntity<List<EjecucionReglaResponse>> historialPorTransaccion(@PathVariable Long transaccionId) {
        Transaccion t = new Transaccion();
        t.setId(transaccionId);
        return ResponseEntity.ok(ejecucionReglaRepository.findByTransaccion(t).stream().map(this::toResponse).toList());
    }

    @GetMapping("/historial/regla/{reglaId}")
    public ResponseEntity<List<EjecucionReglaResponse>> historialPorRegla(@PathVariable Long reglaId) {
        return ResponseEntity.ok(ejecucionReglaRepository.findByReglaId(reglaId).stream().map(this::toResponse).toList());
    }

    private EjecucionReglaResponse toResponse(com.antifraude.rules.EjecucionRegla ejecucion) {
        return new EjecucionReglaResponse(
                ejecucion.getId(),
                ejecucion.getTransaccion() != null ? ejecucion.getTransaccion().getId() : null,
                ejecucion.getTransaccion() != null ? ejecucion.getTransaccion().getCodigo() : null,
                ejecucion.getRegla() != null ? ejecucion.getRegla().getId() : null,
                ejecucion.getRegla() != null ? ejecucion.getRegla().getCodigo() : null,
                ejecucion.getRegla() != null ? ejecucion.getRegla().getNombre() : null,
                ejecucion.getRegla() != null ? ejecucion.getRegla().getVersion() : null,
                "CUMPLIO".equals(ejecucion.getResultadoEvaluacion()),
                ejecucion.getScoreRegla(),
                ejecucion.getDetalle(),
                ejecucion.getTiempoEjecucionMs(),
                ejecucion.getFechaEjecucion());
    }
}
