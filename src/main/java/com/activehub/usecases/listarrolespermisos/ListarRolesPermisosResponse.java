package com.activehub.usecases.listarrolespermisos;

import java.util.List;
import java.util.UUID;

/**
 * La matriz completa de E4Ad-PAN-05: los roles (columna izquierda) y el catalogo de
 * permisos (columnas de la derecha). Cada rol trae las claves que tiene habilitadas, asi la
 * pantalla arma los checkboxes sin una segunda consulta por rol.
 */
public record ListarRolesPermisosResponse(List<Rol> roles, List<Permiso> permisos) {

    public record Rol(
            UUID id, String nombre, String descripcion, boolean sistema, long usuarios, List<String> permisos) {
    }

    /**
     * {@code configurable == false} = permiso implicito ({@code Permiso.IMPLICITOS}): lo tienen
     * todos los roles y la pantalla no lo ofrece como checkbox.
     */
    public record Permiso(
            UUID id, String clave, String modulo, String accion, boolean critico, boolean configurable) {
    }
}
