package com.activehub.usecases.preguntaralasistente;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Una consulta al asistente, con los turnos anteriores de la misma conversación.
 *
 * <p><b>El historial lo manda el cliente porque el chat no tiene estado en el servidor</b>: no hay
 * tabla de conversaciones ni sesión de chat, y no hace falta — la burbuja vive mientras la pestaña
 * está abierta. Lo que sí hace falta es que el historial venga acotado: es texto que entra en el
 * prompt, o sea tokens de una cuota gratuita, y es texto que el cliente controla.
 *
 * <p>Los topes son parte de la defensa, no decoración. Una "pregunta" de 20.000 caracteres es la
 * forma más barata de inyectar instrucciones largas o de agotar la cuota del minuto, así que se
 * rechaza en el borde con un 400 en vez de reenviársela al modelo.
 */
public record PreguntarAlAsistenteRequest(

        @NotBlank(message = "Escribí tu consulta")
        @Size(max = 400, message = "La consulta no puede superar los 400 caracteres")
        @SinHtml
        String pregunta,

        @Size(max = 6, message = "El historial no puede tener más de 6 mensajes")
        List<@Valid Turno> historial
) {

    /**
     * Un turno anterior. {@code deElAsistente} distingue quién lo dijo: se traduce a los roles
     * {@code assistant}/{@code user} del proveedor, y nunca a {@code system} — un turno del
     * historial no puede convertirse en una instrucción del sistema (ver el prompt del service).
     */
    public record Turno(
            boolean deElAsistente,

            @NotBlank(message = "Un mensaje del historial no puede estar vacío")
            @Size(max = 1200, message = "Un mensaje del historial no puede superar los 1200 caracteres")
            String texto
    ) {
    }
}
