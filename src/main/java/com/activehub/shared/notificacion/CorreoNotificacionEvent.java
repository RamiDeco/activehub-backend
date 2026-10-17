package com.activehub.shared.notificacion;

/**
 * Una notificación importante que también tiene que llegar por correo. Lleva los datos ya
 * resueltos (correo y nombre del destinatario) porque quien la consume corre en otro hilo,
 * después del commit, sin la sesión de JPA de quien la publicó.
 */
public record CorreoNotificacionEvent(String email, String nombre, TipoNotificacion tipo, String mensaje) {
}
