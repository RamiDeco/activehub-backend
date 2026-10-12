package com.activehub.usecases.inscribirse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lo que devuelve el Card Payment Brick al tokenizar la tarjeta. El monto, la referencia y el
 * pagador NO vienen del cliente: los pone el servidor (precio congelado de la clase, id de la
 * inscripcion, correo del alumno logueado).
 */
public record DatosPagoRequest(
        @NotBlank(message = "Falta el token de la tarjeta")
        @Size(max = 100)
        String token,
        @NotBlank(message = "Falta el medio de pago")
        @Size(max = 50)
        String paymentMethodId,
        @Size(max = 30)
        String paymentMethodType,
        @Min(value = 1, message = "Las cuotas tienen que ser al menos 1")
        @Max(value = 24, message = "Cantidad de cuotas inválida")
        int cuotas
) {
}
