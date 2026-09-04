package com.antifraude.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Job que drena la cola de AppLogAppender y persiste los eventos en app_log
 * fuera de la transaccion del request. Usa un lote por tick y nunca propaga
 * errores hacia la aplicacion. Cada fila se inserta en una transaccion propia
 * (REQUIRES_NEW) en la que se establece el GUC app.current_empresa_id de
 * PostgreSQL, de modo que el aislamiento por RLS de app_log (FORCE ROW LEVEL
 * SECURITY) acepte la fila con su empresa original.
 */
@Component
public class AppLogPersistenceJob {

    private static final Logger log = LoggerFactory.getLogger(AppLogPersistenceJob.class);
    private static final int TAMANO_LOTE = 200;

    private final AppLogAppender appender;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate txTemplate;

    public AppLogPersistenceJob(AppLogAppender appender,
                                JdbcTemplate jdbcTemplate,
                                PlatformTransactionManager transactionManager) {
        this.appender = appender;
        this.jdbcTemplate = jdbcTemplate;
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelay = 1000)
    public void persistirPendientes() {
        List<AppLogAppender.EntradaLog> lote = new ArrayList<>();
        appender.cola().drainTo(lote, TAMANO_LOTE);
        if (lote.isEmpty()) {
            return;
        }
        try {
            for (AppLogAppender.EntradaLog entrada : lote) {
                persistir(entrada);
            }
        } catch (RuntimeException ex) {
            Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
            log.warn("No se pudo persistir lote en app_log: {} | causa={}", ex.getMessage(), causa, ex);
        }
    }

    private void persistir(AppLogAppender.EntradaLog entrada) {
        txTemplate.executeWithoutResult(status -> {
            ILoggingEvent evento = entrada.evento();
            String mensaje = evento.getFormattedMessage();
            if (mensaje != null && mensaje.length() > 4000) {
                mensaje = mensaje.substring(0, 4000);
            }
            String logger = evento.getLoggerName();
            if (logger != null && logger.length() > 250) {
                logger = logger.substring(0, 250);
            }
            UUID empresaId = entrada.empresaId();
            jdbcTemplate.queryForObject(
                    "SELECT set_config('app.current_empresa_id', ?, true)",
                    String.class,
                    empresaId != null ? empresaId.toString() : "");
            jdbcTemplate.update(
                    "INSERT INTO app_log (empresa_id, nivel, logger, mensaje) VALUES (?, ?, ?, ?)",
                    empresaId,
                    evento.getLevel() != null ? evento.getLevel().toString() : "INFO",
                    logger,
                    mensaje);
        });
    }
}