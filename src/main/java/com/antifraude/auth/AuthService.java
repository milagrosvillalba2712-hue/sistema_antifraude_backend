package com.antifraude.auth;

import com.antifraude.audit.AuditoriaService;
import com.antifraude.dto.LoginRequest;
import com.antifraude.dto.LoginResponse;
import com.antifraude.exception.AuthenticationErrorException;
import com.antifraude.security.JwtTokenProvider;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
                       UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuditoriaService auditoriaService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();

        try {
            log.debug("[AUTH] Autenticando usuario: {} - IP: {}", request.email(), ip);
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException e) {
            log.warn("[AUTH] Credenciales incorrectas para {} - IP: {}", request.email(), ip);
            incrementarIntentosFallidos(request.email());
            throw e;
        } catch (LockedException e) {
            log.warn("[AUTH] Cuenta bloqueada para {} - IP: {}", request.email(), ip);
            throw e;
        } catch (DisabledException e) {
            log.warn("[AUTH] Cuenta deshabilitada para {} - IP: {}", request.email(), ip);
            throw e;
        }

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.error("[AUTH] Usuario autenticado pero no encontrado en BD: {}", request.email());
                    return new AuthenticationErrorException("Usuario no encontrado");
                });

        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);

        String token = jwtTokenProvider.generateToken(usuario.getEmail(), usuario.getRol());
        log.info("[AUTH] Token generado para {} - Rol: {} - IP: {}", usuario.getEmail(), usuario.getRol(), ip);

        auditoriaService.registrar(usuario.getId(), "LOGIN", "Inicio de sesion exitoso",
                ip, "usuarios", usuario.getId());

        return new LoginResponse(token, "Bearer", usuario.getEmail(), usuario.getRol());
    }

    public void registrarUsuario(Usuario usuario) {
        log.info("[AUTH] Registrando nuevo usuario: {}", usuario.getEmail());
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new AuthenticationErrorException("El email ya esta registrado");
        }
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        usuarioRepository.save(usuario);
        log.info("[AUTH] Usuario registrado exitosamente: {}", usuario.getEmail());
    }

    private void incrementarIntentosFallidos(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            int intentos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(intentos);
            usuarioRepository.save(usuario);
            log.warn("[AUTH] Intentos fallidos para {}: {}/5 - IP: {}", email, intentos);
        });
    }
}
