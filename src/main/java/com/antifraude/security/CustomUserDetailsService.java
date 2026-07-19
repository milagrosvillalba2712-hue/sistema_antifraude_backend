package com.antifraude.security;

import com.antifraude.licensing.PermissionService;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PermissionService permissionService;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository, PermissionService permissionService) {
        this.usuarioRepository = usuarioRepository;
        this.permissionService = permissionService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
        PermissionService.SessionAccess access = permissionService.buildAccess(usuario);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + access.rol()));
        access.permisos().forEach(permiso -> authorities.add(new SimpleGrantedAuthority(permiso)));
        return new User(usuario.getEmail(), usuario.getPasswordHash(), authorities);
    }
}
