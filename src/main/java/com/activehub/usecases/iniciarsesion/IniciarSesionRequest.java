package com.activehub.usecases.iniciarsesion;

import jakarta.validation.constraints.NotBlank;

/**
 * El campo se llama {@code identificador} y no {@code email} porque acepta las dos credenciales:
 * la nota de la epica E1A dice que "el modulo de Autenticacion admite ademas el ingreso por DNI".
 *
 * <p>Tampoco lleva {@code @Email}: con esa anotacion un DNI valido era rechazado por formato
 * antes de llegar al service, asi que el login por DNI no podia funcionar aunque el resto
 * estuviera. El formato se decide en {@link IniciarSesionService} segun lo que llegue.
 */
public record IniciarSesionRequest(
        @NotBlank(message = "Ingresá tu correo electrónico o DNI") String identificador,
        @NotBlank(message = "La contraseña es obligatoria") String password
) {
}
