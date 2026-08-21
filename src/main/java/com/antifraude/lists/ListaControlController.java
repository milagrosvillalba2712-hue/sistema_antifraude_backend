package com.antifraude.lists;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/listas-control")
public class ListaControlController {

    private final ListaControlService service;

    public ListaControlController(ListaControlService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ListaControlDtos.ListaControlResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    public ResponseEntity<ListaControlDtos.ListaControlResponse> crear(@Valid @RequestBody ListaControlDtos.ListaControlRequest request) {
        return ResponseEntity.ok(service.crearLista(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListaControlDtos.ListaControlResponse> actualizar(@PathVariable Long id,
                                                                            @Valid @RequestBody ListaControlDtos.ListaControlRequest request) {
        return ResponseEntity.ok(service.actualizarLista(id, request));
    }

    @GetMapping("/{id}/elementos")
    public ResponseEntity<List<ListaControlDtos.ElementoControlResponse>> elementos(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarElementos(id));
    }

    @PostMapping("/{id}/elementos")
    public ResponseEntity<ListaControlDtos.ElementoControlResponse> crearElemento(@PathVariable Long id,
                                                                                  @Valid @RequestBody ListaControlDtos.ElementoControlRequest request) {
        return ResponseEntity.ok(service.crearElemento(id, request));
    }

    @PostMapping(path = "/{id}/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListaControlDtos.ImportacionListaControlResponse> importar(@PathVariable Long id,
                                                                                     @RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(service.importar(id, archivo));
    }

    @GetMapping("/importacion/plantilla")
    public ResponseEntity<ListaControlDtos.ImportPreview> plantilla() {
        return ResponseEntity.ok(new ListaControlDtos.ImportPreview(service.columnasImportacion()));
    }
}

