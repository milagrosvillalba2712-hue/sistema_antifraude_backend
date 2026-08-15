package com.antifraude.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class ApiEventoControllerAspect {

    private final ApiEventoService apiEventoService;

    public ApiEventoControllerAspect(ApiEventoService apiEventoService) {
        this.apiEventoService = apiEventoService;
    }

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object registrarEventoApi(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            ServletRequestAttributes attributes = currentServletAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                HttpServletResponse response = attributes.getResponse();
                long duracionMs = (System.nanoTime() - start) / 1_000_000;
                apiEventoService.registrarEntrada(request, response != null ? response.getStatus() : 200, duracionMs);
            }
        }
    }

    private ServletRequestAttributes currentServletAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servletAttributes ? servletAttributes : null;
    }
}
