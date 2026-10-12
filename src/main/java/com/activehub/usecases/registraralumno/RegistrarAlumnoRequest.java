package com.activehub.usecases.registraralumno;

import com.activehub.shared.error.SinHtml;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegistrarAlumnoRequest(
        @NotBlank(message = "El nombre es obligatorio") @SinHtml String nombre,
        @NotBlank(message = "El apellido es obligatorio") @SinHtml String apellido,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido") String email,
        @NotBlank(message = "El teléfono es obligatorio") String telefono,
        // Opcional: las pantallas maquetadas usan el correo como credencial y el DNI aparece
        // solo como credencial alternativa de login (nota de la épica E1A). Cuando viene, es
        // clave de unicidad de la cuenta igual que el email.
        @Pattern(regexp = "^$|^[0-9]{7,8}$", message = "El DNI debe tener 7 u 8 dígitos") String dni,
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número"
        ) String password,
        @NotNull(message = "La fecha de nacimiento es obligatoria") LocalDate fechaNacimiento,
        /** Ids de TipoActividad: desde V19 un interés es un tipo del catálogo, no texto libre. */
        List<UUID> intereses,
        @Size(max = 500, message = "Máximo 500 caracteres") @SinHtml String condicionSalud,
        @AssertTrue(message = "Tenés que aceptar los términos y condiciones") boolean aceptaTerminos
) {
}


