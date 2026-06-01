package com.antifraude.dashboard;

import com.antifraude.dto.DashboardResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> dashboard(HttpServletRequest request) {
        log.info("[DASHBOARD] GET /api/dashboard - IP: {}", request.getRemoteAddr());
        DashboardResponse response = dashboardService.obtenerDashboard();
        log.info("[DASHBOARD] Dashboard generado exitosamente");
        return ResponseEntity.ok(response);
    }
}
