package com.antifraude.licensing;

import com.antifraude.users.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PermissionService {

    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final RolPermisoRepository rolPermisoRepository;

    public PermissionService(UsuarioEmpresaRepository usuarioEmpresaRepository,
                             RolPermisoRepository rolPermisoRepository) {
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.rolPermisoRepository = rolPermisoRepository;
    }

    @Transactional(readOnly = true)
    public SessionAccess buildAccess(Usuario usuario) {
        UsuarioEmpresa assignment = usuarioEmpresaRepository
                .findFirstByUsuarioIdAndActivoTrueOrderByIdAsc(usuario.getId())
                .orElse(null);
        String roleCode = assignment != null ? assignment.getRol().getCodigo() : "SIN_ROL";
        Long empresaId = assignment != null && assignment.getEmpresa() != null ? assignment.getEmpresa().getId() : null;
        Long rolId = assignment != null ? assignment.getRol().getId() : null;

        Set<String> permisos = new LinkedHashSet<>(rolPermisoRepository.findPermisosByRolCodigo(roleCode));
        return new SessionAccess(empresaId, rolId, roleCode, List.copyOf(permisos));
    }

    public record SessionAccess(Long empresaId, Long rolId, String rol, List<String> permisos) {}
}
