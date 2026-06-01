package com.antifraude.config;

import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        crearUsuarioSiNoExiste("admin@antifraude.com", "Admin", "password", "ADMINISTRADOR");
        crearUsuarioSiNoExiste("analista@antifraude.com", "Analista", "password", "ANALISTA");
    }

    private void crearUsuarioSiNoExiste(String email, String nombre, String rawPassword, String rol) {
        if (!usuarioRepository.existsByEmail(email)) {
            Usuario usuario = Usuario.builder()
                    .nombre(nombre)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .rol(rol)
                    .activo(true)
                    .intentosFallidos(0)
                    .build();
            usuarioRepository.save(usuario);
        }
    }
}
