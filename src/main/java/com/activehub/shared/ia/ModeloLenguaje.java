package com.activehub.shared.ia;

import java.util.List;

/**
 * El modelo de lenguaje, detrás de una interfaz: el mismo criterio que
 * {@code AlmacenamientoArchivos} y {@code PaymentGateway}. Los casos de uso que lo consumen
 * (el asistente de ayuda y el informe de beneficios y prevenciones) no saben si del otro lado
 * hay Groq, otro proveedor o nada configurado.
 *
 * <p><b>Nadie llama a esto sin preguntar primero por {@link #disponible()}.</b> Sin credenciales
 * la implementación activa es {@link ModeloLenguajeNoConfigurado}, que lanza
 * {@code IaNoDisponibleException} — un 503 que el frontend usa para caer a su comportamiento
 * anterior (FAQs por coincidencia de palabras, plantillas por nivel de intensidad) en vez de
 * mostrar un error.
 */
public interface ModeloLenguaje {

    /** Si hay credenciales cargadas. Con {@code false}, {@link #completar} siempre falla. */
    boolean disponible();

    /**
     * Una sola vuelta de chat. {@code mensajes} viaja tal cual (el primero suele ser el
     * {@code system}), y la respuesta es el texto del único candidato que se pide.
     *
     * @param temperatura 0 para las respuestas que tienen que ceñirse a un texto fuente.
     * @param maxTokens   techo de la respuesta; el proveedor corta ahí.
     * @throws com.activehub.shared.error.IaNoDisponibleException si el proveedor no responde,
     *                                                            rechaza la credencial o
     *                                                            devuelve algo inesperado.
     */
    String completar(List<MensajeIa> mensajes, double temperatura, int maxTokens);

    /** Para el log de arranque: qué quedó activo. */
    String descripcion();
}
