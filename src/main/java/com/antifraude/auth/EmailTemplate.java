package com.antifraude.auth;

import java.time.Year;

/**
 * Plantilla HTML general para los correos transaccionales de Regula.
 *
 * El diseno replica el branding de ejemplo_html/template_correo_invitacion.html
 * convertido a CSS inline + tablas (compatible con clientes de correo, que
 * descartan <script> y CSS externo).
 */
final class EmailTemplate {

    // Paleta Regula (guia de estilo)
    static final String PRIMARY_CONTAINER = "#DE7426";
    static final String SECONDARY_SLATE = "#4E616E";
    static final String BACKGROUND = "#F7F9FC";
    static final String SURFACE = "#FFFFFF";
    static final String ON_SURFACE = "#191C1E";
    static final String ON_SURFACE_VARIANT = "#564338";
    static final String BORDER = "#E6E8EB";
    static final String BADGE_BG = "#FFF3EB";
    static final String BADGE_BORDER = "#FDE5D6";
    static final String OUTLINE_VARIANT = "#DDC1B2";
    static final String OUTLINE = "#897266";
    static final String FONT_STACK = "Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif";

    static final String SUPPORT_EMAIL = "plataformaregula@gmail.com";

    /** Contenido variable de un correo. badge, ctaTexto/ctaUrl y notaSeguridad son opcionales (null). */
    record Contenido(String titulo, String badge, String cuerpoHtml,
                     String ctaTexto, String ctaUrl, String notaSeguridad) {
    }

    private EmailTemplate() {
    }

