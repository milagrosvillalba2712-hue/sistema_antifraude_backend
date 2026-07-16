package com.antifraude.drools;

import com.antifraude.drools.fact.TransaccionFact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Component
public class ConditionEvaluator {

    private final ObjectMapper objectMapper;

    public ConditionEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean evaluate(String condicionesJson, RiskContext context) {
        if (condicionesJson == null || condicionesJson.isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(condicionesJson);
            String combinador = root.path("combinador").asText(root.path("operator").asText("ALL"));
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                items = root.path("condiciones");
            }
            if (!items.isArray() || items.isEmpty()) {
                return false;
            }

            boolean any = "ANY".equalsIgnoreCase(combinador) || "OR".equalsIgnoreCase(combinador);
            for (JsonNode item : items) {
                boolean result = evaluateItem(item, context);
                if (any && result) return true;
                if (!any && !result) return false;
            }
            return !any;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean evaluateItem(JsonNode item, RiskContext context) {
        if (item.has("items") || item.has("condiciones")) {
            return evaluate(item.toString(), context);
        }
        String fact = item.path("fact").asText(item.path("campo").asText(""));
        String operator = item.path("operador").asText(item.path("operator").asText("=="));
        JsonNode expected = item.has("valor") ? item.get("valor") : item.get("value");
        Object actual = facts(context).get(normalize(fact));

        return switch (operator.toLowerCase()) {
            case "==", "=", "eq" -> compare(actual, expected) == 0;
            case "!=", "<>", "ne" -> compare(actual, expected) != 0;
            case ">", "gt" -> compare(actual, expected) > 0;
            case ">=", "gte" -> compare(actual, expected) >= 0;
            case "<", "lt" -> compare(actual, expected) < 0;
            case "<=", "lte" -> compare(actual, expected) <= 0;
            case "in" -> in(actual, expected);
            case "between" -> between(actual, expected);
            case "exists" -> exists(actual);
            default -> false;
        };
    }

    private Map<String, Object> facts(RiskContext context) {
        Map<String, Object> facts = new HashMap<>();
        TransaccionFact t = context.getTransaccionFact();
        if (t != null) {
            facts.put("monto", t.getMonto());
            facts.put("moneda", t.getMonedaCodigo());
            facts.put("monedacodigo", t.getMonedaCodigo());
            facts.put("canal", t.getCanalCodigo());
            facts.put("canalcodigo", t.getCanalCodigo());
            facts.put("paisorigen", t.getPaisOrigenCodigo());
            facts.put("paisorigencodigo", t.getPaisOrigenCodigo());
            facts.put("paisdestino", t.getPaisDestinoCodigo());
            facts.put("paisdestinocodigo", t.getPaisDestinoCodigo());
            facts.put("documento", t.getIdentificadorDocumento());
            facts.put("documentocliente", t.getIdentificadorDocumento());
            facts.put("producto", t.getProductoNombre());
            facts.put("fecha", t.getFechaTransaccion());
            facts.put("fechahora", t.getFechaTransaccion());
        }
        facts.put("pep", !context.getRegistrosPEP().isEmpty());
        facts.put("observado", !context.getRegistrosObservados().isEmpty());
        facts.put("listas", listDocuments(context));
        facts.put("horario", !context.getHorariosRiesgo().isEmpty());
        facts.put("frecuencia", context.getHistorialTransacciones().size());
        facts.put("fechaactual", context.getFechaHoraActual() != null ? context.getFechaHoraActual() : LocalDateTime.now());
        return facts;
    }

    private Set<String> listDocuments(RiskContext context) {
        Set<String> values = new HashSet<>();
        context.getListasNegras().forEach(l -> values.add(l.getDocumentoIdentidad()));
        context.getListasGrises().forEach(l -> values.add(l.getDocumentoIdentidad()));
        context.getListasBlancas().forEach(l -> values.add(l.getDocumentoIdentidad()));
        return values;
    }

    private int compare(Object actual, JsonNode expected) {
        if (actual == null || expected == null || expected.isNull()) {
            return actual == null && (expected == null || expected.isNull()) ? 0 : -1;
        }
        BigDecimal leftNumber = toNumber(actual);
        BigDecimal rightNumber = toNumber(expected);
        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber);
        }
        return String.valueOf(actual).compareToIgnoreCase(jsonValue(expected));
    }

    private boolean in(Object actual, JsonNode expected) {
        if (actual == null || expected == null) return false;
        if (actual instanceof Set<?> set) {
            if (expected.isArray()) {
                for (JsonNode value : expected) {
                    if (set.contains(jsonValue(value))) return true;
                }
            }
            return set.contains(jsonValue(expected));
        }
        if (expected.isArray()) {
            for (JsonNode value : expected) {
                if (compare(actual, value) == 0) return true;
            }
        }
        return false;
    }

    private boolean between(Object actual, JsonNode expected) {
        if (expected == null || !expected.isArray() || expected.size() < 2) return false;
        return compare(actual, expected.get(0)) >= 0 && compare(actual, expected.get(1)) <= 0;
    }

    private boolean exists(Object actual) {
        if (actual == null) return false;
        if (actual instanceof Boolean b) return b;
        if (actual instanceof Set<?> set) return !set.isEmpty();
        if (actual instanceof String s) return !s.isBlank();
        return true;
    }

    private BigDecimal toNumber(Object value) {
        try {
            if (value instanceof BigDecimal decimal) return decimal;
            if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toNumber(JsonNode value) {
        if (value == null || value.isNull()) return null;
        try {
            if (value.isNumber()) return value.decimalValue();
            return new BigDecimal(value.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private String jsonValue(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (value.isTextual() || value.isNumber() || value.isBoolean()) return value.asText();
        return value.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("_", "").replace("-", "").replace(".", "").toLowerCase();
    }
}
