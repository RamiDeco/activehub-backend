package com.activehub.usecases.asignarrolusuario;

import java.util.UUID;

public record AsignarRolUsuarioResponse(UUID usuarioId, UUID rolId, String rol) {
}
