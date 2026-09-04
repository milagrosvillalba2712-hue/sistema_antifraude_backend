package com.antifraude.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import com.antifraude.logging.AppLogAppender;

/**
 * Registra el AppLogAppender en el logger raiz de logback al inicio de la
 * aplicacion, para que todos los eventos de logging (INFO/WARN/ERROR) se
 * persistan en la tabla app_log.
 */
@Configuration
public class LogbackAppLogConfig {

    private final AppLogAppender appender;

    public LogbackAppLogConfig(AppLogAppender appender) {
        this.appender = appender;
    }

    @PostConstruct
    public void registerAppender() {
        if (appender == null) {
            return;
        }
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        appender.setName("APP_LOG");
        appender.setContext(context);
        appender.start();
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        if (!root.isAttached(appender)) {
            root.addAppender(appender);
        }
    }
}