package com.antifraude;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.antifraude.alerts.AlertaService;

@SpringBootTest
class AntifraudeApplicationTests {

    @Autowired
    private AlertaService alertaService;

    @Test
    void contextLoads() {
    }

    @Test
    void alertaFiltrosCargaSinError() {
        alertaService.obtenerFiltros();
    }
}
