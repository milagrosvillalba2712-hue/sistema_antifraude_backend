package com.antifraude.catalog;

import com.antifraude.common.entity.Canal;
import com.antifraude.common.entity.Moneda;
import com.antifraude.common.entity.Pais;
import com.antifraude.common.entity.Producto;
import com.antifraude.common.entity.TipoDocumento;
import com.antifraude.common.repository.CanalRepository;
import com.antifraude.common.repository.MonedaRepository;
import com.antifraude.common.repository.PaisRepository;
import com.antifraude.common.repository.ProductoRepository;
import com.antifraude.common.repository.TipoDocumentoRepository;
import com.antifraude.dto.TipoDocumentoCatalogoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private final PaisRepository paisRepository;
    private final MonedaRepository monedaRepository;
    private final CanalRepository canalRepository;
    private final ProductoRepository productoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;

    public CatalogoController(PaisRepository paisRepository, MonedaRepository monedaRepository,
                               CanalRepository canalRepository, ProductoRepository productoRepository,
                               TipoDocumentoRepository tipoDocumentoRepository) {
        this.paisRepository = paisRepository;
        this.monedaRepository = monedaRepository;
        this.canalRepository = canalRepository;
        this.productoRepository = productoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
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

    @GetMapping("/tipos-documento")
    public ResponseEntity<List<TipoDocumentoCatalogoResponse>> listarTiposDocumento(
            @RequestParam(name = "pais", required = false) String pais) {
        String paisCodigo = pais == null || pais.isBlank() ? null : pais.trim().toUpperCase(Locale.ROOT);
        return ResponseEntity.ok(tipoDocumentoRepository.findActivosByPaisCodigo(paisCodigo).stream()
                .map(this::toTipoDocumentoResponse)
                .toList());
    }

    private TipoDocumentoCatalogoResponse toTipoDocumentoResponse(TipoDocumento tipoDocumento) {
        return new TipoDocumentoCatalogoResponse(
                tipoDocumento.getId(),
                tipoDocumento.getCodigo(),
                tipoDocumento.getCodigoTecnico(),
                tipoDocumento.getSigla(),
                tipoDocumento.getNombre(),
                tipoDocumento.getPaisRelacion() != null ? tipoDocumento.getPaisRelacion().getCodigoIso() : null,
                tipoDocumento.getPaisRelacion() != null ? tipoDocumento.getPaisRelacion().getNombre() : "Global",
                tipoDocumento.getTipoPersona(),
                tipoDocumento.getFormatoRegex(),
                tipoDocumento.getFuenteOficialCita());
    }
}
