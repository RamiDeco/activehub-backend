package com.activehub.usecases.actualizarmicondicionsalud;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.Size;

/**
 * Lo que el alumno escribe en "¿Padecés alguna enfermedad o lesión?". Opcional: vacío o null
 * borra lo que había. Mismas reglas que en el alta ({@code RegistrarAlumnoRequest}).
 */
public record ActualizarMiCondicionSaludRequest(
        @Size(max = 500, message = "Máximo 500 caracteres") @SinHtml String condicionSalud
) {
}
