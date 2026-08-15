package com.antifraude.exception;

import com.antifraude.dto.ApiErrorDescriptor;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ApiErrorCatalog {

    private static final List<ApiErrorDescriptor> ERRORS = List.of(
            internal("AUTH", "UNAUTHORIZED", 401, "Token de autenticacion requerido o invalido", "Sesion no autenticada o token expirado.", "SEGURIDAD"),
            internal("AUTH", "BAD_CREDENTIALS", 401, "Email o password incorrectos", "Credenciales invalidas en inicio de sesion.", "SEGURIDAD"),
            internal("AUTH", "ACCOUNT_LOCKED", 403, "La cuenta ha sido bloqueada por exceso de intentos fallidos", "Bloqueo temporal de seguridad.", "SEGURIDAD"),
            internal("AUTH", "ACCESS_DENIED", 403, "No tiene permisos para acceder a este recurso", "Permiso ausente para la operacion solicitada.", "SEGURIDAD"),
            internal("ADMIN_EMPRESA", "TENANT_REQUERIDO", 400, "No se pudo resolver la empresa del usuario autenticado", "El token no contiene empresa activa.", "TENANT"),
            internal("ADMIN_EMPRESA", "EMPRESA_NO_ENCONTRADA", 404, "Empresa no encontrada", "La empresa del tenant no existe o no esta activa.", "TENANT"),
            internal("LICENCIAMIENTO", "EMPRESA_INACTIVA", 400, "La empresa no se encuentra activa", "El enforcement de licencia bloqueo la operacion.", "LICENCIA"),
            internal("LICENCIAMIENTO", "MODULO_NO_INCLUIDO", 400, "El modulo no esta incluido en el plan contratado", "El plan vigente no habilita el modulo solicitado.", "LICENCIA"),
            internal("LICENCIAMIENTO", "LIMITE_TRANSACCIONES_PLAN", 429, "Se supero el limite mensual de transacciones del plan", "Consumo mensual excedido.", "CUOTA"),
            internal("LICENCIAMIENTO", "LIMITE_KYC_PLAN", 429, "Se supero el limite mensual de consultas KYC del plan", "Consumo mensual excedido.", "CUOTA"),
            internal("LICENCIAMIENTO", "LIMITE_REPORTES_PLAN", 429, "Se supero el limite mensual de reportes del plan", "Consumo mensual excedido.", "CUOTA"),
            internal("LICENCIAMIENTO", "MODO_SOLO_LECTURA", 403, "Licencia en periodo de gracia: solo operaciones de lectura disponibles", "El cliente puede consultar pero no mutar informacion.", "LICENCIA"),
            internal("ALERTAS", "ALERTA_NO_ENCONTRADA", 404, "Alerta no encontrada", "No existe la alerta solicitada para la empresa actual.", "NEGOCIO"),
            internal("ALERTAS", "ALERTA_CERRADA", 400, "La alerta cerrada no puede modificarse", "Se intento mutar una alerta ya finalizada.", "NEGOCIO"),
            internal("TRANSACCIONES", "TRANSACCION_INVALIDA", 400, "La transaccion no cumple el contrato requerido", "Faltan datos obligatorios o hay valores invalidos.", "VALIDACION"),
            internal("MOTOR_REGLAS", "REGLA_INVALIDA", 400, "La regla no cumple el contrato requerido", "Condicion, accion o catalogo invalido.", "VALIDACION"),
            internal("REPORTES", "REPORT_GENERATION_ERROR", 400, "Error al generar reporte ROS", "No se pudo construir el reporte solicitado.", "NEGOCIO"),
            internal("API_INTERNA", "VALIDATION_ERROR", 400, "Errores de validacion en los campos enviados", "El payload no cumple validaciones de entrada.", "VALIDACION"),
            internal("API_INTERNA", "INTERNAL_ERROR", 500, "Error interno del servidor", "Excepcion no controlada. Revisar logs backend.", "SISTEMA"),
            external("PROVEEDOR_EXTERNO", "TIMEOUT", 504, "Timeout al consultar proveedor externo", "La API externa no respondio dentro del tiempo maximo.", "EXTERNA"),
            external("PROVEEDOR_EXTERNO", "HTTP_TRANSITORIO", 502, "Error transitorio del proveedor externo", "El proveedor externo respondio 429 o 5xx y se aplico retry.", "EXTERNA"),
            external("PROVEEDOR_EXTERNO", "HTTP_NO_TRANSITORIO", 400, "Error no transitorio del proveedor externo", "El proveedor externo respondio 4xx no recuperable.", "EXTERNA"),
            external("PROVEEDOR_EXTERNO", "CONEXION_O_RESPUESTA", 503, "Fallo de conexion o respuesta externa", "No se pudo conectar, parsear o completar la respuesta del proveedor.", "EXTERNA"),
            external("IDENTIFICACIONES", "EXTERNAL_TIMEOUT", 504, "Timeout al consultar proveedor externo", "El proveedor externo no respondio dentro del tiempo maximo.", "EXTERNA"),
            external("IDENTIFICACIONES", "EXTERNAL_4XX", 400, "Proveedor externo rechazo la consulta", "El documento o payload fue rechazado por el proveedor.", "EXTERNA"),
            external("IDENTIFICACIONES", "EXTERNAL_5XX", 502, "Proveedor externo respondio con error", "Fallo del proveedor externo de identidad/KYC.", "EXTERNA"),
            external("BCP_SANCIONES", "EXTERNAL_TIMEOUT", 504, "Timeout al consultar listas/sanciones", "La fuente externa de sanciones no respondio a tiempo.", "EXTERNA"),
            external("SEPRELAD_PEP", "EXTERNAL_TIMEOUT", 504, "Timeout al consultar PEP", "La fuente externa PEP no respondio a tiempo.", "EXTERNA"),
            external("CONTROL_PLANE", "CONTROL_PLANE_NO_DISPONIBLE", 503, "Control Plane no disponible", "No se pudo validar licencia o sincronizar catalogos.", "EXTERNA"),
            external("CATALOG_SYNC", "CATALOGO_SYNC_ERROR", 503, "No se pudo sincronizar catalogos", "Fallo de comunicacion o versionamiento de catalogos.", "EXTERNA")
    );

    private ApiErrorCatalog() {
    }

    public static List<ApiErrorDescriptor> all() {
        return ERRORS;
    }

    public static Optional<ApiErrorDescriptor> find(String api, String code) {
        if (code == null) return Optional.empty();
        String normalizedApi = api != null ? api.toUpperCase(Locale.ROOT) : "";
        String normalizedCode = code.toUpperCase(Locale.ROOT);
        return ERRORS.stream()
                .filter(error -> error.codigoError().equalsIgnoreCase(normalizedCode)
                        && (normalizedApi.isBlank() || error.api().equalsIgnoreCase(normalizedApi)))
                .findFirst()
                .or(() -> ERRORS.stream().filter(error -> error.codigoError().equalsIgnoreCase(normalizedCode)).findFirst());
    }

    private static ApiErrorDescriptor internal(String api, String code, int status, String message,
                                               String detail, String category) {
        return new ApiErrorDescriptor(api, "INTERNA", api, code, status, message, detail, category, null);
    }

    private static ApiErrorDescriptor external(String api, String code, int status, String message,
                                               String detail, String category) {
        return new ApiErrorDescriptor(api, "EXTERNA", api, code, status, message, detail, category, null);
    }
}
