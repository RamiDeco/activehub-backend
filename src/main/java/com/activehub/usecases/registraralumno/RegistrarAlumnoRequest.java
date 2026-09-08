package com.activehub.usecases.registraralumno;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

public record RegistrarAlumnoRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El apellido es obligatorio") String apellido,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido") String email,
        @NotBlank(message = "El teléfono es obligatorio") String telefono,
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número"
        ) String password,
        @NotNull(message = "La fecha de nacimiento es obligatoria") LocalDate fechaNacimiento,
        List<String> intereses,
        @AssertTrue(message = "Tenés que aceptar los términos y condiciones") boolean aceptaTerminos
) {
}


