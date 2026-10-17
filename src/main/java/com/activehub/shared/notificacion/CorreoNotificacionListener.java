package com.activehub.shared.notificacion;

import com.activehub.shared.email.EmailSender;
import com.activehub.shared.email.PlantillaEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Manda por correo las notificaciones importantes (las de {@link TipoNotificacion#porCorreo()}).
 *
 * <ul>
 *   <li><b>Después del commit</b> ({@code AFTER_COMMIT}): si la operación que notificó hace
 *       rollback, no sale ningún mail. {@code fallbackExecution = true} cubre a quien notifica
 *       sin transacción (los schedulers que commitean pago por pago).</li>
 *   <li><b>Asíncrono</b>: Gmail puede tardar segundos, y cancelar una clase con veinte
 *       inscriptos no puede esperar veinte envíos.</li>
 *   <li><b>Nunca propaga</b>: el aviso en la campana ya quedó guardado; un SMTP caído no
 *       tiene que deshacer nada ni ensuciar el log de quien notificó.</li>
 * </ul>
 */
@Component
public class CorreoNotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(CorreoNotificacionListener.class);

    private final EmailSender emailSender;
    private final String frontendUrl;

    public CorreoNotificacionListener(EmailSender emailSender, @Value("${app.frontend-url}") String frontendUrl) {
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void enviar(CorreoNotificacionEvent evento) {
        try {
            emailSender.enviar(
                    evento.email(),
                    PlantillaEmail.asuntoNotificacion(evento.tipo()),
                    PlantillaEmail.cuerpoNotificacion(evento.tipo(), evento.nombre(), evento.mensaje(), frontendUrl));
        } catch (RuntimeException e) {
            log.error("No se pudo mandar por correo la notificación {} a {}", evento.tipo(), evento.email(), e);
        }
    }
}
