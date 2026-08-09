package com.antifraude.reports;

import com.antifraude.alerts.Alerta;
import com.antifraude.alerts.AlertaRepository;
import com.antifraude.common.entity.Caso;
import com.antifraude.common.repository.CasoAlertaRepository;
import com.antifraude.exception.BusinessException;
import com.antifraude.exception.ResourceNotFoundException;
import com.antifraude.users.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    private final ReporteRosRepository reporteRosRepository;
    private final AlertaRepository alertaRepository;
    private final CasoAlertaRepository casoAlertaRepository;

    public ReporteService(ReporteRosRepository reporteRosRepository,
                          AlertaRepository alertaRepository,
                          CasoAlertaRepository casoAlertaRepository) {
        this.reporteRosRepository = reporteRosRepository;
        this.alertaRepository = alertaRepository;
        this.casoAlertaRepository = casoAlertaRepository;
    }

    @Transactional
    public byte[] generarReporteRos(Long alertaId, Usuario usuario) {
        log.info("[REPORTS] Generando reporte ROS - Alerta ID: {} - Usuario: {}", alertaId, usuario.getEmail());
        Alerta alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> {
                    log.warn("[REPORTS] Alerta no encontrada para reporte - ID: {}", alertaId);
                    return new ResourceNotFoundException("Alerta", "id", alertaId);
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            writer.write("ID_ALERTA,ID_TRANSACCION,REGLA,PRIORIDAD,FECHA\n");
            writer.write(String.format("%s,%s,%s,%s,%s%n",
                    alerta.getId(),
                    alerta.getTransaccion() != null ? alerta.getTransaccion().getId() : "N/A",
                    alerta.getRegla() != null ? alerta.getRegla().getNombre() : "N/A",
                    alerta.getPrioridad(),
                    alerta.getFechaGeneracion()));
        } catch (Exception e) {
            log.error("[REPORTS] Error al generar CSV - Alerta ID: {} - Error: {}", alertaId, e.getMessage());
            throw new BusinessException("REPORT_GENERATION_ERROR", "Error al generar reporte ROS");
        }

        String nombreArchivo = "ROS_" + alertaId + "_" + LocalDateTime.now().toString().replace(":", "-") + ".csv";
        Caso caso = casoAlertaRepository.findByAlertaId(alertaId).stream()
                .findFirst()
                .map(casoAlerta -> casoAlerta.getCaso())
                .orElseThrow(() -> new BusinessException("CASE_REQUIRED",
                        "La alerta debe estar asociada a un caso antes de generar un reporte ROS"));
        String codigo = "ROS-" + alerta.getId() + "-" + System.currentTimeMillis();
        ReporteRos reporte = ReporteRos.builder()
                .alerta(alerta)
                .caso(caso)
                .empresa(alerta.getEmpresa())
                .generadoPor(usuario)
                .nombreArchivo(nombreArchivo)
                .codigo(codigo)
                .estado("GENERADO")
                .descripcionSospecha("Reporte ROS generado desde alerta " + alerta.getCodigo())
                .soporteReferencia(nombreArchivo)
                .reporteJson("{\"alertaId\":" + alerta.getId() + ",\"casoId\":" + caso.getId()
                        + ",\"generadoPor\":\"" + usuario.getEmail() + "\"}")
                .build();
        reporteRosRepository.save(reporte);
        log.info("[REPORTS] Reporte ROS generado - Archivo: {} - Tamanio: {} bytes", nombreArchivo, baos.size());
        return baos.toByteArray();
    }

    public List<ReporteRos> listarReportes() {
        log.debug("[REPORTS] Listando todos los reportes ROS");
        return reporteRosRepository.findAll();
    }
}
