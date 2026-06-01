package com.antifraude.auth;

import com.antifraude.dto.LoginRequest;
import com.antifraude.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        log.info("[AUTH] Login attempt - Email: {} - IP: {}", request.email(), httpRequest.getRemoteAddr());
        LoginResponse response = authService.login(request, httpRequest);
        log.info("[AUTH] Login exitoso - Email: {} - IP: {}", request.email(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }
}
