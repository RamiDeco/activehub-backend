package com.activehub.usecases.descargarmidocumento;

public record DescargarMiDocumentoResponse(String nombreArchivo, String tipoContenido, byte[] contenido) {
}
