package com.antifraude.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors,
        String origen,
        @JsonProperty("codigo_error")
        String codigoError,
        @JsonProperty("status_code")
        int statusCode,
        String mensaje,
        Map<String, Object> detalles,
        String api
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, LocalDateTime.now(), null);
    }

    public ErrorResponse(int status, String error, String message, String path,
                         LocalDateTime timestamp, Map<String, String> fieldErrors) {
        this(status, error, message, path, timestamp, fieldErrors,
                resolveOrigen(path), error, status, message, null, resolveApi(path));
    }

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path);
    }

    public static ErrorResponse of(int status, String error, String message, String path,
                                    Map<String, String> fieldErrors) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now(), fieldErrors);
    }

    public static ErrorResponse of(int status, String codigoError, String mensaje, String path,
                                   String origen, Map<String, Object> detalles) {
        return new ErrorResponse(status, codigoError, mensaje, path, LocalDateTime.now(), null,
                origen != null ? origen : resolveOrigen(path),
                codigoError,
                status,
                mensaje,
                detalles,
                resolveApi(path));
    }

    public static ErrorResponse of(ApiErrorDescriptor descriptor, String path, Map<String, Object> detalles) {
        return new ErrorResponse(descriptor.statusCode(), descriptor.codigoError(), descriptor.mensaje(), path,
                LocalDateTime.now(), null, descriptor.origen(), descriptor.codigoError(), descriptor.statusCode(),
                descriptor.mensaje(), detalles, descriptor.api());
    }

    public Map<String, Object> asMonitorRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("origen", origen);
        row.put("api", api);
        row.put("codigo_error", codigoError);
        row.put("status_code", statusCode);
        row.put("mensaje", mensaje);
        row.put("detalles", detalles);
        row.put("timestamp", timestamp);
        return row;
    }

    private static String resolveApi(String path) {
        if (path == null || path.isBlank()) return "API_DESCONOCIDA";
        if (path.startsWith("/api/admin-empresa")) return "ADMIN_EMPRESA";
        if (path.startsWith("/api/auth")) return "AUTH";
        if (path.startsWith("/api/alertas")) return "ALERTAS";
        if (path.startsWith("/api/transacciones")) return "TRANSACCIONES";
        if (path.startsWith("/api/rule-engine") || path.startsWith("/api/reglas")) return "MOTOR_REGLAS";
        if (path.startsWith("/api/kyc")) return "KYC";
        if (path.startsWith("/api/reportes")) return "REPORTES";
        if (path.startsWith("/api/licensing") || path.startsWith("/api/licensing-local")) return "LICENCIAMIENTO";
        if (path.startsWith("/api/auditoria")) return "AUDITORIA";
        if (path.startsWith("/api/admin/users")) return "USUARIOS";
        return "API_INTERNA";
    }

    private static String resolveOrigen(String path) {
        return "INTERNA:" + resolveApi(path);
    }
}
