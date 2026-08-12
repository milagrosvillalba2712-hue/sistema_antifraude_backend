package com.antifraude.reports;

import com.antifraude.alerts.Alerta;
import com.antifraude.alerts.AlertaRepository;
import com.antifraude.common.entity.Caso;
import com.antifraude.common.repository.CasoAlertaRepository;
import com.antifraude.exception.BusinessException;
import com.antifraude.exception.ResourceNotFoundException;
import com.antifraude.licensing.ConsumoService;
import com.antifraude.licensing.EnforcementService;
import com.antifraude.users.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);
    private static final Set<String> FORMATOS_JSON_XML = Set.of("JSON", "XML");

    private final ReporteRosRepository reporteRosRepository;
    private final AlertaRepository alertaRepository;
    private final CasoAlertaRepository casoAlertaRepository;
    private final EnforcementService enforcementService;
    private final ConsumoService consumoService;
    private final ObjectMapper objectMapper;

    public ReporteService(ReporteRosRepository reporteRosRepository,
                          AlertaRepository alertaRepository,
                          CasoAlertaRepository casoAlertaRepository,
                          EnforcementService enforcementService,
                          ConsumoService consumoService,
                          ObjectMapper objectMapper) {
        this.reporteRosRepository = reporteRosRepository;
        this.alertaRepository = alertaRepository;
        this.casoAlertaRepository = casoAlertaRepository;
        this.enforcementService = enforcementService;
        this.consumoService = consumoService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public byte[] generarReporteRos(Long alertaId, Usuario usuario) {
        return generarReporteRos(alertaId, usuario, "CSV");
    }

    @Transactional
    public byte[] generarReporteRos(Long alertaId, Usuario usuario, String formato) {
        String formatoNormalizado = normalizarFormato(formato);
        log.info("[REPORTS] Generando reporte ROS ({}) - Alerta ID: {} - Usuario: {}",
                formatoNormalizado, alertaId, usuario.getEmail());
        Alerta alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> {
                    log.warn("[REPORTS] Alerta no encontrada para reporte - ID: {}", alertaId);
                    return new ResourceNotFoundException("Alerta", "id", alertaId);
                });

        UUID empresaId = alerta.getEmpresa().getId();
        enforcementService.verificarSuscripcionVigente(empresaId);
        enforcementService.verificarModulo(empresaId, "ROS");
        if (FORMATOS_JSON_XML.contains(formatoNormalizado)) {
            enforcementService.verificarModulo(empresaId, "ROS_JSON_XML");
        }
        enforcementService.verificarLimiteReportes(empresaId);

        String nombreArchivo = "ROS_" + alertaId + "_" + LocalDateTime.now().toString().replace(":", "-")
                + "." + extension(formatoNormalizado);
        Caso caso = casoAlertaRepository.findByAlertaId(alertaId).stream()
                .findFirst()
                .map(casoAlerta -> casoAlerta.getCaso())
                .orElseThrow(() -> new BusinessException("CASE_REQUIRED",
                        "La alerta debe estar asociada a un caso antes de generar un reporte ROS"));
        String codigo = "ROS-" + alerta.getId() + "-" + System.currentTimeMillis();
        String descripcionSospecha = "Reporte ROS generado desde alerta " + alerta.getCodigo();
        Map<String, Object> datos = datosReporte(alerta, caso, usuario, codigo, descripcionSospecha);
        String reporteJsonCanonico = toJson(datos);
        byte[] contenido = serializar(formatoNormalizado, datos);
        if (contenido.length == 0) {
            log.error("[REPORTS] Error al generar {} - Alerta ID: {} - Tamanio vacio", formatoNormalizado, alertaId);
            throw new BusinessException("REPORT_GENERATION_ERROR", "Error al generar reporte ROS");
        }

        ReporteRos reporte = ReporteRos.builder()
                .alerta(alerta)
                .caso(caso)
                .empresa(alerta.getEmpresa())
                .generadoPor(usuario)
                .nombreArchivo(nombreArchivo)
                .codigo(codigo)
                .estado("GENERADO")
                .formato(formatoNormalizado)
                .descripcionSospecha(descripcionSospecha)
                .soporteReferencia(nombreArchivo)
                .reporteJson(reporteJsonCanonico)
                .build();
        reporteRosRepository.save(reporte);
        consumoService.registrarReporte(empresaId);
        log.info("[REPORTS] Reporte ROS {} generado - Archivo: {} - Tamanio: {} bytes",
                formatoNormalizado, nombreArchivo, contenido.length);
        return contenido;
    }

    private String normalizarFormato(String formato) {
        if (formato == null || formato.isBlank()) {
            return "CSV";
        }
        String superior = formato.trim().toUpperCase();
        if (!FORMATOS_JSON_XML.contains(superior)) {
            return "CSV";
        }
        return superior;
    }

    private String extension(String formato) {
        return switch (formato) {
            case "JSON" -> "json";
            case "XML" -> "xml";
            default -> "csv";
        };
    }

    private Map<String, Object> datosReporte(Alerta alerta, Caso caso, Usuario usuario, String codigo,
                                             String descripcionSospecha) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("alertaId", alerta.getId());
        datos.put("alertaCodigo", alerta.getCodigo());
        datos.put("transaccionId", alerta.getTransaccion() != null ? alerta.getTransaccion().getId() : null);
        datos.put("regla", alerta.getRegla() != null ? alerta.getRegla().getNombre() : "N/A");
        datos.put("prioridad", alerta.getPrioridad());
        datos.put("fechaGeneracion", String.valueOf(alerta.getFechaGeneracion()));
        datos.put("casoId", caso.getId());
        datos.put("empresa", alerta.getEmpresa().getNombre());
        datos.put("codigo", codigo);
        datos.put("descripcionSospecha", descripcionSospecha);
        datos.put("generadoPor", usuario.getEmail());
        return datos;
    }

    private byte[] serializar(String formato, Map<String, Object> datos) {
        return switch (formato) {
            case "JSON" -> toJson(datos).getBytes(StandardCharsets.UTF_8);
            case "XML" -> toXml(datos).getBytes(StandardCharsets.UTF_8);
            default -> toCsv(datos).getBytes(StandardCharsets.UTF_8);
        };
    }

    private String toJson(Map<String, Object> datos) {
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new BusinessException("REPORT_GENERATION_ERROR", "No se pudo serializar el reporte JSON");
        }
    }

    private String toCsv(Map<String, Object> datos) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            writer.write("ID_ALERTA,ID_TRANSACCION,REGLA,PRIORIDAD,FECHA\n");
            writer.write(String.format("%s,%s,%s,%s,%s%n",
                    datos.get("alertaId"),
                    datos.get("transaccionId") != null ? datos.get("transaccionId") : "N/A",
                    datos.get("regla"),
                    datos.get("prioridad"),
                    datos.get("fechaGeneracion")));
        } catch (Exception e) {
            log.error("[REPORTS] Error al generar CSV: {}", e.getMessage());
            throw new BusinessException("REPORT_GENERATION_ERROR", "Error al generar reporte ROS");
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private String toXml(Map<String, Object> datos) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<reporteRos>\n");
        datos.forEach((clave, valor) -> xml.append("  <").append(clave).append(">")
                .append(escapeXml(valor)).append("</").append(clave).append(">\n"));
        xml.append("</reporteRos>\n");
        return xml.toString();
    }

    private String escapeXml(Object valor) {
        if (valor == null) {
            return "";
        }
        return String.valueOf(valor)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public List<ReporteRos> listarReportes() {
        log.debug("[REPORTS] Listando todos los reportes ROS");
        return reporteRosRepository.findAll();
    }
}