    static String render(Contenido c, String linkBaseUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <!DOCTYPE html>
                <html lang="es">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&amp;display=swap" rel="stylesheet">
                <title>""").append(esc(c.titulo())).append("</title></head>\n");
        sb.append("<body style=\"margin:0;padding:0;background-color:").append(BACKGROUND)
                .append(";font-family:").append(FONT_STACK).append(";\">\n");
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:")
                .append(BACKGROUND).append(";\">\n<tr><td align=\"center\" style=\"padding:48px 16px;\">\n");
        // Tarjeta principal
        sb.append("<table role=\"presentation\" width=\"640\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:640px;background-color:")
                .append(SURFACE).append(";border:1px solid ").append(BORDER).append(";border-radius:16px;\">\n");
        // Encabezado: logo + etiqueta institucional
        sb.append("<tr><td align=\"center\" style=\"padding:48px 48px 8px 48px;\">\n")
                .append("<img src=\"cid:regulaLogo\" alt=\"Regula\" width=\"158\" height=\"40\" style=\"display:block;width:158px;height:40px;\">\n")
                .append("<div style=\"margin-top:12px;font-size:12px;line-height:16px;font-weight:700;letter-spacing:0.05em;text-transform:uppercase;color:")
                .append(SECONDARY_SLATE).append(";\">Sistema Antifraude</div>\n</td></tr>\n");
        // Cuerpo
        sb.append("<tr><td align=\"center\" style=\"padding:24px 48px 48px 48px;\">\n");
        sb.append("<h1 style=\"margin:0 0 16px 0;font-size:28px;line-height:36px;font-weight:700;color:")
                .append(ON_SURFACE).append(";\">").append(esc(c.titulo())).append("</h1>\n");
        if (c.badge() != null) {
            sb.append("<div style=\"margin:8px 0 24px 0;\"><span style=\"display:inline-block;background-color:")
                    .append(BADGE_BG).append(";border:1px solid ").append(BADGE_BORDER).append(";color:")
                    .append(PRIMARY_CONTAINER)
                    .append(";font-size:14px;line-height:20px;font-weight:500;padding:8px 16px;border-radius:9999px;\">")
                    .append(esc(c.badge())).append("</span></div>\n");
        }
        sb.append(c.cuerpoHtml()).append("\n");
        if (c.ctaTexto() != null && c.ctaUrl() != null) {
            sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:32px auto 16px auto;\">\n")
                    .append("<tr><td align=\"center\" bgcolor=\"").append(PRIMARY_CONTAINER)
                    .append("\" style=\"border-radius:12px;\">\n")
                    .append("<a href=\"").append(esc(c.ctaUrl()))
                    .append("\" style=\"display:inline-block;padding:16px 40px;font-size:16px;line-height:24px;font-weight:600;color:#FFFFFF;text-decoration:none;\">")
                    .append(esc(c.ctaTexto())).append("</a>\n</td></tr>\n</table>\n");
        }
        if (c.notaSeguridad() != null) {
            sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:32px;\">\n")
                    .append("<tr><td style=\"background-color:").append(BACKGROUND).append(";border:1px solid ")
                    .append(BORDER).append(";border-radius:12px;padding:24px;font-size:14px;line-height:20px;color:")
                    .append(ON_SURFACE_VARIANT).append(";text-align:left;\">")
                    .append(esc(c.notaSeguridad())).append("</td></tr>\n</table>\n");
        }
        sb.append("</td></tr>\n");
        // Divisor
        sb.append("<tr><td style=\"padding:0 48px;\"><div style=\"height:1px;background-color:")
                .append(BORDER).append(";font-size:0;line-height:0;\">&nbsp;</div></td></tr>\n");
        // Footer
        sb.append("<tr><td align=\"center\" style=\"padding:32px 48px;\">\n")
                .append("<div style=\"margin-bottom:24px;\">\n")
                .append("<a href=\"").append(linkBaseUrl)
                .append("/documentos-legales\" style=\"font-size:13px;font-weight:500;color:")
                .append(ON_SURFACE_VARIANT).append(";text-decoration:none;\">Términos y Condiciones</a>\n")
                .append("<span style=\"color:").append(OUTLINE_VARIANT).append(";\">&nbsp;&nbsp;&#8226;&nbsp;&nbsp;</span>\n")
                .append("<a href=\"").append(linkBaseUrl)
                .append("/documentos-legales/privacidad\" style=\"font-size:13px;font-weight:500;color:")
                .append(ON_SURFACE_VARIANT).append(";text-decoration:none;\">Política de Privacidad</a>\n</div>\n")
                .append("<p style=\"margin:0 0 12px 0;font-size:13px;line-height:20px;color:").append(OUTLINE)
                .append(";\">&#169; ").append(Year.now().getValue())
                .append(" Regula Sistema Antifraude. Todos los derechos reservados.</p>\n")
                .append("<p style=\"margin:0;font-size:13px;line-height:20px;color:").append(OUTLINE)
                .append(";\">Este mensaje fue enviado de manera automatizada. Si lo recibiste por error o necesitas asistencia, contacta a ")
                .append("<a href=\"mailto:").append(SUPPORT_EMAIL).append("\" style=\"color:")
                .append(PRIMARY_CONTAINER).append(";\">").append(SUPPORT_EMAIL).append("</a></p>\n")
                .append("</td></tr>\n</table>\n</td></tr>\n</table>\n</body>\n</html>");
        return sb.toString();
    }

    // ---- helpers de contenido ----

    /** Parrafo de cuerpo centrado (16px/24px, on-surface-variant). */
    static String parrafo(String innerHtml) {
        return "<p style=\"margin:0 0 16px 0;font-size:16px;line-height:24px;font-weight:400;color:"
                + ON_SURFACE_VARIANT + ";text-align:center;\">" + innerHtml + "</p>";
    }

    /** Texto enfatizado dentro de un parrafo. */
    static String strong(String texto) {
        return "<strong style=\"color:" + ON_SURFACE + ";font-weight:600;\">" + esc(texto) + "</strong>";
    }

    /** Caja de detalles (etiqueta/valor) sobre fondo surface. */
    static String cajaDetalles(String filasHtml) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:24px 0 8px 0;\">"
                + "<tr><td style=\"background-color:" + BACKGROUND + ";border:1px solid " + BORDER
                + ";border-radius:12px;padding:24px;text-align:left;\">" + filasHtml + "</td></tr></table>";
    }

    /** Fila etiqueta-caps + valor para cajaDetalles. */
    static String filaDetalle(String etiqueta, String valor) {
        return "<div style=\"margin-bottom:12px;\">"
                + "<div style=\"font-size:12px;line-height:16px;font-weight:700;letter-spacing:0.05em;text-transform:uppercase;color:"
                + SECONDARY_SLATE + ";\">" + esc(etiqueta) + "</div>"
                + "<div style=\"font-size:14px;line-height:20px;font-weight:500;color:" + ON_SURFACE + ";\">"
                + esc(valor) + "</div></div>";
    }

    /** Escape minimo de HTML para datos de usuario interpolados. */
    static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
