package com.activehub.shared.error;

/**
 * El modelo de lenguaje no está configurado o no respondió.
 *
 * <p>Es un 503 y no un 500 a propósito: no hay nada roto en ActiveHub, hay un servicio externo
 * que ahora no está. El frontend lo trata como "sin IA por ahora" y cae a lo que hacía antes
 * (las FAQs por coincidencia de palabras en el chat, las plantillas por nivel de intensidad en el
 * informe de beneficios), así que ninguna pantalla queda sin respuesta.
 */
public class IaNoDisponibleException extends ApiException {

    public IaNoDisponibleException() {
        super(
                ApiErrorCode.IA_NO_DISPONIBLE,
                "El asistente no está disponible en este momento. Intentá de nuevo en un rato.");
    }
}
