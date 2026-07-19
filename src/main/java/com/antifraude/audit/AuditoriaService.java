package com.antifraude.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(Long usuarioId, String accion, String descripcion,
                          String direccionIp, String entidadAfectada, Long entidadId) {
        registrar(usuarioId, null, accion, descripcion, direccionIp, null, entidadAfectada, entidadId, null, null);
    }

    public void registrar(Long usuarioId, Long empresaId, String accion, String descripcion,
                          String direccionIp, String userAgent, String entidadAfectada, Long entidadId,
                          String valorAnteriorJson, String valorNuevoJson) {
        log.debug("[AUDIT] Registrando: {} - Usuario: {} - IP: {} - Entidad: {}:{}",
                accion, usuarioId, direccionIp, entidadAfectada, entidadId);
        Auditoria auditoria = Auditoria.builder()
                .usuarioId(usuarioId)
                .empresaId(empresaId)
                .accion(accion)
                .descripcion(descripcion)
                .direccionIp(direccionIp)
                .userAgent(userAgent)
                .entidadAfectada(entidadAfectada)
                .entidadId(entidadId)
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

    public List<Auditoria> buscarPorUsuario(Long usuarioId) {
        log.debug("[AUDIT] Buscando eventos por usuario ID: {}", usuarioId);
        return auditoriaRepository.findByUsuarioIdOrderByFechaEventoDesc(usuarioId);
    }

    public List<Auditoria> buscarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        log.debug("[AUDIT] Buscando eventos entre {} y {}", inicio, fin);
        return auditoriaRepository.findByFechaEventoBetween(inicio, fin);
    }
}
