package com.antifraude.auth;

import com.antifraude.dto.AceptaTerminosRequestDTO;
import com.antifraude.dto.DocumentoLegalResponseDTO;
import com.antifraude.dto.PendienteAceptacionDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/terminos-condiciones")
public class TerminosCondicionesController {

    private final TerminosCondicionesService terminosCondicionesService;

    public TerminosCondicionesController(TerminosCondicionesService terminosCondicionesService) {
        this.terminosCondicionesService = terminosCondicionesService;
    }

    @GetMapping("/pendientes")
    public ResponseEntity<PendienteAceptacionDTO> pendientes(Authentication authentication) {
        UUID usuarioId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(terminosCondicionesService.obtenerPendientes(usuarioId));
    }

    @PostMapping("/aceptar")
    public ResponseEntity<Map<String, String>> aceptar(@Valid @RequestBody AceptaTerminosRequestDTO request,
                                                        Authentication authentication,
                                                        HttpServletRequest httpRequest) {
        UUID usuarioId = UUID.fromString(authentication.getName());
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        terminosCondicionesService.aceptarDocumento(usuarioId, request.documentoLegalId(),
                request.acepto(), ip, userAgent);

        return ResponseEntity.ok(Map.of("mensaje", request.acepto()
                ? "Documento aceptado correctamente"
                : "Documento rechazado"));
    }

    @GetMapping("/documentos")
    public ResponseEntity<List<DocumentoLegalResponseDTO>> documentos() {
        return ResponseEntity.ok(terminosCondicionesService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoLegalResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(terminosCondicionesService.obtenerPorId(id));
    }

    @GetMapping("/publico")
    public ResponseEntity<List<DocumentoLegalResponseDTO>> publico() {
        return ResponseEntity.ok(terminosCondicionesService.listarActivos());
    }
}
