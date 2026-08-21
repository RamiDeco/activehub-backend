package com.activehub.usecases.notificarausenciaprofesor;

import java.util.UUID;

public record NotificarAusenciaProfesorResponse(UUID claseId, String estado, int alumnosNotificados) {
}
