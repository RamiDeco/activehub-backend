package com.activehub.usecases.actualizarpermisosrol;

import java.util.List;
import java.util.UUID;

public record ActualizarPermisosRolResponse(UUID rolId, String nombre, List<String> permisos) {
}
