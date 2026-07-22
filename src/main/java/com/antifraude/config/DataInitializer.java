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
        crearUsuarioSiNoExiste("admin.general1@regula.com", "Valeria Duarte Benitez", "password");
        crearUsuarioSiNoExiste("admin.general2@regula.com", "Hugo Ramirez Aquino", "password");
        crearUsuarioSiNoExiste("admin.general3@regula.com", "Claudia Vera Sosa", "password");

        crearUsuarioSiNoExiste("admin.empresa1@demo.com", "Marcelo Gimenez Franco", "password");
        crearUsuarioSiNoExiste("admin.empresa2@demo.com", "Patricia Rojas Caballero", "password");
        crearUsuarioSiNoExiste("admin.empresa3@demo.com", "Federico Lopez Ortega", "password");

        crearUsuarioSiNoExiste("supervisor1@demo.com", "Sofia Martinez Lezcano", "password");
        crearUsuarioSiNoExiste("supervisor2@demo.com", "Ricardo Villalba Acosta", "password");
        crearUsuarioSiNoExiste("supervisor3@demo.com", "Gabriela Aquino Torres", "password");

        crearUsuarioSiNoExiste("auditor1@demo.com", "Daniel Pereira Caceres", "password");
        crearUsuarioSiNoExiste("auditor2@demo.com", "Lorena Benitez Arce", "password");
        crearUsuarioSiNoExiste("auditor3@demo.com", "Esteban Rios Ferreira", "password");
        crearUsuarioSiNoExiste("auditor4@demo.com", "Noelia Caballero Ortiz", "password");

        crearUsuarioSiNoExiste("analista1@demo.com", "Ana Patricia Gomez Riveros", "password");
        crearUsuarioSiNoExiste("analista2@demo.com", "Jorge Luis Medina Torres", "password");
        crearUsuarioSiNoExiste("analista3@demo.com", "Mariana Isabel Cabrera Nuñez", "password");
        crearUsuarioSiNoExiste("analista4@demo.com", "Luis Alberto Sosa Mendez", "password");
        crearUsuarioSiNoExiste("analista5@demo.com", "Carolina Beatriz Ferreira Diaz", "password");
        crearUsuarioSiNoExiste("analista6@demo.com", "Victor Hugo Riquelme Vera", "password");
        crearUsuarioSiNoExiste("analista7@demo.com", "Natalia Andrea Barrios Cano", "password");
        crearUsuarioSiNoExiste("analista8@demo.com", "Pablo Enrique Salinas Morinigo", "password");
        crearUsuarioSiNoExiste("analista9@demo.com", "Rosa Elena Centurion Ayala", "password");
        crearUsuarioSiNoExiste("analista10@demo.com", "Miguel Angel Torres Fariña", "password");
        crearUsuarioSiNoExiste("analista11@demo.com", "Andrea Beatriz Franco Ibarra", "password");
        crearUsuarioSiNoExiste("analista12@demo.com", "Diego Fernando Benitez Ruiz", "password");
        crearUsuarioSiNoExiste("analista13@demo.com", "Lucia Mercedes Acosta Paredes", "password");
        crearUsuarioSiNoExiste("analista14@demo.com", "Raul Antonio Duarte Medina", "password");
        crearUsuarioSiNoExiste("analista15@demo.com", "Marta Carolina Villagra Lezcano", "password");
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
