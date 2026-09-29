package com.activehub.shared.storage;

/**
 * Falló guardar, leer o borrar un archivo. No extiende {@code ApiException} a propósito: el
 * mensaje que ve el usuario depende de qué estaba subiendo ("No pudimos guardar la foto",
 * "No pudimos guardar el archivo"), así que lo elige el caso de uso y no la capa de
 * almacenamiento, que no sabe en qué pantalla está parado nadie.
 */
public class AlmacenamientoException extends RuntimeException {

    public AlmacenamientoException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlmacenamientoException(String message) {
        super(message);
    }
}
