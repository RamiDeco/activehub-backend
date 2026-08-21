package com.activehub.usecases.notificarausenciaprofesor;

import jakarta.validation.constraints.NotBlank;

public record NotificarAusenciaProfesorRequest(@NotBlank String mensaje) {
}
