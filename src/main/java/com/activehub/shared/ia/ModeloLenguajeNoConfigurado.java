package com.activehub.shared.ia;

import com.activehub.shared.error.IaNoDisponibleException;
import java.util.List;

/**
 * Lo que queda activo sin {@code GROQ_API_KEY}: la aplicación arranca igual y todo lo que no es
 * IA funciona, pero cada pedido al modelo responde 503 con un mensaje para el usuario.
 *
 * <p>Es el mismo patrón que {@code EmailSender} en modo log: un checkout sin credenciales tiene
 * que poder levantar y usarse. La alternativa —fallar al arrancar— dejaría el proyecto entero
 * atado a una clave de un servicio externo.
 */
public class ModeloLenguajeNoConfigurado implements ModeloLenguaje {

    @Override
    public boolean disponible() {
        return false;
    }

    @Override
    public String completar(List<MensajeIa> mensajes, double temperatura, int maxTokens) {
        throw new IaNoDisponibleException();
    }

    @Override
    public String descripcion() {
        return "sin configurar (falta GROQ_API_KEY)";
    }
}
