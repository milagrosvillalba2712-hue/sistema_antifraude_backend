package com.antifraude.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.antifraude.security.tenant.TenantContext;
import com.antifraude.security.tenant.RlsContextService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditoriaRepository;
    private final RlsContextService rlsContextService;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, RlsContextService rlsContextService) {
        this.auditoriaRepository = auditoriaRepository;
        this.rlsContextService = rlsContextService;
    }

    public void registrar(UUID usuarioId, String accion, String descripcion,
                          String direccionIp, String entidadAfectada, Object entidadId) {
        registrar(usuarioId, TenantContext.getEmpresaId(), accion, descripcion, direccionIp, null, entidadAfectada,
                entidadId != null ? String.valueOf(entidadId) : null, null, null);
    }

    public void registrar(UUID usuarioId, UUID empresaId, String accion, String descripcion,
                          String direccionIp, String userAgent, String entidadAfectada, Object entidadId,
                          String valorAnteriorJson, String valorNuevoJson) {
        rlsContextService.apply(empresaId, usuarioId);
        String entidadIdTexto = entidadId != null ? String.valueOf(entidadId) : null;
        log.debug("[AUDIT] Registrando: {} - Usuario: {} - IP: {} - Entidad: {}:{}",
                accion, usuarioId, direccionIp, entidadAfectada, entidadIdTexto);
        Auditoria auditoria = Auditoria.builder()
                .usuarioId(usuarioId)
                .empresaId(empresaId)
                .accion(accion)
                .descripcion(descripcion)
                .direccionIp(direccionIp)
                .userAgent(userAgent)
                .entidadAfectada(entidadAfectada)
                .entidadId(entidadIdTexto)
                .valorAnteriorJson(valorAnteriorJson)
                .valorNuevoJson(valorNuevoJson)
                .build();
        auditoriaRepository.save(auditoria);
        log.debug("[AUDIT] Evento registrado - Accion: {} - Usuario: {}", accion, usuarioId);
    }

    public void registrar(Auditoria auditoria) {
        log.debug("[AUDIT] Registrando evento: {} - Usuario: {}", auditoria.getAccion(), auditoria.getUsuarioId());
        auditoriaRepository.save(auditoria);
    }

    public List<Auditoria> listarTodas() {
        log.debug("[AUDIT] Listando todos los eventos de auditoria");
        List<Auditoria> eventos = auditoriaRepository.findAll();
        log.debug("[AUDIT] Total eventos: {}", eventos.size());
        return eventos;
    }

    public List<Auditoria> buscarPorUsuario(UUID usuarioId) {
        log.debug("[AUDIT] Buscando eventos por usuario ID: {}", usuarioId);
        return auditoriaRepository.findByUsuarioIdOrderByFechaEventoDesc(usuarioId);
    }

    public List<Auditoria> buscarPorRangoFechas(OffsetDateTime inicio, OffsetDateTime fin) {
        log.debug("[AUDIT] Buscando eventos entre {} y {}", inicio, fin);
        return auditoriaRepository.findByFechaEventoBetween(inicio, fin);
    }
}
