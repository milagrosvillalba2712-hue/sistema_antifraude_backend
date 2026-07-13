package com.antifraude.motor;

import com.antifraude.rules.EjecucionRegla;
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
    public ResponseEntity<List<EjecucionRegla>> listarHistorial() {
        return ResponseEntity.ok(ejecucionReglaRepository.findAll());
    }

    @GetMapping("/historial/transaccion/{transaccionId}")
    public ResponseEntity<List<EjecucionRegla>> historialPorTransaccion(@PathVariable Long transaccionId) {
        Transaccion t = new Transaccion();
        t.setId(transaccionId);
        return ResponseEntity.ok(ejecucionReglaRepository.findByTransaccion(t));
    }

    @GetMapping("/historial/regla/{reglaId}")
    public ResponseEntity<List<EjecucionRegla>> historialPorRegla(@PathVariable Long reglaId) {
        return ResponseEntity.ok(ejecucionReglaRepository.findByReglaId(reglaId));
    }
}
