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
                        List.of("==", "!=", "in")),
                new FactDefinition("tipoTransaccion", "Tipo de transacción Paraguay", "CATALOGO", "tipo_transaccion",
                        List.of("==", "!=", "in")),
                new FactDefinition("infraestructuraPago", "Infraestructura de pago", "CATALOGO", "canal_transaccion",
                        List.of("==", "!=", "in")),
                new FactDefinition("moduloSipap", "Módulo SIPAP/SPI/LBTR", "CATALOGO", "canal_transaccion",
                        List.of("==", "!=", "in", "exists")),
                new FactDefinition("esSpi", "Es transferencia SPI", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("esLbtr", "Es transferencia LBTR", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("esEmpe", "Es operación EMPE o billetera", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("esQr", "Es pago QR", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("esTarjeta", "Es operación con tarjeta", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("esCheque", "Es operación con cheque", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("esEfectivo", "Es operación en efectivo", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("esRemesa", "Es operación de remesa", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("esFx", "Es operación de cambio FX", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("declaracionFondos", "Tiene declaración de origen/destino de fondos", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("depositanteTercero", "Depósito realizado por tercero", "BOOLEANO", null,
                        List.of("exists", "==")),
                new FactDefinition("empeOperador", "Operador EMPE", "CATALOGO", "empe_operador",
                        List.of("==", "!=", "in")),
                new FactDefinition("procesadoraTarjeta", "Procesadora de tarjeta", "CATALOGO", "procesadora_tarjeta",
                        List.of("==", "!=", "in")),
                new FactDefinition("mcc", "MCC del comercio", "TEXTO", null,
                        List.of("==", "!=", "in")),
                new FactDefinition("tipoCheque", "Tipo de cheque", "TEXTO", null,
                        List.of("==", "!=", "in")),
                new FactDefinition("estadoClearing", "Estado de clearing", "TEXTO", null,
                        List.of("==", "!=", "in")),
                new FactDefinition("paisCorredorRemesa", "País corredor de remesa", "CATALOGO", "pais",
                        List.of("==", "!=", "in"))
        ));
    }

    public record FactDefinition(String fact, String etiqueta, String tipo, String catalogo,
                                 List<String> operadores) {}
}
