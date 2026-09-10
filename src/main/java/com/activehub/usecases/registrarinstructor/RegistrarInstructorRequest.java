package com.activehub.usecases.registrarinstructor;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record RegistrarInstructorRequest(
        @NotBlank(message = "El nombre es obligatorio") @SinHtml String nombre,
        @NotBlank(message = "El apellido es obligatorio") @SinHtml String apellido,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido") String email,
        @NotBlank(message = "El teléfono es obligatorio") String telefono,
        // Opcional, igual que en el alta de alumno; ver RegistrarAlumnoRequest.
        @Pattern(regexp = "^$|^[0-9]{7,8}$", message = "El DNI debe tener 7 u 8 dígitos") String dni,
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número"
        ) String password,
        LocalDate fechaNacimiento,
        @NotBlank(message = "La especialidad es obligatoria") @SinHtml String especialidad,
        Integer aniosExperiencia,
        @SinHtml String descripcion,
        @AssertTrue(message = "Tenés que aceptar los términos y condiciones") boolean aceptaTerminos
) {
}


