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
    public ResponseEntity<byte[]> generarRos(@PathVariable Long alertaId, Authentication auth,
                                              HttpServletRequest request) {
        log.info("[REPORTS] GET /api/reportes/ros/{} - Usuario: {} - IP: {}",
                alertaId, auth.getName(), request.getRemoteAddr());
        Usuario usuario = usuarioService.buscarPorEmail(auth.getName());
        byte[] csv = reporteService.generarReporteRos(alertaId, usuario);
        log.info("[REPORTS] Reporte ROS generado - Alerta ID: {} - Tamanio: {} bytes", alertaId, csv.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ROS_" + alertaId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
