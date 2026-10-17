package com.activehub.shared.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.shared.email.EmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CorreoNotificacionListenerTest {

    @Mock private EmailSender emailSender;

    @Test
    void enviar_mandaElMensajeDeLaCampanaConElAsuntoDelTipo() {
        var listener = new CorreoNotificacionListener(emailSender, "http://localhost:5173");

        listener.enviar(new CorreoNotificacionEvent(
                "sofia@example.com", "Sofía", TipoNotificacion.CLASE_CANCELADA,
                "Se canceló la clase de \"Yoga\" del 12/03 18:00. Se liberó la reserva del pago en tu tarjeta."));

        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(emailSender).enviar(eq("sofia@example.com"), eq("Se canceló una clase — ActiveHub"), cuerpo.capture());
        // El texto viaja escapado y es el mismo de la campana.
        assertThat(cuerpo.getValue()).contains("Se canceló la clase de &quot;Yoga&quot; del 12/03 18:00.");
        assertThat(cuerpo.getValue()).contains("Hola Sofía,");
        assertThat(cuerpo.getValue()).contains("href=\"http://localhost:5173\"");
    }

    @Test
    void enviar_siElSenderExplota_noPropaga() {
        var listener = new CorreoNotificacionListener(emailSender, "http://localhost:5173");
        when(emailSender.enviar(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("smtp"));

        assertThatCode(() -> listener.enviar(new CorreoNotificacionEvent(
                "sofia@example.com", "Sofía", TipoNotificacion.PENALIZACION_APLICADA, "Recibiste una penalización.")))
                .doesNotThrowAnyException();
    }
}
