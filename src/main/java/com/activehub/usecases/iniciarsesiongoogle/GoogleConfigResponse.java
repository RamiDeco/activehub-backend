package com.activehub.usecases.iniciarsesiongoogle;

/** @param clientId vacío cuando no está configurado. */
public record GoogleConfigResponse(boolean habilitado, String clientId) {
}
