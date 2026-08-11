package com.activehub.usecases.listartiposactividad;

import java.util.UUID;

public record ListarTiposActividadResponse(UUID id, String nombre, UUID categoriaId) {
}
