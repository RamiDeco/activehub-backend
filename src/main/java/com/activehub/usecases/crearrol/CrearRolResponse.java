package com.activehub.usecases.crearrol;

import java.util.UUID;

public record CrearRolResponse(UUID id, String nombre, String descripcion, boolean sistema, long usuarios) {
}
