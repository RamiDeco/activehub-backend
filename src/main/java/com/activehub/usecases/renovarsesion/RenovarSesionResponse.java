package com.activehub.usecases.renovarsesion;

/** `expiraEnMinutos` es la ventana de inactividad configurada en el backend. */
public record RenovarSesionResponse(String token, long expiraEnMinutos) {
}
