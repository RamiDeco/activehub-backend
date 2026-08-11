package com.activehub.usecases.actualizartipoactividad;

import java.util.UUID;

public record ActualizarTipoActividadResponse(UUID id, String nombre, UUID categoriaId) {
}
