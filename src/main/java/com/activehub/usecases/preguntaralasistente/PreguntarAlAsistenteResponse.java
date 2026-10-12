package com.activehub.usecases.preguntaralasistente;

import java.util.List;

/**
 * La respuesta del asistente.
 *
 * @param respuesta    el texto que se muestra en la burbuja.
 * @param secciones    los títulos de las secciones del manual con las que se armó la respuesta. Se
 *                     devuelven para que la burbuja pueda mostrar de dónde salió lo que dice: una
 *                     respuesta que no se puede rastrear al manual se lee como inventada. Va vacía
 *                     cuando el asistente contesta que no tiene esa información.
 * @param sinInformacion true si el asistente respondió que la consulta no está cubierta por el
 *                     manual. El frontend lo usa para ofrecer la pantalla de Ayuda en vez de
 *                     dejar la conversación en una negativa seca.
 */
public record PreguntarAlAsistenteResponse(
        String respuesta,
        List<String> secciones,
        boolean sinInformacion
) {
}
