package com.activehub.shared.email;

import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Manda un mail HTML por Gmail SMTP.
 *
 * <h2>Sin credenciales NO falla: escribe el mail en el log</h2>
 *
 * Si falta alguna de las dos credenciales SMTP, en vez de intentar conectarse a Gmail loguea el
 * asunto, el destinatario y el cuerpo en WARN. Es deliberado y es lo que hace que el repo
 * siga siendo usable sin secretos: sin este modo, un checkout limpio no podría registrar a
 * nadie (el alta manda el código), y {@code ActivehubApiApplicationTests} —que levanta el
 * contexto completo— dependería de una casilla real.
 *
 * <p><b>En ese modo el código de verificación aparece en el log del backend</b>, que es
 * justamente cómo se prueba el flujo en desarrollo.
 *
 * <h2>Un fallo de SMTP no puede tumbar la operación que lo disparó</h2>
 *
 * {@link #enviar} no propaga: devuelve {@code false}. Gmail puede estar caído, rechazar la
 * contraseña de aplicación o tardar más que el timeout, y ninguna de esas cosas justifica
 * perder un alta ya guardada. Quien llama decide qué hacer; el usuario siempre tiene el botón
 * de reenviar.
 */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromNombre;
    private final String usuarioSmtp;
    private final String passwordSmtp;

    public EmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String from,
            @Value("${app.mail.from-nombre:ActiveHub}") String fromNombre,
            @Value("${spring.mail.username:}") String usuarioSmtp,
            @Value("${spring.mail.password:}") String passwordSmtp
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromNombre = fromNombre;
        this.usuarioSmtp = usuarioSmtp;
        this.passwordSmtp = passwordSmtp;
    }

    /**
     * Si hay credenciales completas. Con {@code false} los mails van al log.
     *
     * <p>Se exigen <b>las dos</b>. Con el usuario cargado y la contraseña vacía, Gmail
     * rechazaría la autenticación y el código se perdería — bastante peor que no intentar y
     * dejarlo en el log, que es el modo de desarrollo.
     */
    public boolean habilitado() {
        return usuarioSmtp != null && !usuarioSmtp.isBlank()
                && passwordSmtp != null && !passwordSmtp.isBlank();
    }

    /** @return true si se entregó al servidor SMTP. Nunca lanza. */
    public boolean enviar(String destinatario, String asunto, String cuerpoHtml) {
        if (!habilitado()) {
            log.warn("""
                    [MAIL NO CONFIGURADO] No se envió nada. Cargá MAIL_USERNAME y MAIL_PASSWORD \
                    (contraseña de aplicación de Google) para mandar mails de verdad.
                      Para : {}
                      Asunto: {}
                    {}""", destinatario, asunto, cuerpoHtml);
            return false;
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(mensaje, false, StandardCharsets.UTF_8.name());
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            // true = el cuerpo es HTML. El mail no lleva alternativa en texto plano a
            // propósito: el código también va en el asunto, así que un cliente que no
            // renderice HTML igual lo muestra en la bandeja.
            helper.setText(cuerpoHtml, true);
            helper.setFrom(remitente(), fromNombre);
            mailSender.send(mensaje);
            return true;
        } catch (UnsupportedEncodingException | org.springframework.mail.MailException | jakarta.mail.MessagingException e) {
            log.error("No se pudo enviar el mail a {} (asunto: {})", destinatario, asunto, e);
            return false;
        }
    }

    /**
     * Gmail <b>reescribe</b> el From a la cuenta autenticada, así que si {@code app.mail.from}
     * quedó vacío se usa directamente el usuario SMTP en vez de mandar un From inválido.
     */
    private String remitente() {
        return from != null && !from.isBlank() ? from : usuarioSmtp;
    }
}
