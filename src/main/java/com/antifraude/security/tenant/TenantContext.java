package com.antifraude.security.tenant;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_EMPRESA_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_USUARIO_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setEmpresaId(UUID empresaId) {
        CURRENT_EMPRESA_ID.set(empresaId);
    }

    public static UUID getEmpresaId() {
        return CURRENT_EMPRESA_ID.get();
    }

    public static void setUsuarioId(UUID usuarioId) {
        CURRENT_USUARIO_ID.set(usuarioId);
    }

    public static UUID getUsuarioId() {
        return CURRENT_USUARIO_ID.get();
    }

    public static void clear() {
        CURRENT_EMPRESA_ID.remove();
        CURRENT_USUARIO_ID.remove();
    }
}
