package com.activehub.shared.error;

import java.time.Duration;
import lombok.Getter;

/**
 * El asistente existe y está configurado, pero ahora mismo no puede responder porque se agotó una
 * cuota: la del plan gratuito del proveedor (429 de Groq, por minuto o por día) o la nuestra
 * ({@code LimiteConsultasIa}, que reparte esa misma cuota entre los usuarios).
 *
 * <h2>Por qué es una excepción propia y no {@link IaNoDisponibleException}</h2>
 *
 * Para el usuario son dos situaciones distintas y merecen dos respuestas distintas:
 *
 * <ul>
 *   <li><b>503 {@code IA_NO_DISPONIBLE}</b> = "no hay IA". El frontend responde con las preguntas
 *       frecuentes y no hace falta explicarle nada a nadie: el asistente sigue contestando.</li>
 *   <li><b>429 {@code IA_SIN_CUOTA}</b> = "hay IA, pero hay que esperar". Acá callarse y contestar
 *       una FAQ sería mentirle: su pregunta no se respondió. El mensaje dice <b>cuánto</b> falta,
 *       para que la persona sepa que puede volver, y la deriva a las preguntas frecuentes mientras
 *       tanto. Un cuadro que se queda en blanco es la peor versión de esto.</li>
 * </ul>
 *
 * <p>El mensaje se arma en el backend y no en la pantalla porque el tiempo de espera lo sabe el
 * backend: lo dice el header {@code Retry-After} del proveedor o el reloj de nuestra ventana.
 *
 * <p>{@link #getEspera()} viaja además del texto para quien quiera hacer algo más que mostrarlo.
 */
@Getter
public class IaSinCuotaException extends ApiException {

    /** Por qué se quedó sin cuota. Cambia el texto, no el código HTTP. */
    public enum Motivo {
        /** La cuota del proveedor (plan gratuito) se agotó para todos. */
        CUOTA_DEL_PROVEEDOR,
        /** Este usuario o esta IP consultaron demasiado seguido y les toca esperar. */
        USO_INTENSO
    }

    private final transient Duration espera;
    private final Motivo motivo;

    public IaSinCuotaException(Duration espera, Motivo motivo, String mensaje) {
        super(ApiErrorCode.IA_SIN_CUOTA, mensaje);
        this.espera = espera;
        this.motivo = motivo;
    }

    /**
     * Cuándo la cuota agotada es la del día y no la del minuto. El proveedor no lo dice con un
     * campo, pero sí con el tiempo que pide esperar: un tope por minuto se libera en segundos, uno
     * diario en horas. Se usa sólo para elegir el texto —"en este momento" o "por hoy"—, porque
     * decirle "por hoy" a alguien que puede volver en 40 segundos lo espanta al vacío.
     */
    private boolean esDeHoy() {
        return espera != null && espera.toMinutes() >= 10;
    }

    /** La misma situación, contada para la burbuja de chat. */
    public IaSinCuotaException paraElAsistente() {
        String cuanto = EsperaLegible.enPalabras(espera);
        String mensaje;
        if (motivo == Motivo.USO_INTENSO) {
            mensaje = "Uf, me hiciste varias preguntas seguidas y necesito un descanso. Volvé a "
                    + "escribirme en " + cuanto + ". Mientras tanto podés seguir consultando "
                    + "nuestras preguntas frecuentes. ¡Gracias!";
        } else if (esDeHoy()) {
            mensaje = "Lo sentimos, el chatbot necesita descansar: por hoy ya respondió muchas "
                    + "consultas. Volvé en " + cuanto + ". Mientras tanto podés seguir consultando "
                    + "nuestras preguntas frecuentes. ¡Gracias!";
        } else {
            mensaje = "Lo sentimos, el chatbot está respondiendo muchas consultas en este momento y "
                    + "necesita un descanso. Volvé a escribirme en " + cuanto + ". Mientras tanto "
                    + "podés seguir consultando nuestras preguntas frecuentes. ¡Gracias!";
        }
        return new IaSinCuotaException(espera, motivo, mensaje);
    }

    /** La misma situación, contada para el informe de beneficios y prevenciones. */
    public IaSinCuotaException paraElInforme() {
        String cuanto = EsperaLegible.enPalabras(espera);
        String mensaje;
        if (motivo == Motivo.USO_INTENSO) {
            mensaje = "Generaste varios informes seguidos y el asistente necesita un descanso. "
                    + "Probá de nuevo en " + cuanto + ". Mientras tanto podés ver los beneficios y "
                    + "prevenciones generales de la actividad más abajo. ¡Gracias!";
        } else if (esDeHoy()) {
            mensaje = "Lo sentimos, el asistente necesita descansar: por hoy ya generó muchos "
                    + "informes. Probá de nuevo en " + cuanto + ". Mientras tanto podés ver los "
                    + "beneficios y prevenciones generales de la actividad más abajo. ¡Gracias!";
        } else {
            mensaje = "Lo sentimos, el asistente está generando muchos informes en este momento y "
                    + "necesita un descanso. Probá de nuevo en " + cuanto + ". Mientras tanto podés "
                    + "ver los beneficios y prevenciones generales de la actividad más abajo. "
                    + "¡Gracias!";
        }
        return new IaSinCuotaException(espera, motivo, mensaje);
    }
}
