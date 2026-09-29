package com.activehub.usecases.inscribirse;

import com.activehub.shared.payments.MercadoPagoOrderRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InscribirseRequest(
        @NotBlank(message = "El método de pago es obligatorio")
        @Pattern(regexp = "Mercado Pago|Efectivo", message = "Método de pago inválido")
        String metodoPago,
        @jakarta.validation.Valid MercadoPagoOrderRequest datosPago
) {
    public InscribirseRequest(String metodoPago) {
        this(metodoPago, null);
    }
}
