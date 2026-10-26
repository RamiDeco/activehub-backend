package com.activehub.usecases.rechazarinstructor;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El motivo es obligatorio desde el 2026-10-26: es lo que lee el instructor para saber qué
 * corregir antes de volver a postularse. Sin él, la pantalla mostraba un texto genérico.
 */
public record RechazarInstructorRequest(
        @NotBlank(message = "Escribí el motivo del rechazo: es lo que va a leer el instructor para corregir su solicitud.")
        @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres.")
        @SinHtml
        String motivo
) {
}
