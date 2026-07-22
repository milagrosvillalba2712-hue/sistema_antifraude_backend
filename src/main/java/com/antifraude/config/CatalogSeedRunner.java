package com.antifraude.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class CatalogSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeedRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public CatalogSeedRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        seedBaseCatalogs();
    }

    private void seedBaseCatalogs() {
        log.info("[SEED] Verificando catalogos minimos del motor de reglas");
        seedPais();
        seedMoneda();
        seedCanal();
        seedProducto();
        seedNivelRiesgo();
        seedTipoDocumento();
        seedEscenario();
        seedAccion();
        seedReglas();
    }

    private void seedPais() {
        insert("pais", "codigo_iso", "AR", "INSERT INTO pais (codigo_iso,nombre,continente,activo) VALUES ('AR','Argentina','Sudamerica',true)");
        insert("pais", "codigo_iso", "BR", "INSERT INTO pais (codigo_iso,nombre,continente,activo) VALUES ('BR','Brasil','Sudamerica',true)");
        insert("pais", "codigo_iso", "CL", "INSERT INTO pais (codigo_iso,nombre,continente,activo) VALUES ('CL','Chile','Sudamerica',true)");
        insert("pais", "codigo_iso", "UY", "INSERT INTO pais (codigo_iso,nombre,continente,activo) VALUES ('UY','Uruguay','Sudamerica',true)");
        insert("pais", "codigo_iso", "PY", "INSERT INTO pais (codigo_iso,nombre,continente,activo) VALUES ('PY','Paraguay','Sudamerica',true)");
        insert("pais", "codigo_iso", "US", "INSERT INTO pais (codigo_iso,nombre,continente,activo) VALUES ('US','Estados Unidos','Norteamerica',true)");
        insert("pais", "codigo_iso", "ES", "INSERT INTO pais (codigo_iso,nombre,continente,activo) VALUES ('ES','España','Europa',true)");
        insert("pais", "codigo_iso", "MX", "INSERT INTO pais (codigo_iso,nombre,continente,activo) VALUES ('MX','Mexico','Norteamerica',true)");
        insert("pais", "codigo_iso", "CO", "INSERT INTO pais (codigo_iso,nombre,continente,activo) VALUES ('CO','Colombia','Sudamerica',true)");
        insert("pais", "codigo_iso", "PE", "INSERT INTO pais (codigo_iso,nombre,continente,activo) VALUES ('PE','Peru','Sudamerica',true)");
    }

    private void seedMoneda() {
        insert("moneda", "codigo_iso", "ARS", "INSERT INTO moneda (codigo_iso,nombre,activo) VALUES ('ARS','Peso Argentino',true)");
        insert("moneda", "codigo_iso", "USD", "INSERT INTO moneda (codigo_iso,nombre,activo) VALUES ('USD','Dolar Estadounidense',true)");
        insert("moneda", "codigo_iso", "EUR", "INSERT INTO moneda (codigo_iso,nombre,activo) VALUES ('EUR','Euro',true)");
        insert("moneda", "codigo_iso", "BRL", "INSERT INTO moneda (codigo_iso,nombre,activo) VALUES ('BRL','Real Brasileño',true)");
        insert("moneda", "codigo_iso", "UYU", "INSERT INTO moneda (codigo_iso,nombre,activo) VALUES ('UYU','Peso Uruguayo',true)");
        insert("moneda", "codigo_iso", "CLP", "INSERT INTO moneda (codigo_iso,nombre,activo) VALUES ('CLP','Peso Chileno',true)");
        insert("moneda", "codigo_iso", "PYG", "INSERT INTO moneda (codigo_iso,nombre,activo) VALUES ('PYG','Guarani Paraguayo',true)");
        insert("moneda", "codigo_iso", "MXN", "INSERT INTO moneda (codigo_iso,nombre,activo) VALUES ('MXN','Peso Mexicano',true)");
        insert("moneda", "codigo_iso", "COP", "INSERT INTO moneda (codigo_iso,nombre,activo) VALUES ('COP','Peso Colombiano',true)");
        insert("moneda", "codigo_iso", "PEN", "INSERT INTO moneda (codigo_iso,nombre,activo) VALUES ('PEN','Sol Peruano',true)");
    }

    private void seedCanal() {
        insert("canal", "codigo", "WEB", "INSERT INTO canal (codigo,nombre,activo) VALUES ('WEB','Banca Web',true)");
        insert("canal", "codigo", "SUCURSAL", "INSERT INTO canal (codigo,nombre,activo) VALUES ('SUCURSAL','Sucursal Fisica',true)");
        insert("canal", "codigo", "CAJERO", "INSERT INTO canal (codigo,nombre,activo) VALUES ('CAJERO','Cajero Automatico',true)");
        insert("canal", "codigo", "TRANSFERENCIA", "INSERT INTO canal (codigo,nombre,activo) VALUES ('TRANSFERENCIA','Transferencia Electronica',true)");
        insert("canal", "codigo", "DEBITO_AUTOMATICO", "INSERT INTO canal (codigo,nombre,activo) VALUES ('DEBITO_AUTOMATICO','Debito Automatico',true)");
        insert("canal", "codigo", "MOVIL", "INSERT INTO canal (codigo,nombre,activo) VALUES ('MOVIL','App Movil',true)");
        insert("canal", "codigo", "API", "INSERT INTO canal (codigo,nombre,activo) VALUES ('API','Integracion API',true)");
        insert("canal", "codigo", "POS", "INSERT INTO canal (codigo,nombre,activo) VALUES ('POS','Punto de Venta',true)");
        insert("canal", "codigo", "ATM", "INSERT INTO canal (codigo,nombre,activo) VALUES ('ATM','ATM Internacional',true)");
        insert("canal", "codigo", "CALL_CENTER", "INSERT INTO canal (codigo,nombre,activo) VALUES ('CALL_CENTER','Call Center',true)");
    }

    private void seedProducto() {
        insert("producto", "codigo", "CTA_CTE", "INSERT INTO producto (codigo,nombre,activo) VALUES ('CTA_CTE','Cuenta Corriente',true)");
        insert("producto", "codigo", "CAJA_AHORRO", "INSERT INTO producto (codigo,nombre,activo) VALUES ('CAJA_AHORRO','Caja de Ahorro',true)");
        insert("producto", "codigo", "PLAZO_FIJO", "INSERT INTO producto (codigo,nombre,activo) VALUES ('PLAZO_FIJO','Plazo Fijo',true)");
        insert("producto", "codigo", "TARJETA_CREDITO", "INSERT INTO producto (codigo,nombre,activo) VALUES ('TARJETA_CREDITO','Tarjeta de Credito',true)");
        insert("producto", "codigo", "TARJETA_DEBITO", "INSERT INTO producto (codigo,nombre,activo) VALUES ('TARJETA_DEBITO','Tarjeta de Debito',true)");
        insert("producto", "codigo", "PRESTAMO", "INSERT INTO producto (codigo,nombre,activo) VALUES ('PRESTAMO','Prestamo Personal',true)");
        insert("producto", "codigo", "TRANSFERENCIA", "INSERT INTO producto (codigo,nombre,activo) VALUES ('TRANSFERENCIA','Transferencia',true)");
        insert("producto", "codigo", "GIRO", "INSERT INTO producto (codigo,nombre,activo) VALUES ('GIRO','Giro Nacional',true)");
        insert("producto", "codigo", "REMESA", "INSERT INTO producto (codigo,nombre,activo) VALUES ('REMESA','Remesa Internacional',true)");
        insert("producto", "codigo", "WALLET", "INSERT INTO producto (codigo,nombre,activo) VALUES ('WALLET','Billetera Digital',true)");
    }

    private void seedNivelRiesgo() {
        insert("nivel_riesgo", "codigo", "MUY_BAJO", "INSERT INTO nivel_riesgo (codigo,nombre,orden,activo) VALUES ('MUY_BAJO','Muy Bajo',0,true)");
        insert("nivel_riesgo", "codigo", "BAJO", "INSERT INTO nivel_riesgo (codigo,nombre,orden,activo) VALUES ('BAJO','Bajo',1,true)");
        insert("nivel_riesgo", "codigo", "MEDIO", "INSERT INTO nivel_riesgo (codigo,nombre,orden,activo) VALUES ('MEDIO','Medio',2,true)");
        insert("nivel_riesgo", "codigo", "ALTO", "INSERT INTO nivel_riesgo (codigo,nombre,orden,activo) VALUES ('ALTO','Alto',3,true)");
        insert("nivel_riesgo", "codigo", "CRITICO", "INSERT INTO nivel_riesgo (codigo,nombre,orden,activo) VALUES ('CRITICO','Critico',4,true)");
    }

    private void seedTipoDocumento() {
        insert("tipo_documento", "codigo", "DNI", "INSERT INTO tipo_documento (codigo,nombre,pais_relacion_id,activo) SELECT 'DNI','Documento Nacional de Identidad',p.id,true FROM pais p WHERE p.codigo_iso='AR'");
        insert("tipo_documento", "codigo", "PASAPORTE", "INSERT INTO tipo_documento (codigo,nombre,pais_relacion_id,activo) VALUES ('PASAPORTE','Pasaporte',NULL,true)");
        insert("tipo_documento", "codigo", "CUIT", "INSERT INTO tipo_documento (codigo,nombre,pais_relacion_id,activo) SELECT 'CUIT','Clave Unica de Identificacion Tributaria',p.id,true FROM pais p WHERE p.codigo_iso='AR'");
        insert("tipo_documento", "codigo", "CUIL", "INSERT INTO tipo_documento (codigo,nombre,pais_relacion_id,activo) SELECT 'CUIL','Clave Unica de Identificacion Laboral',p.id,true FROM pais p WHERE p.codigo_iso='AR'");
        insert("tipo_documento", "codigo", "CDI", "INSERT INTO tipo_documento (codigo,nombre,pais_relacion_id,activo) SELECT 'CDI','Cedula de Identidad',p.id,true FROM pais p WHERE p.codigo_iso='PY'");
        insert("tipo_documento", "codigo", "RUC", "INSERT INTO tipo_documento (codigo,nombre,pais_relacion_id,activo) SELECT 'RUC','Registro Unico de Contribuyente',p.id,true FROM pais p WHERE p.codigo_iso='PY'");
        insert("tipo_documento", "codigo", "CI", "INSERT INTO tipo_documento (codigo,nombre,pais_relacion_id,activo) SELECT 'CI','Cedula de Identidad',p.id,true FROM pais p WHERE p.codigo_iso='PY'");
        insert("tipo_documento", "codigo", "NIT", "INSERT INTO tipo_documento (codigo,nombre,pais_relacion_id,activo) SELECT 'NIT','Numero de Identificacion Tributaria',p.id,true FROM pais p WHERE p.codigo_iso='CO'");
        insert("tipo_documento", "codigo", "SSN", "INSERT INTO tipo_documento (codigo,nombre,pais_relacion_id,activo) SELECT 'SSN','Social Security Number',p.id,true FROM pais p WHERE p.codigo_iso='US'");
        insert("tipo_documento", "codigo", "OTRO", "INSERT INTO tipo_documento (codigo,nombre,activo) VALUES ('OTRO','Otro documento',true)");
    }

    private void seedEscenario() {
        insert("escenario", "codigo", "FRAUDE_CLASICO", "INSERT INTO escenario (codigo,nombre,descripcion,activo) VALUES ('FRAUDE_CLASICO','Fraude Clasico','Suplantacion, uso no autorizado y patrones de fraude tradicional',true)");
        insert("escenario", "codigo", "LAVADO_DINERO", "INSERT INTO escenario (codigo,nombre,descripcion,activo) VALUES ('LAVADO_DINERO','Lavado de Dinero','Operaciones de lavado de activos',true)");
        insert("escenario", "codigo", "FINANCIAMIENTO_TERRORISMO", "INSERT INTO escenario (codigo,nombre,descripcion,activo) VALUES ('FINANCIAMIENTO_TERRORISMO','Financiamiento del Terrorismo','Operaciones vinculadas a financiamiento ilicito',true)");
        insert("escenario", "codigo", "OPERACIONES_SOSPECHOSAS", "INSERT INTO escenario (codigo,nombre,descripcion,activo) VALUES ('OPERACIONES_SOSPECHOSAS','Operaciones Sospechosas','Operaciones que requieren revision reforzada',true)");
        insert("escenario", "codigo", "PEP", "INSERT INTO escenario (codigo,nombre,descripcion,activo) VALUES ('PEP','Persona Politicamente Expuesta','Evaluacion de PEP y vinculados',true)");
        insert("escenario", "codigo", "LISTAS", "INSERT INTO escenario (codigo,nombre,descripcion,activo) VALUES ('LISTAS','Listas Regulatorias','Coincidencias contra listas y sanciones',true)");
        insert("escenario", "codigo", "PAIS_RIESGO", "INSERT INTO escenario (codigo,nombre,descripcion,activo) VALUES ('PAIS_RIESGO','Pais de Riesgo','Origen o destino con riesgo regulatorio',true)");
        insert("escenario", "codigo", "FRECUENCIA", "INSERT INTO escenario (codigo,nombre,descripcion,activo) VALUES ('FRECUENCIA','Frecuencia Inusual','Alta cantidad de operaciones en ventana corta',true)");
        insert("escenario", "codigo", "HORARIO", "INSERT INTO escenario (codigo,nombre,descripcion,activo) VALUES ('HORARIO','Horario Inusual','Operaciones fuera del patron horario',true)");
        insert("escenario", "codigo", "IMPORTE", "INSERT INTO escenario (codigo,nombre,descripcion,activo) VALUES ('IMPORTE','Importe Inusual','Montos fuera de umbrales esperados',true)");
    }

    private void seedAccion() {
        insert("accion", "codigo", "REVISION_MANUAL", "INSERT INTO accion (codigo,descripcion,activo) VALUES ('REVISION_MANUAL','Enviar a revision manual',true)");
        insert("accion", "codigo", "CREAR_ALERTA", "INSERT INTO accion (codigo,descripcion,activo) VALUES ('CREAR_ALERTA','Crear alerta operativa',true)");
        insert("accion", "codigo", "BLOQUEAR_TRANSACCION", "INSERT INTO accion (codigo,descripcion,activo) VALUES ('BLOQUEAR_TRANSACCION','Bloquear transaccion preventivamente',true)");
        insert("accion", "codigo", "SOLICITAR_DOCUMENTACION", "INSERT INTO accion (codigo,descripcion,activo) VALUES ('SOLICITAR_DOCUMENTACION','Solicitar documentacion adicional',true)");
        insert("accion", "codigo", "ESCALAR_SUPERVISOR", "INSERT INTO accion (codigo,descripcion,activo) VALUES ('ESCALAR_SUPERVISOR','Escalar a supervisor',true)");
        insert("accion", "codigo", "GENERAR_CASO", "INSERT INTO accion (codigo,descripcion,activo) VALUES ('GENERAR_CASO','Generar caso de investigacion',true)");
        insert("accion", "codigo", "CONSULTAR_KYC", "INSERT INTO accion (codigo,descripcion,activo) VALUES ('CONSULTAR_KYC','Consultar fuentes KYC externas',true)");
        insert("accion", "codigo", "MARCAR_PEP", "INSERT INTO accion (codigo,descripcion,activo) VALUES ('MARCAR_PEP','Marcar cliente como PEP',true)");
        insert("accion", "codigo", "AGREGAR_OBSERVADO", "INSERT INTO accion (codigo,descripcion,activo) VALUES ('AGREGAR_OBSERVADO','Agregar cliente observado',true)");
        insert("accion", "codigo", "GENERAR_ROS", "INSERT INTO accion (codigo,descripcion,activo) VALUES ('GENERAR_ROS','Preparar reporte ROS',true)");
    }

    private void seedReglas() {
        insertRule("AML_MONTO_USD", "Monto alto en USD", "LAVADO_DINERO", "ALTA", 10, 40,
                "{\"combinador\":\"ALL\",\"items\":[{\"fact\":\"monto\",\"operador\":\">\",\"valor\":10000},{\"fact\":\"moneda\",\"operador\":\"==\",\"valor\":\"USD\"}]}");
        insertRule("AML_PAIS_RIESGO", "Pais de riesgo", "PAIS_RIESGO", "ALTA", 9, 35,
                "{\"combinador\":\"ANY\",\"items\":[{\"fact\":\"paisOrigen\",\"operador\":\"in\",\"valor\":[\"US\",\"PY\",\"BR\"]},{\"fact\":\"paisDestino\",\"operador\":\"in\",\"valor\":[\"US\",\"PY\",\"BR\"]}]}");
        insertRule("AML_PEP", "Cliente PEP", "PEP", "ALTA", 8, 40,
                "{\"combinador\":\"ALL\",\"items\":[{\"fact\":\"pep\",\"operador\":\"exists\",\"valor\":true}]}");
        insertRule("AML_OBSERVADO", "Cliente observado", "OPERACIONES_SOSPECHOSAS", "ALTA", 7, 35,
                "{\"combinador\":\"ALL\",\"items\":[{\"fact\":\"observado\",\"operador\":\"exists\",\"valor\":true}]}");
        insertRule("AML_LISTAS", "Coincidencia en listas", "LISTAS", "CRITICA", 1, 60,
                "{\"combinador\":\"ALL\",\"items\":[{\"fact\":\"listas\",\"operador\":\"exists\",\"valor\":true}]}");
        insertRule("AML_FRECUENCIA", "Frecuencia inusual", "FRECUENCIA", "MEDIA", 6, 20,
                "{\"combinador\":\"ALL\",\"items\":[{\"fact\":\"frecuencia\",\"operador\":\">=\",\"valor\":5}]}");
        insertRule("AML_HORARIO", "Horario inusual", "HORARIO", "MEDIA", 5, 15,
                "{\"combinador\":\"ALL\",\"items\":[{\"fact\":\"horario\",\"operador\":\"exists\",\"valor\":true}]}");
        insertRule("AML_MONTO_PYG", "Monto alto en PYG", "IMPORTE", "MEDIA", 4, 25,
                "{\"combinador\":\"ALL\",\"items\":[{\"fact\":\"monto\",\"operador\":\">\",\"valor\":50000000},{\"fact\":\"moneda\",\"operador\":\"==\",\"valor\":\"PYG\"}]}");
        insertRule("FRAUDE_CANAL_WEB", "Canal web de riesgo", "FRAUDE_CLASICO", "MEDIA", 3, 20,
                "{\"combinador\":\"ALL\",\"items\":[{\"fact\":\"canal\",\"operador\":\"==\",\"valor\":\"WEB\"}]}");
        insertRule("FRAUDE_MOVIL_MONTO", "Movil con monto alto", "FRAUDE_CLASICO", "ALTA", 2, 35,
                "{\"combinador\":\"ALL\",\"items\":[{\"fact\":\"canal\",\"operador\":\"==\",\"valor\":\"MOVIL\"},{\"fact\":\"monto\",\"operador\":\">\",\"valor\":5000}]}");
    }

    private void insertRule(String codigo, String nombre, String escenarioCodigo, String severidad, int prioridad, int score, String condicionesJson) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reglas_riesgo WHERE codigo = ?", Integer.class, codigo);
            if (count != null && count > 0) return;
            jdbcTemplate.update("""
                    INSERT INTO reglas_riesgo
                    (codigo,nombre,descripcion,tipo_regla,severidad,prioridad,condicion,condiciones_json,acciones_json,escenario_id,score_base,creada_por,version,activa,estado)
                    VALUES (?,?,?,?,?,?,?,?,?,(SELECT id FROM escenario WHERE codigo = ?),?,(SELECT id FROM usuarios WHERE email = 'admin@antifraude.com'),1,true,'ACTIVA')
                    """,
                    codigo,
                    nombre,
                    "Regla inicial para " + nombre,
                    "GUIADA",
                    severidad,
                    prioridad,
                    "Condicion guiada JSON para " + nombre,
                    condicionesJson,
                    "[{\"codigo\":\"REVISION_MANUAL\",\"descripcion\":\"Revision manual\"}]",
                    escenarioCodigo,
                    score);
        } catch (Exception e) {
            log.warn("[SEED] No se pudo insertar regla {}: {}", codigo, e.getMessage());
        }
    }

    private void insert(String table, String column, String value, String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, value);
            if (count == null || count == 0) {
                jdbcTemplate.execute(sql);
            }
        } catch (Exception e) {
            log.warn("[SEED] No se pudo insertar {}.{}={}: {}", table, column, value, e.getMessage());
        }
    }
}
