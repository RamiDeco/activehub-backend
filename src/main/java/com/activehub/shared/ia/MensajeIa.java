package com.activehub.shared.ia;

/**
 * Un turno de conversación tal como lo espera la API de chat del modelo.
 *
 * <p>El {@code rol} es el de OpenAI/Groq ({@code system}, {@code user}, {@code assistant}) y no
 * una traducción nuestra: es lo único que el proveedor entiende y traducirlo acá solo agregaría
 * un mapeo más que puede quedar mal. Quien arma la conversación usa las fábricas de abajo.
 */
public record MensajeIa(String rol, String contenido) {

    public static MensajeIa sistema(String contenido) {
        return new MensajeIa("system", contenido);
    }

    public static MensajeIa usuario(String contenido) {
        return new MensajeIa("user", contenido);
    }

    public static MensajeIa asistente(String contenido) {
        return new MensajeIa("assistant", contenido);
    }
}
