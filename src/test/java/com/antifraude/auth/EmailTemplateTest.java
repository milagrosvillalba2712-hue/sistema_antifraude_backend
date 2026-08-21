package com.antifraude.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que la plantilla general de correo aplique el branding Regula
 * (paleta, tipografia, footer) y los elementos opcionales segun el tipo.
 */
class EmailTemplateTest {

    private static final String BASE = "http://localhost:5173";

    @Test
    void invitacionUsaBrandingRegula() {
        String cuerpo = EmailTemplate.parrafo(EmailTemplate.strong("Empresa academica Regula")
                + " te ha invitado con el rol de " + EmailTemplate.strong("Analista") + ".");
        String html = EmailTemplate.render(new EmailTemplate.Contenido(
                "Invitación a Regula", "Este enlace es válido por 7 días", cuerpo,
                "Completar registro", BASE + "/invitacion?codigo=abc123",
                "Por razones de seguridad, nunca compartas este enlace con terceros."), BASE);

        // Estructura general
        assertTrue(html.contains("lang=\"es\""));
        assertTrue(html.contains("charset=\"UTF-8\""));
        assertTrue(html.contains("cid:regulaLogo"));
        // Logo con dimensiones explicitas (clientes que ignoran height no deben explotar el layout)
        assertTrue(html.contains("width=\"158\" height=\"40\""));
        // Paleta
        assertTrue(html.contains("#F7F9FC")); // background
        assertTrue(html.contains("#DE7426")); // boton principal
        assertTrue(html.contains("#4E616E")); // slate institucional
        assertTrue(html.contains("#191C1E")); // titulares
        assertTrue(html.contains("#FFF3EB")); // badge
        // Tipografia
        assertTrue(html.contains("Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"));
        // Contenido
        assertTrue(html.contains("Invitación a Regula"));
        assertTrue(html.contains("Este enlace es válido por 7 días"));
        assertTrue(html.contains(BASE + "/invitacion?codigo=abc123"));
        assertTrue(html.contains("Completar registro"));
        // Footer
        assertTrue(html.contains(BASE + "/documentos-legales"));
        assertTrue(html.contains(BASE + "/documentos-legales/privacidad"));
        assertTrue(html.contains("plataformaregula@gmail.com"));
        assertTrue(html.contains("Regula Sistema Antifraude"));
    }

    @Test
    void elementosOpcionalesSeOmiten() {
        String html = EmailTemplate.render(new EmailTemplate.Contenido(
                "Verifica tu correo electrónico", null,
                EmailTemplate.parrafo("Haz clic en el botón."),
                "Verificar correo", BASE + "/verify-email?codigo=x", null), BASE);

        assertFalse(html.contains("#FFF3EB")); // sin badge
        assertFalse(html.contains("nunca compartas")); // sin nota de seguridad
        assertTrue(html.contains("Verificar correo")); // CTA presente
    }

    @Test
    void escapaDatosDeUsuario() {
        String html = EmailTemplate.render(new EmailTemplate.Contenido(
                "<script>alert(1)</script>", null,
                EmailTemplate.parrafo(EmailTemplate.strong("<b>Empresa</b>")),
                null, null, null), BASE);

        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("&lt;b&gt;Empresa&lt;/b&gt;"));
    }

    @Test
    void cajaDetallesUsaLabelCaps() {
        String cuerpo = EmailTemplate.cajaDetalles(
                EmailTemplate.filaDetalle("Empresa", "Financiera Santa Clara")
                + EmailTemplate.filaDetalle("Rol", "Auditor"));
        String html = EmailTemplate.render(new EmailTemplate.Contenido(
                "Bienvenido a Regula", null, cuerpo,
                "Iniciar sesión", BASE + "/login", null), BASE);

        assertTrue(html.contains("Financiera Santa Clara"));
        assertTrue(html.contains("Auditor"));
        assertTrue(html.contains("letter-spacing:0.05em")); // label caps
        assertTrue(html.contains(BASE + "/login"));
    }
}
