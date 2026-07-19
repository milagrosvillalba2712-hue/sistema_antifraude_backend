package com.antifraude.config;

import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        for (int i = 1; i <= 3; i++) {
            crearUsuarioSiNoExiste("admin.general" + i + "@regula.com", "Admin General " + i, "password");
            crearUsuarioSiNoExiste("admin.empresa" + i + "@demo.com", "Admin Empresa " + i, "password");
            crearUsuarioSiNoExiste("supervisor" + i + "@demo.com", "Gerente Supervisor " + i, "password");
            crearUsuarioSiNoExiste("auditor" + i + "@demo.com", "Auditor " + i, "password");
        }
        for (int i = 1; i <= 10; i++) {
            crearUsuarioSiNoExiste("analista" + i + "@demo.com", "Analista " + i, "password");
        }
    }

    private void crearUsuarioSiNoExiste(String email, String nombre, String rawPassword) {
        if (!usuarioRepository.existsByEmail(email)) {
            Usuario usuario = Usuario.builder()
                    .nombre(nombre)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .activo(true)
                    .intentosFallidos(0)
                    .build();
            usuarioRepository.save(usuario);
        }
    }
}
