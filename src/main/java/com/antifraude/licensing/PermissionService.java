package com.antifraude.licensing;

import com.antifraude.users.Usuario;
import com.antifraude.security.tenant.RlsContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PermissionService {

    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final RlsContextService rlsContextService;

    public PermissionService(UsuarioEmpresaRepository usuarioEmpresaRepository,
                             RolPermisoRepository rolPermisoRepository,
                             RlsContextService rlsContextService) {
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.rolPermisoRepository = rolPermisoRepository;
        this.rlsContextService = rlsContextService;
    }

    @Transactional(readOnly = true)
    public SessionAccess buildAccess(Usuario usuario) {
        rlsContextService.apply(null, usuario.getId());
        UsuarioEmpresa assignment = usuarioEmpresaRepository
                .findFirstByUsuarioIdAndActivoTrueOrderByIdAsc(usuario.getId())
                .orElse(null);
        String roleCode = assignment != null ? assignment.getRol().getCodigo() : "SIN_ROL";
        UUID empresaId = assignment != null && assignment.getEmpresa() != null ? assignment.getEmpresa().getId() : null;
        Long rolId = assignment != null ? assignment.getRol().getId() : null;

        Set<String> permisos = new LinkedHashSet<>(rolPermisoRepository.findPermisosByRolCodigo(roleCode));
        return new SessionAccess(empresaId, rolId, roleCode, List.copyOf(permisos));
    }

    public record SessionAccess(UUID empresaId, Long rolId, String rol, List<String> permisos) {}
}
