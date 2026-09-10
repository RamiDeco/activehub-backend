package com.activehub.usecases.agregarimagenactividad;

import java.util.UUID;

public record AgregarImagenActividadResponse(UUID id, UUID actividadId, int orden) {
}
