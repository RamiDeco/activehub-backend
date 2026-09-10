package com.activehub.usecases.responderresenia;

import java.time.Instant;
import java.util.UUID;

public record ResponderReseniaResponse(UUID id, String respuestaInstructor, Instant respuestaInstructorAt) {
}
