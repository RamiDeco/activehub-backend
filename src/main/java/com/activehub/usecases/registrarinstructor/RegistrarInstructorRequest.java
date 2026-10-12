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
        /**
         * Obligatoria, salvo que el alta venga con {@code googleIdToken}: una cuenta de Google
         * no tiene contraseña que el usuario conozca. Por eso la regla "es obligatoria" no
         * puede ser un {@code @NotBlank} y la aplica el Service, que es quien ve los dos
         * campos juntos; acá sólo se valida la forma cuando viene.
         */
        @Pattern(
                regexp = "^$|^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número"
        ) String password,
        LocalDate fechaNacimiento,
        @NotBlank(message = "La especialidad es obligatoria") @SinHtml String especialidad,
        Integer aniosExperiencia,
        @SinHtml String descripcion,
        @AssertTrue(message = "Tenés que aceptar los términos y condiciones") boolean aceptaTerminos,
        /**
         * ID token de Google, cuando el alta arranco con "Continuar con Google".
         *
         * <p>Se vuelve a verificar del lado del servidor aunque ya se haya verificado al
         * precargar el formulario: entre una cosa y la otra no se guarda ningun estado, y dar
         * por bueno un "ya lo validamos" que manda el cliente es confiar en el cliente.
         */
        String googleIdToken
) {
}


