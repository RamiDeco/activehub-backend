package com.activehub.usecases.listarmisnotificaciones;

import java.time.Instant;
import java.util.UUID;

/**
 * @param entidadId   el registro que originó el aviso. Su significado depende del tipo; sirve
 *                    para trazar, no para navegar.
 * @param destinoTipo a qué pantalla lleva el click ({@code NINGUNO} = no es clickeable).
 * @param destinoId   el parámetro de ruta de esa pantalla, ya resuelto por el backend.
 */
public record ListarMisNotificacionesResponse(
        UUID id,
        String tipo,
        String mensaje,
        UUID entidadId,
        String destinoTipo,
        UUID destinoId,
        boolean leida,
        Instant createdAt
) {
}
