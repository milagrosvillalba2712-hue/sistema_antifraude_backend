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
                return normalizar(primera);
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return normalizar(realIp.trim());
        }
        return normalizar(request.getRemoteAddr());
    }

    /**
     * Normaliza la representacion del loopback IPv6 (::1 / 0:0:0:0:0:0:0:1) a su
     * forma IPv4 (127.0.0.1) para mantener logs y auditoria consistentes cuando
     * la conexion local llega via IPv6.
     */
    static String normalizar(String ip) {
        if (ip == null) {
            return null;
        }
        String limpia = ip.trim();
        if ("::1".equals(limpia) || "0:0:0:0:0:0:0:1".equals(limpia)) {
            return "127.0.0.1";
        }
        return limpia;
    }
}