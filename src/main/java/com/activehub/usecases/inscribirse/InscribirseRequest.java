package com.activehub.usecases.inscribirse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InscribirseRequest(
        @NotBlank(message = "El método de pago es obligatorio")
        @Pattern(regexp = "Mercado Pago|Efectivo", message = "Método de pago inválido")
        String metodoPago,
        @Valid DatosPagoRequest datosPago
) {
    public InscribirseRequest(String metodoPago) {
        this(metodoPago, null);
    }
}
