package com.activehub.usecases.creartipoactividad;

import java.util.UUID;

public record CrearTipoActividadResponse(UUID id, String nombre, UUID categoriaId) {
}
