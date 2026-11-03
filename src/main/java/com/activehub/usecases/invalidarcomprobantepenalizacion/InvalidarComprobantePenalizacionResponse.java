package com.activehub.usecases.invalidarcomprobantepenalizacion;

import java.util.UUID;

public record InvalidarComprobantePenalizacionResponse(UUID penalizacionId, String estadoPago) {
}
