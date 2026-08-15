package com.antifraude.users;

import com.antifraude.exception.BusinessException;
import com.antifraude.exception.ResourceNotFoundException;
import com.antifraude.licensing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final RolSistemaRepository rolSistemaRepository;
    private final EmpresaRepository empresaRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          UsuarioEmpresaRepository usuarioEmpresaRepository,
                          RolSistemaRepository rolSistemaRepository,
                          EmpresaRepository empresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.rolSistemaRepository = rolSistemaRepository;
        this.empresaRepository = empresaRepository;
    }

    public Usuario crearUsuario(Usuario usuario, String rolCodigo, UUID empresaId) {
        log.info("[USERS] Creando usuario: {}", usuario.getEmail());
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new BusinessException("DUPLICATE_EMAIL", "El email ya esta registrado: " + usuario.getEmail());
        }
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        Usuario creado = usuarioRepository.save(usuario);
        asignarRolPrincipal(creado, rolCodigo, empresaId);
        log.info("[USERS] Usuario creado exitosamente - ID: {} - Email: {}", creado.getId(), creado.getEmail());
        return creado;
    }

    public List<Usuario> listarTodos() {
        log.debug("[USERS] Listando todos los usuarios");
        List<Usuario> usuarios = usuarioRepository.findAll();
        log.debug("[USERS] Total usuarios encontrados: {}", usuarios.size());
        return usuarios;
    }

    public Usuario buscarPorId(UUID id) {
        log.debug("[USERS] Buscando usuario por ID: {}", id);
        return usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[USERS] Usuario no encontrado con ID: {}", id);
                    return new ResourceNotFoundException("Usuario", "id", id);
                });
    }

    public Usuario buscarPorEmail(String email) {
        log.debug("[USERS] Buscando usuario por email: {}", email);
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[USERS] Usuario no encontrado con email: {}", email);
                    return new ResourceNotFoundException("Usuario", "email", email);
                });
    }

    public Usuario actualizar(UUID id, Usuario actualizado, String rolCodigo, UUID empresaId) {
        log.info("[USERS] Actualizando usuario ID: {}", id);
        Usuario usuario = buscarPorId(id);
        usuario.setNombre(actualizado.getNombre());
        usuario.setEmail(actualizado.getEmail());
        if (actualizado.getPasswordHash() != null && !actualizado.getPasswordHash().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(actualizado.getPasswordHash()));
            log.debug("[USERS] Password actualizado para usuario ID: {}", id);
        }
        Usuario guardado = usuarioRepository.save(usuario);
        asignarRolPrincipal(guardado, rolCodigo, empresaId);
        log.info("[USERS] Usuario actualizado exitosamente - ID: {}", id);
        return guardado;
    }

    public UsuarioEmpresa asignacionPrincipal(Usuario usuario) {
        return usuarioEmpresaRepository.findFirstByUsuarioIdAndActivoTrueOrderByIdAsc(usuario.getId()).orElse(null);
    }

    public String rolPrincipal(Usuario usuario) {
        UsuarioEmpresa asignacion = asignacionPrincipal(usuario);
        return asignacion != null ? asignacion.getRol().getCodigo() : "SIN_ROL";
    }

    public void desactivar(UUID id) {
        log.info("[USERS] Desactivando usuario ID: {}", id);
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        log.info("[USERS] Usuario desactivado exitosamente - ID: {} - Email: {}", id, usuario.getEmail());
    }

    private void asignarRolPrincipal(Usuario usuario, String rolCodigo, UUID empresaId) {
        RolSistema rol = rolSistemaRepository.findByCodigo(rolCodigo)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Rol no encontrado: " + rolCodigo));
        Empresa empresa = empresaId != null
                ? empresaRepository.findById(empresaId).orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", empresaId))
                : null;
        usuarioEmpresaRepository.findByUsuarioIdAndActivoTrue(usuario.getId())
                .forEach(asignacion -> {
                    asignacion.setActivo(false);
                    usuarioEmpresaRepository.save(asignacion);
                });
        usuarioEmpresaRepository.save(UsuarioEmpresa.builder()
                .usuario(usuario)
                .empresa(empresa)
                .rol(rol)
                .activo(true)
                .build());
    }
}
