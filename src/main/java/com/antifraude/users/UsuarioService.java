package com.antifraude.users;

import com.antifraude.exception.BusinessException;
import com.antifraude.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario crearUsuario(Usuario usuario) {
        log.info("[USERS] Creando usuario: {}", usuario.getEmail());
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new BusinessException("DUPLICATE_EMAIL", "El email ya esta registrado: " + usuario.getEmail());
        }
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        Usuario creado = usuarioRepository.save(usuario);
        log.info("[USERS] Usuario creado exitosamente - ID: {} - Email: {}", creado.getId(), creado.getEmail());
        return creado;
    }

    public List<Usuario> listarTodos() {
        log.debug("[USERS] Listando todos los usuarios");
        List<Usuario> usuarios = usuarioRepository.findAll();
        log.debug("[USERS] Total usuarios encontrados: {}", usuarios.size());
        return usuarios;
    }

    public Usuario buscarPorId(Long id) {
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

    public Usuario actualizar(Long id, Usuario actualizado) {
        log.info("[USERS] Actualizando usuario ID: {}", id);
        Usuario usuario = buscarPorId(id);
        usuario.setNombre(actualizado.getNombre());
        usuario.setRol(actualizado.getRol());
        if (actualizado.getPasswordHash() != null && !actualizado.getPasswordHash().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(actualizado.getPasswordHash()));
            log.debug("[USERS] Password actualizado para usuario ID: {}", id);
        }
        Usuario guardado = usuarioRepository.save(usuario);
        log.info("[USERS] Usuario actualizado exitosamente - ID: {}", id);
        return guardado;
    }

    public void desactivar(Long id) {
        log.info("[USERS] Desactivando usuario ID: {}", id);
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        log.info("[USERS] Usuario desactivado exitosamente - ID: {} - Email: {}", id, usuario.getEmail());
    }
}
