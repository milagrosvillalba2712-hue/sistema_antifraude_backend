package com.antifraude.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Servicio de envio de emails transaccionales (verificacion, recuperacion,
 * invitacion, bienvenida). Todos usan la plantilla general con branding Regula
 * ({@link EmailTemplate}). Si SMTP no esta configurado, imprime en consola (demo).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String linkBaseUrl;
    private final String fromAddress;
    private final String fromName;
    private final boolean smtpEnabled;
    private final ClassPathResource logoResource = new ClassPathResource("mail/regula-horizontal.png");

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.auth.link-base-url:http://localhost:5173}") String linkBaseUrl,
            @Value("${app.mail.from-address:noreply@regula.com.pa}") String fromAddress,
            @Value("${app.mail.from-name:Regula AML}") String fromName,
            @Value("${app.mail.enabled:false}") boolean smtpEnabled) {
        this.mailSender = mailSender;
        this.linkBaseUrl = linkBaseUrl;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.smtpEnabled = smtpEnabled;
    }

    public void enviarVerificacion(String email, String codigo) {
        String url = linkBaseUrl + "/verify-email?codigo=" + codigo;
        String cuerpo = EmailTemplate.parrafo("Has sido registrado en " + EmailTemplate.strong("Regula") + ".")
                + EmailTemplate.parrafo("Haz clic en el siguiente botón para verificar tu correo electrónico:");
        String html = EmailTemplate.render(new EmailTemplate.Contenido(
                "Verifica tu correo electrónico", null, cuerpo,
                "Verificar correo", url,
                "Si no solicitaste este registro, ignora este mensaje. El equipo de soporte nunca te solicitará tu contraseña."),
                linkBaseUrl);
        sendEmail(email, "Regula AML - Verifica tu correo electronico", html);
    }

    public void enviarRecuperacion(String email, String codigo) {
        String url = linkBaseUrl + "/reset-password?codigo=" + codigo;
        String cuerpo = EmailTemplate.parrafo("Recibimos una solicitud para restablecer tu contraseña en "
                + EmailTemplate.strong("Regula") + ".")
                + EmailTemplate.parrafo("Haz clic en el siguiente botón para continuar:");
        String html = EmailTemplate.render(new EmailTemplate.Contenido(
                "Restablece tu contraseña", "Este enlace es válido por 30 minutos", cuerpo,
                "Restablecer contraseña", url,
                "Si no solicitaste este cambio, ignora este mensaje. El equipo de soporte nunca te solicitará tu contraseña."),
                linkBaseUrl);
        sendEmail(email, "Regula AML - Restablece tu contrasena", html);
    }

    /**
     * Envia enlace de invitacion para crear usuario.
     * @param email correo del invitado
     * @param codigo codigo de invitacion
     * @param rol nombre del rol asignado
     * @param empresa nombre de la empresa
     */
    public void enviarInvitacion(String email, String codigo, String rol, String empresa) {
        String url = linkBaseUrl + "/invitacion?codigo=" + codigo;
        String cuerpo = EmailTemplate.parrafo(EmailTemplate.strong(empresa)
                        + " te ha invitado a unirte a la plataforma antifraude con el rol de "
                        + EmailTemplate.strong(rol) + ".")
                + EmailTemplate.parrafo("Haz clic en el siguiente botón para completar tu registro de forma segura y acceder al entorno de control.");
        String html = EmailTemplate.render(new EmailTemplate.Contenido(
                "Invitación a Regula", "Este enlace es válido por 7 días", cuerpo,
                "Completar registro", url,
                "Por razones de seguridad, nunca compartas este enlace con terceros. El equipo de soporte técnico nunca te solicitará tu contraseña."),
                linkBaseUrl);
        sendEmail(email, "Regula AML - Fuiste invitado a unirte como " + rol, html);
    }

    /**
     * Envia correo de bienvenida despues de completar el registro.
     */
    public void enviarBienvenida(String email, String nombre, String rol, String empresa) {
        String cuerpo = EmailTemplate.parrafo("Hola " + EmailTemplate.strong(nombre) + ",")
                + EmailTemplate.parrafo("Tu cuenta ha sido creada exitosamente en "
                        + EmailTemplate.strong("Regula") + ".")
                + EmailTemplate.cajaDetalles(
                        EmailTemplate.filaDetalle("Empresa", empresa)
                        + EmailTemplate.filaDetalle("Rol", rol));
        String html = EmailTemplate.render(new EmailTemplate.Contenido(
                "Bienvenido a Regula", null, cuerpo,
                "Iniciar sesión", linkBaseUrl + "/login",
                "Si tienes alguna consulta, contacta al administrador de tu empresa."),
                linkBaseUrl);
        sendEmail(email, "Regula AML - Bienvenido al sistema", html);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        if (!smtpEnabled) {
            log.warn("[MAIL-MOCK] Para {} | Asunto: {} | URL base: {}", to, subject, linkBaseUrl);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (logoResource.exists()) {
                helper.addInline("regulaLogo", logoResource);
            } else {
                log.warn("[MAIL] Logo no encontrado en classpath (mail/regula-horizontal.png); el correo saldra sin logo");
            }
            mailSender.send(message);
            log.info("[MAIL] Enviado a {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("[MAIL] Error al enviar a {}: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("[MAIL] Error inesperado al enviar a {}: {}", to, e.getMessage());
        }
    }
}
