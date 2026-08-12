package com.antifraude.auth;

import com.antifraude.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<MensajeResponse> register(@Valid @RequestBody RegisterRequest request,
                                                     HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.registrar(request, httpRequest));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<MensajeResponse> verifyEmail(@RequestParam String codigo) {
        return ResponseEntity.ok(authService.verificarEmail(codigo));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MensajeResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                           HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.solicitarRecuperacion(request, httpRequest));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MensajeResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.restablecerPassword(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MensajeResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                           Authentication authentication,
                                                           HttpServletRequest httpRequest) {
        String email = authentication.getName();
        return ResponseEntity.ok(authService.cambiarPassword(email, request, httpRequest));
    }
}