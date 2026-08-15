package com.antifraude.catalog;

import com.antifraude.common.entity.Canal;
import com.antifraude.common.entity.Moneda;
import com.antifraude.common.entity.Pais;
import com.antifraude.common.entity.Producto;
import com.antifraude.common.repository.CanalRepository;
import com.antifraude.common.repository.MonedaRepository;
import com.antifraude.common.repository.PaisRepository;
import com.antifraude.common.repository.ProductoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private final PaisRepository paisRepository;
    private final MonedaRepository monedaRepository;
    private final CanalRepository canalRepository;
    private final ProductoRepository productoRepository;

    public CatalogoController(PaisRepository paisRepository, MonedaRepository monedaRepository,
                               CanalRepository canalRepository, ProductoRepository productoRepository) {
        this.paisRepository = paisRepository;
        this.monedaRepository = monedaRepository;
        this.canalRepository = canalRepository;
        this.productoRepository = productoRepository;
    }

    @GetMapping("/paises")
    public ResponseEntity<List<Pais>> listarPaises() {
        return ResponseEntity.ok(paisRepository.findAll());
    }

    @GetMapping("/monedas")
    public ResponseEntity<List<Moneda>> listarMonedas() {
        return ResponseEntity.ok(monedaRepository.findAll());
    }

    @GetMapping("/canales")
    public ResponseEntity<List<Canal>> listarCanales() {
        return ResponseEntity.ok(canalRepository.findAll());
    }

    @GetMapping("/productos")
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(productoRepository.findAll());
    }
}
