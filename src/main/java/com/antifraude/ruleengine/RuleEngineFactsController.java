package com.antifraude.ruleengine;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rule-engine/facts")
public class RuleEngineFactsController {

    @GetMapping
    public ResponseEntity<List<FactDefinition>> listarFacts() {
        return ResponseEntity.ok(List.of(
                new FactDefinition("monto", "Monto de la transaccion", "NUMERICO", null,
                        List.of(">", ">=", "<", "<=", "between")),
                new FactDefinition("canal", "Canal utilizado", "CATALOGO", "canal",
                        List.of("==", "!=", "in")),
                new FactDefinition("moneda", "Moneda", "CATALOGO", "moneda",
                        List.of("==", "!=", "in")),
                new FactDefinition("paisOrigen", "Pais de origen", "CATALOGO", "pais",
                        List.of("==", "!=", "in")),
                new FactDefinition("paisDestino", "Pais de destino", "CATALOGO", "pais",
                        List.of("==", "!=", "in")),
                new FactDefinition("pep", "Cliente PEP", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("observado", "Cliente observado", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("listas", "Presente en listas regulatorias", "EXISTENCIA", null,
                        List.of("exists", "==")),
                new FactDefinition("remitenteEnLista", "Remitente en lista de riesgo", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("beneficiarioEnLista", "Beneficiario en lista de riesgo", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("documentoEnLista", "Documento en lista de riesgo", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("cuentaEnLista", "Cuenta en lista de riesgo", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("paisOrigenAltoRiesgo", "País origen de alto riesgo", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("paisDestinoAltoRiesgo", "País destino de alto riesgo", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("paisOrigenMonitoreado", "País origen bajo monitoreo", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("paisDestinoMonitoreado", "País destino bajo monitoreo", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("tipoLista", "Tipo de lista coincidente", "CATALOGO", "fuente_datos_riesgo",
                        List.of("==", "!=", "in")),
                new FactDefinition("fuenteLista", "Fuente de lista coincidente", "CATALOGO", "fuente_datos_riesgo",
                        List.of("==", "!=", "in")),
                new FactDefinition("severidadLista", "Severidad de lista coincidente", "CATALOGO", "nivel_riesgo",
                        List.of("==", "!=", "in"))
        ));
    }

    public record FactDefinition(String fact, String etiqueta, String tipo, String catalogo,
                                 List<String> operadores) {}
}
