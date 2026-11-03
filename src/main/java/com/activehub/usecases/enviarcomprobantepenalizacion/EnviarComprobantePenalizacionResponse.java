package com.activehub.usecases.enviarcomprobantepenalizacion;

import java.util.UUID;

public record EnviarComprobantePenalizacionResponse(UUID penalizacionId, UUID comprobanteId, String estadoPago) {
}
