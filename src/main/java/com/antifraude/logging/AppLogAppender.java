package com.antifraude.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.antifraude.security.tenant.TenantContext;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Appender de logging que encola los eventos (INFO/DEBUG/WARN/ERROR) junto con
 * la empresa activa del momento, para que un job programado los persista en
 * app_log fuera de la transaccion del request. De este modo, un fallo de
 * persistencia de logs nunca afecta a la operacion de negocio ni participa en
 * la transaccion del request.
 */
@Component
public class AppLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    public record EntradaLog(ILoggingEvent evento, UUID empresaId) {
    }

    public static final int CAPACIDAD = 5000;

    private final BlockingQueue<EntradaLog> cola = new LinkedBlockingQueue<>(CAPACIDAD);

    public AppLogAppender() {
    }

    @Override
    protected void append(ILoggingEvent event) {
        EntradaLog entrada = new EntradaLog(event, TenantContext.getEmpresaId());
        if (!cola.offer(entrada)) {
            cola.poll();
            cola.offer(entrada);
        }
    }

    public BlockingQueue<EntradaLog> cola() {
        return cola;
    }

    public int pendientes() {
        return cola.size();
    }
}