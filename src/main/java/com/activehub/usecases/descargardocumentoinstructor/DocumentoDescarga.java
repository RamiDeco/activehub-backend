package com.activehub.usecases.descargardocumentoinstructor;

public record DocumentoDescarga(String nombreArchivo, String tipoContenido, byte[] contenido) {
}
