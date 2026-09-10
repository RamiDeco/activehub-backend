package com.activehub.usecases.listarnivelesintensidad;

import java.util.UUID;

/** `actividades` es el conteo que la tabla de E4Ad-PAN-04 muestra en su columna "Actividades". */
public record ListarNivelesIntensidadResponse(UUID id, String nombre, String descripcion, long actividades) {
}
