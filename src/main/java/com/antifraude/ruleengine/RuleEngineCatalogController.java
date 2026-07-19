package com.antifraude.ruleengine;

import com.antifraude.common.entity.*;
import com.antifraude.common.repository.*;
import com.antifraude.dto.CatalogoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rule-engine/catalogos")
public class RuleEngineCatalogController {

    private final EscenarioRepository escenarioRepository;
    private final AccionRepository accionRepository;
    private final PaisRepository paisRepository;
    private final MonedaRepository monedaRepository;
    private final CanalRepository canalRepository;
    private final ProductoRepository productoRepository;

    public RuleEngineCatalogController(EscenarioRepository escenarioRepository,
                                       AccionRepository accionRepository,
                                       PaisRepository paisRepository,
                                       MonedaRepository monedaRepository,
                                       CanalRepository canalRepository,
                                       ProductoRepository productoRepository) {
        this.escenarioRepository = escenarioRepository;
        this.accionRepository = accionRepository;
        this.paisRepository = paisRepository;
        this.monedaRepository = monedaRepository;
        this.canalRepository = canalRepository;
        this.productoRepository = productoRepository;
    }

    @GetMapping("/{tipo}")
    public ResponseEntity<List<CatalogoResponse>> listar(@PathVariable String tipo) {
        return ResponseEntity.ok(repository(tipo).findAll().stream().map(this::toResponse).toList());
    }

    @PostMapping("/{tipo}")
    public ResponseEntity<CatalogoResponse> crear(@PathVariable String tipo, @RequestBody CatalogoResponse request) {
        return ResponseEntity.ok(toResponse(repository(tipo).save(toEntity(tipo, request, null))));
    }

    @PutMapping("/{tipo}/{id}")
    public ResponseEntity<CatalogoResponse> actualizar(@PathVariable String tipo,
                                                       @PathVariable Long id,
                                                       @RequestBody CatalogoResponse request) {
        return ResponseEntity.ok(toResponse(repository(tipo).save(toEntity(tipo, request, id))));
    }

    @SuppressWarnings("unchecked")
    private JpaRepository<Object, Long> repository(String tipo) {
        return switch (tipo.toLowerCase()) {
            case "escenarios" -> (JpaRepository<Object, Long>) (JpaRepository<?, ?>) escenarioRepository;
            case "acciones" -> (JpaRepository<Object, Long>) (JpaRepository<?, ?>) accionRepository;
            case "paises" -> (JpaRepository<Object, Long>) (JpaRepository<?, ?>) paisRepository;
            case "monedas" -> (JpaRepository<Object, Long>) (JpaRepository<?, ?>) monedaRepository;
            case "canales" -> (JpaRepository<Object, Long>) (JpaRepository<?, ?>) canalRepository;
            case "productos" -> (JpaRepository<Object, Long>) (JpaRepository<?, ?>) productoRepository;
            default -> throw new IllegalArgumentException("Catalogo no soportado: " + tipo);
        };
    }

    private Object toEntity(String tipo, CatalogoResponse request, Long id) {
        return switch (tipo.toLowerCase()) {
            case "escenarios" -> {
                Escenario e = id != null ? escenarioRepository.findById(id).orElse(new Escenario()) : new Escenario();
                e.setCodigo(request.codigo());
                e.setNombre(request.nombre());
                e.setDescripcion(request.descripcion());
                e.setActivo(request.activo() != null ? request.activo() : true);
                yield e;
            }
            case "acciones" -> {
                Accion a = id != null ? accionRepository.findById(id).orElse(new Accion()) : new Accion();
                a.setCodigo(request.codigo());
                a.setDescripcion(request.descripcion() != null ? request.descripcion() : request.nombre());
                a.setActivo(request.activo() != null ? request.activo() : true);
                yield a;
            }
            case "paises" -> {
                Pais p = id != null ? paisRepository.findById(id).orElse(new Pais()) : new Pais();
                p.setCodigoIso(request.codigo());
                p.setNombre(request.nombre());
                p.setContinente(request.descripcion());
                p.setActivo(request.activo() != null ? request.activo() : true);
                yield p;
            }
            case "monedas" -> {
                Moneda m = id != null ? monedaRepository.findById(id).orElse(new Moneda()) : new Moneda();
                m.setCodigoIso(request.codigo());
                m.setNombre(request.nombre());
                m.setActivo(request.activo() != null ? request.activo() : true);
                yield m;
            }
            case "canales" -> {
                Canal c = id != null ? canalRepository.findById(id).orElse(new Canal()) : new Canal();
                c.setCodigo(request.codigo());
                c.setNombre(request.nombre());
                c.setActivo(request.activo() != null ? request.activo() : true);
                yield c;
            }
            case "productos" -> {
                Producto p = id != null ? productoRepository.findById(id).orElse(new Producto()) : new Producto();
                p.setCodigo(request.codigo());
                p.setNombre(request.nombre());
                p.setActivo(request.activo() != null ? request.activo() : true);
                yield p;
            }
            default -> throw new IllegalArgumentException("Catalogo no soportado: " + tipo);
        };
    }

    private CatalogoResponse toResponse(Object entity) {
        if (entity instanceof Escenario e) return new CatalogoResponse(e.getId(), e.getCodigo(), e.getNombre(), e.getDescripcion(), e.getActivo());
        if (entity instanceof Accion a) return new CatalogoResponse(a.getId(), a.getCodigo(), a.getDescripcion(), a.getDescripcion(), a.getActivo());
        if (entity instanceof Pais p) return new CatalogoResponse(p.getId(), p.getCodigoIso(), p.getNombre(), p.getContinente(), p.getActivo());
        if (entity instanceof Moneda m) return new CatalogoResponse(m.getId(), m.getCodigoIso(), m.getNombre(), null, m.getActivo());
        if (entity instanceof Canal c) return new CatalogoResponse(c.getId(), c.getCodigo(), c.getNombre(), null, c.getActivo());
        if (entity instanceof Producto p) return new CatalogoResponse(p.getId(), p.getCodigo(), p.getNombre(), null, p.getActivo());
        throw new IllegalArgumentException("Entidad no soportada");
    }
}
