package com.activehub.usecases.generarinformeactividad;

import java.time.Instant;
import java.util.List;

/**
 * El informe orientativo del "Asistente de beneficios y prevenciones".
 *
 * @param resumen      dos o tres oraciones sobre si la actividad le puede gustar a este alumno y
 *                     por qué, en segunda persona.
 * @param afinidad     {@code Alta}, {@code Media} o {@code Baja}: cuánto pega la actividad con lo
 *                     que el alumno ya eligió antes. Es una etiqueta para la pantalla, no un
 *                     puntaje — y no bloquea nada.
 * @param beneficios   qué puede sacar de esta actividad, atado a su perfil.
 * @param prevenciones qué conviene tener en cuenta. <b>No son indicaciones médicas</b>: son las
 *                     precauciones de sentido común que ya mostraba la pantalla (hidratación,
 *                     calzado, avisarle al instructor), y la única mención a la salud posible es
 *                     recomendar consultar a un profesional.
 * @param generadoEn   cuándo se generó, para el "Generado el …" de la pantalla.
 */
public record GenerarInformeActividadResponse(
        String resumen,
        String afinidad,
        List<String> beneficios,
        List<String> prevenciones,
        Instant generadoEn
) {
}
