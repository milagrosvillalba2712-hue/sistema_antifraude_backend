package com.antifraude.reports;

import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final Logger log = LoggerFactory.getLogger(ReporteController.class);

    private final ReporteService reporteService;
    private final UsuarioService usuarioService;

    public ReporteController(ReporteService reporteService, UsuarioService usuarioService) {
        this.reporteService = reporteService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/ros/{alertaId}")
    public ResponseEntity<byte[]> generarRos(@PathVariable Long alertaId,
                                              @RequestParam(defaultValue = "CSV") String formato,
                                              Authentication auth,
                                              HttpServletRequest request) {
        String formatoNormalizado = normalizarFormato(formato);
        log.info("[REPORTS] GET /api/reportes/ros/{} (formato {}) - Usuario: {} - IP: {}",
                alertaId, formatoNormalizado, auth.getName(), request.getRemoteAddr());
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        byte[] contenido = reporteService.generarReporteRos(alertaId, usuario, formatoNormalizado);
        log.info("[REPORTS] Reporte ROS {} generado - Alerta ID: {} - Tamanio: {} bytes",
                formatoNormalizado, alertaId, contenido.length);
        String extension = extension(formatoNormalizado);
        MediaType tipoContenido = mediaType(formatoNormalizado);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ROS_" + alertaId + "." + extension)
                .contentType(tipoContenido)
                .body(contenido);
    }

    private String normalizarFormato(String formato) {
        if (formato == null) return "CSV";
        String superior = formato.trim().toUpperCase();
        return "JSON".equals(superior) || "XML".equals(superior) ? superior : "CSV";
    }

    private String extension(String formato) {
        return switch (formato) {
            case "JSON" -> "json";
            case "XML" -> "xml";
            default -> "csv";
        };
    }

    private MediaType mediaType(String formato) {
        return switch (formato) {
            case "JSON" -> MediaType.APPLICATION_JSON;
            case "XML" -> MediaType.APPLICATION_XML;
            default -> MediaType.parseMediaType("text/csv");
        };
    }
}
