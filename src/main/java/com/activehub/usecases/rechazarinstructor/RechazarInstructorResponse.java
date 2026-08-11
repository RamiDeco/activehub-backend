package com.activehub.usecases.rechazarinstructor;

import java.util.UUID;

public record RechazarInstructorResponse(UUID id, String estadoVerificacion, String motivoRechazo) {
}
