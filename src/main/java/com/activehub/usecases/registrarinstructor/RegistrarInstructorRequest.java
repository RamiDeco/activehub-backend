package com.activehub.usecases.registrarinstructor;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record RegistrarInstructorRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El apellido es obligatorio") String apellido,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido") String email,
        @NotBlank(message = "El teléfono es obligatorio") String telefono,
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, una letra y un número"
        ) String password,
        LocalDate fechaNacimiento,
        @NotBlank(message = "La especialidad es obligatoria") String especialidad,
        Integer aniosExperiencia,
        String descripcion,
        @AssertTrue(message = "Tenés que aceptar los términos y condiciones") boolean aceptaTerminos
) {
}
