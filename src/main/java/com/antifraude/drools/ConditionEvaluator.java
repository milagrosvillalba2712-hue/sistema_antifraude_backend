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
            facts.put("cuentaorigen", t.getCuentaOrigen());
            facts.put("cuentadestino", t.getCuentaDestino());
            facts.put("remitente", t.getPersonaRemitenteNombre());
            facts.put("remitentenombre", t.getPersonaRemitenteNombre());
            facts.put("beneficiario", t.getPersonaBeneficiarioNombre());
            facts.put("beneficiarionombre", t.getPersonaBeneficiarioNombre());
            facts.put("tipotransaccion", t.getTipoTransaccion());
            facts.put("infraestructura", t.getInfraestructuraPago());
            facts.put("infraestructurapago", t.getInfraestructuraPago());
            facts.put("modulosipap", t.getModuloSipap());
            facts.put("subtipotransaccion", t.getSubtipoTransaccion());
            facts.put("endtoendid", t.getEndToEndId());
            facts.put("spireference", t.getSpiReference());
            facts.put("aliasemisortipo", t.getAliasEmisorTipo());
            facts.put("aliasreceptortipo", t.getAliasReceptorTipo());
            facts.put("declaracionfondos", t.isRequiereDeclaracionFondos());
            facts.put("depositantetercero", t.isDepositanteTercero());
            facts.put("empeoperador", t.getEmpeOperador());
            facts.put("tipocheque", t.getTipoCheque());
            facts.put("estadoclearing", t.getEstadoClearing());
            facts.put("procesadoratarjeta", t.getProcesadoraTarjeta());
            facts.put("mcc", t.getMcc());
            facts.put("canaltarjeta", t.getCanalTarjeta());
            facts.put("panlast4", t.getPanLast4());
            facts.put("qrestandar", t.getQrStandard());
            facts.put("qrstandard", t.getQrStandard());
            facts.put("qrhubreference", t.getQrHubReference());
            facts.put("remittancepayoutmethod", t.getRemittancePayoutMethod());
            facts.put("paiscorredorremesa", t.getPaisCorredorRemesa());
            facts.put("swiftbicorigen", t.getSwiftBicOrigen());
            facts.put("swiftbicdestino", t.getSwiftBicDestino());
            facts.put("esspi", matchesAny(t, "SPI", "PY_SPI"));
            facts.put("eslbtr", matchesAny(t, "LBTR"));
            facts.put("esempe", matchesAny(t, "EMPE", "WALLET"));
            facts.put("esqr", matchesAny(t, "QR"));
            facts.put("estarjeta", matchesAny(t, "CARD", "TARJETA"));
            facts.put("escheque", matchesAny(t, "CHEQUE"));
            facts.put("esefectivo", matchesAny(t, "CASH", "EFECTIVO"));
            facts.put("esremesa", matchesAny(t, "REMITTANCE", "REMESA"));
            facts.put("esfx", matchesAny(t, "FX", "CAMBIO"));
        }
        facts.put("pep", !context.getRegistrosPEP().isEmpty());
        facts.put("observado", !context.getRegistrosObservados().isEmpty());
        facts.put("listas", listDocuments(context));
        facts.put("sujetoenlista", !context.getCoincidenciasListas().isEmpty());
        facts.put("remitenteenlista", context.isRemitenteEnLista());
        facts.put("beneficiarioenlista", context.isBeneficiarioEnLista());
        facts.put("documentoenlista", context.isDocumentoEnLista());
        facts.put("cuentaenlista", context.isCuentaEnLista());
        facts.put("paisorigenaltoriesgo", context.isPaisOrigenAltoRiesgo());
        facts.put("paisdestinoaltoriesgo", context.isPaisDestinoAltoRiesgo());
        facts.put("paisorigenmonitoreado", context.isPaisOrigenMonitoreado());
        facts.put("paisdestinomonitoreado", context.isPaisDestinoMonitoreado());
        facts.put("tipolista", listValues(context, "categoria"));
        facts.put("fuentelista", listValues(context, "fuente"));
        facts.put("severidadlista", listValues(context, "severidad"));
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

    private Set<String> listValues(RiskContext context, String field) {
        Set<String> values = new HashSet<>();
        context.getCoincidenciasListas().forEach(c -> {
            String value = switch (field) {
                case "categoria" -> c.getCategoria();
                case "fuente" -> c.getFuenteCodigo();
                case "severidad" -> c.getSeveridad();
                default -> null;
            };
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        });
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

    private boolean matchesAny(TransaccionFact fact, String... needles) {
        String source = String.join(" ",
                safe(fact.getTipoTransaccion()),
                safe(fact.getCanalCodigo()),
                safe(fact.getInfraestructuraPago()),
                safe(fact.getModuloSipap()),
                safe(fact.getSubtipoTransaccion())).toUpperCase();
        for (String needle : needles) {
            if (source.contains(needle.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
