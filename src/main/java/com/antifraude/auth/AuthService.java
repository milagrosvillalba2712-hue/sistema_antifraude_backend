package com.antifraude.auth;

import com.antifraude.audit.AuditoriaService;
import com.antifraude.dto.*;
import com.antifraude.exception.AuthenticationErrorException;
import com.antifraude.exception.BusinessException;
import com.antifraude.licensing.*;
import com.antifraude.security.JwtTokenProvider;
import com.antifraude.security.tenant.RlsContextService;
import com.antifraude.security.tenant.TenantContext;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final PermissionService permissionService;
    private final LoginRateLimiter loginRateLimiter;
    private final AuthTokenService authTokenService;
    private final AuthLinkSender authLinkSender;
    private final RolSistemaRepository rolSistemaRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final RlsContextService rlsContextService;
    private final boolean requireEmailVerified;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
                       UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuditoriaService auditoriaService, PermissionService permissionService,
                       LoginRateLimiter loginRateLimiter, AuthTokenService authTokenService,
                       AuthLinkSender authLinkSender, RolSistemaRepository rolSistemaRepository,
                       EmpresaRepository empresaRepository, UsuarioEmpresaRepository usuarioEmpresaRepository,
                       RlsContextService rlsContextService,
                       @Value("${app.auth.require-email-verified:false}") boolean requireEmailVerified) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.permissionService = permissionService;
        this.loginRateLimiter = loginRateLimiter;
        this.authTokenService = authTokenService;
        this.authLinkSender = authLinkSender;
        this.rolSistemaRepository = rolSistemaRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.rlsContextService = rlsContextService;
        this.requireEmailVerified = requireEmailVerified;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        loginRateLimiter.verificar(ip);

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

        usuario.resetFailedAttempts();
        usuarioRepository.save(usuario);

        PermissionService.SessionAccess access = permissionService.buildAccess(usuario);
        TenantContext.setUsuarioId(usuario.getId());
        TenantContext.setEmpresaId(access.empresaId());
        String token = jwtTokenProvider.generateToken(usuario.getEmail(), usuario.getId(), access.rol(),
                access.empresaId(), access.rolId(), access.permisos());
        log.info("[AUTH] Token generado para {} - Rol: {} - IP: {}", usuario.getEmail(), access.rol(), ip);

        auditoriaService.registrar(usuario.getId(), "LOGIN", "Inicio de sesion exitoso",
                ip, "usuarios", usuario.getId());

        return new LoginResponse(token, "Bearer", usuario.getId(), usuario.getEmail(), access.rol(),
                access.empresaId(), access.rolId(), access.permisos());
    }

    /**
     * Registro regulado (opcion A): el codigo de invitacion es obligatorio y define
     * empresa + rol. Respuesta generica para evitar enumeracion.
     */
    @Transactional
    public MensajeResponse registrar(RegisterRequest request, HttpServletRequest httpRequest) {
        loginRateLimiter.verificar(httpRequest.getRemoteAddr());
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_YA_REGISTRADO",
                    "No se pudo completar el registro. Revisa los datos e intenta nuevamente");
        }

        AuthToken invitacion = authTokenService.consumir(AuthToken.TIPO_INVITACION, request.codigoInvitacion());
        if (invitacion.getEmail() != null && !invitacion.getEmail().equalsIgnoreCase(request.email())) {
            throw new BusinessException("EMAIL_INVITACION_NO_COINCIDE",
                    "El email no coincide con la invitacion emitida");
        }
        PasswordPolicy.validar(request.password());

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .activo(true)
                .emailVerificado(false)
                .build();
        usuario = usuarioRepository.save(usuario);

        asignarRolPorInvitacion(usuario, invitacion);

        String codigoVerificacion = authTokenService.crearVerificacion(usuario.getId(), usuario.getEmail(), 60);
        authLinkSender.enviarVerificacion(usuario.getEmail(), codigoVerificacion);
        auditoriaService.registrar(usuario.getId(), invitacion.getEmpresaId(), "REGISTRO_USUARIO",
                "Usuario registrado por invitacion", httpRequest.getRemoteAddr(), null, "usuarios",
                usuario.getId(), null, null);
        log.info("[AUTH] Registro por invitacion: {} - empresa: {}", usuario.getEmail(), invitacion.getEmpresaId());
        return new MensajeResponse("Registro recibido. Revisa tu bandeja para verificar el email.");
    }

    @Transactional
    public MensajeResponse verificarEmail(String codigo) {
        AuthToken token = authTokenService.consumir(AuthToken.TIPO_VERIFICACION, codigo);
        Usuario usuario = usuarioRepository.findById(token.getUsuarioId())
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);
        log.info("[AUTH] Email verificado: {}", usuario.getEmail());
        return new MensajeResponse("Email verificado correctamente. Ya puedes iniciar sesion.");
    }

    @Transactional
    public MensajeResponse solicitarRecuperacion(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        loginRateLimiter.verificar(httpRequest.getRemoteAddr());
        usuarioRepository.findByEmail(request.email()).ifPresent(usuario -> {
            authTokenService.revocarPorUsuario(usuario.getId(), AuthToken.TIPO_RESET);
            String codigo = authTokenService.crearRecuperacion(usuario.getId(), usuario.getEmail(), 30);
            authLinkSender.enviarRecuperacion(usuario.getEmail(), codigo);
            auditoriaService.registrar(usuario.getId(), "SOLICITAR_RECUPERACION",
                    "Se solicito recuperacion de contrasena", httpRequest.getRemoteAddr(), "usuarios", usuario.getId());
        });
        return new MensajeResponse("Si el email existe, recibiras un enlace para restablecer tu contrasena.");
    }

    @Transactional
    public MensajeResponse restablecerPassword(ResetPasswordRequest request) {
        PasswordPolicy.validar(request.nuevaPassword());
        AuthToken token = authTokenService.consumir(AuthToken.TIPO_RESET, request.codigo());
        Usuario usuario = usuarioRepository.findById(token.getUsuarioId())
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
        usuario.setPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        usuario.setContrasenaCambiadaEn(OffsetDateTime.now());
        usuario.resetFailedAttempts();
        usuarioRepository.save(usuario);
        authTokenService.revocarPorUsuario(usuario.getId(), AuthToken.TIPO_RESET);
        log.info("[AUTH] Contrasena restablecida: {}", usuario.getEmail());
        return new MensajeResponse("Contrasena restablecida. Ya puedes iniciar sesion.");
    }

    @Transactional
    public MensajeResponse cambiarPassword(String email, ChangePasswordRequest request, HttpServletRequest httpRequest) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new BusinessException("PASSWORD_ACTUAL_INCORRECTA", "La contrasena actual es incorrecta");
        }
        PasswordPolicy.validar(request.nuevaPassword());
        usuario.setPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        usuario.setContrasenaCambiadaEn(OffsetDateTime.now());
        usuario.resetFailedAttempts();
        usuarioRepository.save(usuario);
        auditoriaService.registrar(usuario.getId(), "CAMBIAR_PASSWORD",
                "Contrasena actualizada", httpRequest.getRemoteAddr(), "usuarios", usuario.getId());
        log.info("[AUTH] Contrasena actualizada: {}", usuario.getEmail());
        return new MensajeResponse("Contrasena actualizada correctamente.");
    }

    /** Emision de invitacion (admin): devuelve una sola vez el codigo en claro. */
    @Transactional
    public InvitacionEmitida crearInvitacion(InvitacionRequest request, HttpServletRequest httpRequest) {
        RolSistema rol = rolSistemaRepository.findByCodigo(request.rol())
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Rol no encontrado: " + request.rol()));
        Empresa empresa = empresaRepository.findById(request.empresaId())
                .orElseThrow(() -> new BusinessException("EMPRESA_NO_ENCONTRADA", "Empresa no encontrada"));
        String codigo = authTokenService.crearInvitacion(empresa.getId(), rol.getId(), request.email(), 7 * 24 * 60);
        auditoriaService.registrar(TenantContext.getUsuarioId(), empresa.getId(), "CREAR_INVITACION",
                "Invitacion emitida para rol " + rol.getCodigo(), httpRequest.getRemoteAddr(), null,
                "auth_token", empresa.getId(), null, null);
        log.info("[AUTH] Invitacion emitida: empresa {} - rol {}", empresa.getId(), rol.getCodigo());
        return new InvitacionEmitida(request.email(), rol.getCodigo(), empresa.getId(), empresa.getNombre(), codigo);
    }

    public record InvitacionEmitida(String email, String rol, UUID empresaId, String empresa, String codigo) {
    }

    private void asignarRolPorInvitacion(Usuario usuario, AuthToken invitacion) {
        RolSistema rol = rolSistemaRepository.findById(invitacion.getRolId())
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Rol de invitacion invalido"));
        Empresa empresa = invitacion.getEmpresaId() != null
                ? empresaRepository.findById(invitacion.getEmpresaId())
                        .orElseThrow(() -> new BusinessException("EMPRESA_NO_ENCONTRADA", "Empresa de invitacion invalida"))
                : null;
        rlsContextService.apply(empresa != null ? empresa.getId() : null, usuario.getId());
        try {
            usuarioEmpresaRepository.save(UsuarioEmpresa.builder()
                    .usuario(usuario)
                    .empresa(empresa)
                    .rol(rol)
                    .activo(true)
                    .build());
        } finally {
            rlsContextService.apply(null, null);
        }
    }

    public void registrarUsuario(Usuario usuario) {
        log.info("[AUTH] Registrando nuevo usuario: {}", usuario.getEmail());
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new BusinessException("EMAIL_YA_REGISTRADO", "El email ya esta registrado");
        }
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        usuarioRepository.save(usuario);
        log.info("[AUTH] Usuario registrado exitosamente: {}", usuario.getEmail());
    }

    private void incrementarIntentosFallidos(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            usuario.incrementFailedAttempts();
            usuarioRepository.save(usuario);
            log.warn("[AUTH] Intentos fallidos para {}: {}/5", email, usuario.getIntentosFallidos());
        });
    }
}