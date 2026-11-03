package com.activehub.usecases.desbloquearinstructorpenalizacion;

import java.time.LocalDate;
import java.util.UUID;

/**
 * @param suspendidoHasta si ademas tiene una suspension vigente, hasta cuando: aprobar el pago
 *                        no la acorta, y la pantalla del admin se lo dice.
 */
public record DesbloquearInstructorPenalizacionResponse(UUID penalizacionId, String estadoPago, LocalDate suspendidoHasta) {
}
