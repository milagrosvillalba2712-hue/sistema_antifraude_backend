package com.antifraude.exception;

import com.antifraude.dto.ErrorResponse;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                                                                HttpServletRequest request) {
        log.warn("[NOT_FOUND] {} - Ruta: {}", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex,
                                                        HttpServletRequest request) {
        log.warn("[BUSINESS] {} [{}] - Ruta: {}", ex.getMessage(), ex.getCode(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getCode(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ValidationErrorException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationErrorException ex,
                                                          HttpServletRequest request) {
        log.warn("[VALIDATION] {} - Ruta: {} - Campos: {}", ex.getMessage(), request.getRequestURI(), ex.getFieldErrors());
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
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        log.warn("[VALIDATION] Error de validacion en {} - Campos: {}", request.getRequestURI(), fieldErrors);
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
        log.warn("[AUTH] Fallo de autenticacion: {} - IP: {} - Ruta: {}",
                ex.getMessage(), request.getRemoteAddr(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "AUTHENTICATION_ERROR",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                              HttpServletRequest request) {
        log.warn("[AUTH] Credenciales invalidas para IP: {} - Ruta: {}",
                request.getRemoteAddr(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "BAD_CREDENTIALS",
                        "Email o password incorrectos",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex,
                                                      HttpServletRequest request) {
        log.warn("[AUTH] Cuenta bloqueada: {} - IP: {} - Ruta: {}",
                ex.getMessage(), request.getRemoteAddr(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "ACCOUNT_LOCKED",
                        "La cuenta ha sido bloqueada por exceso de intentos fallidos",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex,
                                                        HttpServletRequest request) {
        log.warn("[AUTH] Cuenta deshabilitada: {} - IP: {} - Ruta: {}",
                ex.getMessage(), request.getRemoteAddr(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "ACCOUNT_DISABLED",
                        "La cuenta se encuentra deshabilitada",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AuthorizationErrorException.class)
    public ResponseEntity<ErrorResponse> handleAuthorization(AuthorizationErrorException ex,
                                                             HttpServletRequest request) {
        log.warn("[AUTH] Fallo de autorizacion: {} - IP: {} - Ruta: {}",
                ex.getMessage(), request.getRemoteAddr(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "AUTHORIZATION_ERROR",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        log.warn("[AUTH] Acceso denegado: {} - IP: {} - Ruta: {}",
                ex.getMessage(), request.getRemoteAddr(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "ACCESS_DENIED",
                        "No tiene permisos para acceder a este recurso",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex,
                                                       HttpServletRequest request) {
        log.error("[ERROR] Excepcion no controlada en {} - Tipo: {} - Mensaje: {}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_ERROR",
                        "Error interno del servidor",
                        request.getRequestURI()
                ));
    }
}
