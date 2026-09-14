package com.activehub.usecases.subirdocumento;

import java.util.UUID;

public record SubirDocumentoResponse(UUID id, String nombreArchivo, String tipoDocumento, long tamanioBytes) {
}
