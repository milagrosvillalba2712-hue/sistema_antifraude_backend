package com.antifraude.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Resolucion de la IP real del cliente considerando proxies: prioriza
 * X-Forwarded-For (primer valor) y X-Real-IP, con getRemoteAddr() como
 * fallback. La IP se usa solo como telecometria de licencia, nunca como
 * identidad principal (ADR-002).
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String primera = forwarded.split(",")[0].trim();
            if (StringUtils.hasText(primera)) {
                return primera;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}