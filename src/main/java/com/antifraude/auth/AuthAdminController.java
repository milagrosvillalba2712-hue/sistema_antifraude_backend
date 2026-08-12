package com.antifraude.auth;

import com.antifraude.dto.InvitacionRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/invitaciones")
public class AuthAdminController {

    private final AuthService authService;

    public AuthAdminController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS_CREAR')")
    public ResponseEntity<AuthService.InvitacionEmitida> crear(@Valid @RequestBody InvitacionRequest request,
                                                               HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.crearInvitacion(request, httpRequest));
    }
}