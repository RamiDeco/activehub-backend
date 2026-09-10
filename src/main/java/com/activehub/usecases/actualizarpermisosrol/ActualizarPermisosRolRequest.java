package com.activehub.usecases.actualizarpermisosrol;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Las claves que quedan habilitadas; todo lo que no venga en la lista queda apagado. */
public record ActualizarPermisosRolRequest(
        @NotNull(message = "Enviá la lista de permisos habilitados") List<String> permisos
) {
}
