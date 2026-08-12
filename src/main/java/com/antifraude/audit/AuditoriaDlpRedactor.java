package com.antifraude.audit;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Redaccion de datos sensibles (DLP) aplicada al cuerpo de las peticiones antes
 * de persistirlo en la auditoria. Solo se usa cuando el nivel de auditoria es
 * TOTAL (Fase 3, item 3.3). Nunca escribe el valor original en auditoria.
 */
public final class AuditoriaDlpRedactor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern CLAVE_SENSIBLE = Pattern.compile(
            "(?i)(password|pass|secret|token|api_?key|clave|documento|documentoid|numero[_ ]?documento|"
                    + "cuit|cuil|ruc|dn[ir]|tarjeta|card|cardnumber|iban|cuenta|cvv|cvc|email|correo|"
                    + "telefono|celular|bearer)");

    private AuditoriaDlpRedactor() {
    }

    public static String enmascarar(String cuerpoJson) {
        if (cuerpoJson == null || cuerpoJson.isBlank()) {
            return null;
        }
        try {
            Object redactado = redactar(MAPPER.readValue(cuerpoJson, Object.class));
            return MAPPER.writeValueAsString(redactado);
        } catch (Exception ex) {
            return cuerpoJson;
        }
    }

    private static Object redactar(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> resultado = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                String clave = String.valueOf(entrada.getKey());
                resultado.put(clave, CLAVE_SENSIBLE.matcher(clave).find()
                        ? "***"
                        : redactar(entrada.getValue()));
            }
            return resultado;
        }
        if (valor instanceof List<?> lista) {
            List<Object> resultado = new ArrayList<>(lista.size());
            for (Object elemento : lista) {
                resultado.add(redactar(elemento));
            }
            return resultado;
        }
        return valor;
    }
}