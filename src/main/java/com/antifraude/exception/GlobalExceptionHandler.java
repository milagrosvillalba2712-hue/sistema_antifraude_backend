package com.antifraude.exception;

import com.antifraude.dto.ErrorResponse;
import com.antifraude.audit.AuditoriaService;
import com.antifraude.config.ClientIpResolver;
import com.antifraude.external.ExternalProviderException;
import com.antifraude.observability.ApiEventoService;
import com.antifraude.security.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final AuditoriaService auditoriaService;

    public GlobalExceptionHandler(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                                                                HttpServletRequest request) {
        log.warn("[NOT_FOUND] {} - Ruta: {}", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, null));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(QuotaExceededException ex,
                                                             HttpServletRequest request) {
        log.warn("[QUOTA] {} [{}] - Ruta: {}", ex.getMessage(), ex.getCode(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(error(HttpStatus.TOO_MANY_REQUESTS, ex.getCode(), ex.getMessage(), request, null));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex,
                                                        HttpServletRequest request) {
        log.warn("[BUSINESS] {} [{}] - Ruta: {}", ex.getMessage(), ex.getCode(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request, null));
    }

    @ExceptionHandler(ValidationErrorException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationErrorException ex,
                                                          HttpServletRequest request) {
        log.warn("[VALIDATION] {} - Ruta: {} - Campos: {}", ex.getMessage(), request.getRequestURI(), ex.getFieldErrors());
        markApiError(request, "VALIDATION_ERROR", "VALIDACION", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        ex.getMessage(),
                        request.getRequestURI(),
                        ex.getFieldErrors()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                fieldErrors.put(error.getObjectName(), error.getDefaultMessage());
            }
        });

        log.warn("[VALIDATION] Error de validacion en {} - Campos: {}", request.getRequestURI(), fieldErrors);
        markApiError(request, "VALIDATION_ERROR", "VALIDACION", "Errores de validacion en los campos enviados");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        "Errores de validacion en los campos enviados",
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(AuthenticationErrorException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationErrorException ex,
                                                              HttpServletRequest request) {
        String code = ex.getCode() != null ? ex.getCode() : "AUTHENTICATION_ERROR";
        log.warn("[AUTH] Fallo de autenticacion [{}]: {} - IP: {} - Ruta: {}",
                code, ex.getMessage(), request.getRemoteAddr(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error(HttpStatus.UNAUTHORIZED, code, ex.getMessage(), request, null));
    }

    @SuppressWarnings("unchecked")
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                              HttpServletRequest request) {
        log.warn("[AUTH] Contraseña incorrecta - IP: {} - Ruta: {}",
                request.getRemoteAddr(), request.getRequestURI());
        Map<String, Object> detalles = (Map<String, Object>) request.getAttribute("bad_credentials_details");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error(HttpStatus.UNAUTHORIZED, "BAD_PASSWORD", "Contraseña incorrecta", request, detalles));
    }

    @SuppressWarnings("unchecked")
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex,
                                                      HttpServletRequest request) {
        log.warn("[AUTH] Cuenta bloqueada: {} - IP: {} - Ruta: {}",
                ex.getMessage(), request.getRemoteAddr(), request.getRequestURI());
        Map<String, Object> detalles = (Map<String, Object>) request.getAttribute("lockout_details");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED",
                        "La cuenta ha sido bloqueada por exceso de intentos fallidos", request, detalles));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex,
                                                        HttpServletRequest request) {
        log.warn("[AUTH] Cuenta deshabilitada: {} - IP: {} - Ruta: {}",
                ex.getMessage(), request.getRemoteAddr(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED",
                        "La cuenta se encuentra deshabilitada", request, null));
    }

    @ExceptionHandler(AuthorizationErrorException.class)
    public ResponseEntity<ErrorResponse> handleAuthorization(AuthorizationErrorException ex,
                                                             HttpServletRequest request) {
        log.warn("[AUTH] Fallo de autorizacion: {} - IP: {} - Ruta: {}",
                ex.getMessage(), request.getRemoteAddr(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, "AUTHORIZATION_ERROR", ex.getMessage(), request, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        log.warn("[AUTH] Acceso denegado: {} - IP: {} - Ruta: {}",
                ex.getMessage(), request.getRemoteAddr(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                        "No tiene permisos para acceder a este recurso", request, null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        log.warn("[METHOD] Metodo no soportado: {} - Ruta: {}",
                ex.getMethod(), request.getRequestURI());
        String[] metodos = ex.getSupportedMethods() != null ? ex.getSupportedMethods() : new String[0];
        String soportados = String.join(", ", metodos);
        Map<String, Object> detalles = Map.of(
                "metodo", ex.getMethod() != null ? ex.getMethod() : "",
                "soportados", soportados
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                        "Método HTTP no soportado para este recurso", request, detalles));
    }

    @ExceptionHandler(ExternalProviderException.class)
    public ResponseEntity<ErrorResponse> handleExternalProvider(ExternalProviderException ex,
                                                                HttpServletRequest request) {
        int status = ex.statusHttp() >= 400 && ex.statusHttp() < 600
                ? ex.statusHttp()
                : HttpStatus.SERVICE_UNAVAILABLE.value();
        String code = ex.category() != null && !ex.category().isBlank() ? ex.category() : "EXTERNAL_PROVIDER_ERROR";
        log.warn("[EXTERNAL] {} [{}] status={} correlation={} attempts={} ruta={}",
                ex.provider(), code, status, ex.correlationId(), ex.attempts(), request.getRequestURI());
        Map<String, Object> detalles = Map.of(
                "proveedor", ex.provider(),
                "correlationId", ex.correlationId(),
                "duracionMs", ex.durationMs(),
                "intentos", ex.attempts()
        );
        markApiError(request, code, "ERROR_PROVEEDOR_EXTERNO", ex.getMessage());
        auditApiError(status, code, ex.getMessage(), request, detalles);
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, code, ex.getMessage(), request.getRequestURI(),
                        ex.provider(), detalles));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex,
                                                       HttpServletRequest request) {
        log.error("[ERROR] Excepcion no controlada en {} - Tipo: {} - Mensaje: {}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                        "Error interno del servidor", request,
                        Map.of("tipo", ex.getClass().getSimpleName())));
    }

    private ErrorResponse error(HttpStatus status, String code, String message,
                                HttpServletRequest request, Map<String, Object> details) {
        markApiError(request, code, status.is5xxServerError() ? "ERROR_SERVIDOR" : "ERROR_NEGOCIO", message);
        auditApiError(status.value(), code, message, request, details);
        return ErrorResponse.of(status.value(), code, message, request.getRequestURI(), null, details);
    }

    private void markApiError(HttpServletRequest request, String code, String category, String message) {
        if (request == null) {
            return;
        }
        request.setAttribute(ApiEventoService.ATTR_CODIGO_ERROR, code);
        request.setAttribute(ApiEventoService.ATTR_CATEGORIA_ERROR, category);
        request.setAttribute(ApiEventoService.ATTR_MENSAJE_ERROR, message);
    }

    private void auditApiError(int status, String code, String message,
                               HttpServletRequest request, Map<String, Object> details) {
        if (request == null || request.getRequestURI() == null || !request.getRequestURI().startsWith("/api/")) {
            return;
        }
        try {
            String detalle = "{\"status\":" + status
                    + ",\"codigo\":\"" + jsonSafe(code) + "\""
                    + ",\"path\":\"" + jsonSafe(request.getRequestURI()) + "\""
                    + ",\"mensaje\":\"" + jsonSafe(message) + "\""
                    + (details != null ? ",\"detalles\":\"" + jsonSafe(details.toString()) + "\"" : "")
                    + "}";
            auditoriaService.registrar(TenantContext.getUsuarioId(), TenantContext.getEmpresaId(),
                    "API_ERROR", "Error API " + status + " en " + request.getRequestURI(),
                    ClientIpResolver.resolve(request), request.getHeader("User-Agent"),
                    "api_endpoint", request.getRequestURI(), null, detalle);
        } catch (RuntimeException auditError) {
            log.debug("[AUDIT] No se pudo registrar error API en auditoria_sistema: {}", auditError.getMessage());
        }
    }

    private String jsonSafe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
