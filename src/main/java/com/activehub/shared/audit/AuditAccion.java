package com.activehub.shared.audit;

public enum AuditAccion {
    REGISTRO_ALUMNO,
    REGISTRO_INSTRUCTOR,
    /** Alta creada con "Continuar con Google": nace con el correo ya verificado. */
    REGISTRO_GOOGLE,
    /** Ingresó el código de 6 dígitos del alta: a partir de acá el correo le queda reservado. */
    EMAIL_VERIFICADO,
    /** Confirmó un correo NUEVO desde su perfil. La metadata es la dirección nueva. */
    EMAIL_CAMBIADO,
    LOGIN_OK,
    LOGIN_FALLIDO,
    LOGIN_BLOQUEADO,
    /**
     * Alguien pidio algo para lo que no tiene permiso y el backend lo rechazo con 403.
     * Es una operacion que NO ocurrio, y justamente por eso interesa: un alumno probando
     * /api/admin/roles es la señal que una auditoria quiere ver. Lo registra
     * {@code GlobalExceptionHandler}, que es por donde pasan las denegaciones de
     * {@code @PreAuthorize}.
     */
    ACCESO_DENEGADO,
    ADMIN_CREADO,
    ACTIVIDAD_CREADA,
    ACTIVIDAD_ACTUALIZADA,
    ACTIVIDAD_ELIMINADA,
    CLASE_CREADA,
    CLASE_ACTUALIZADA,
    CLASE_ELIMINADA,
    TIPO_ACTIVIDAD_CREADO,
    TIPO_ACTIVIDAD_ACTUALIZADO,
    TIPO_ACTIVIDAD_ELIMINADO,
    INSTRUCTOR_VALIDADO,
    INSTRUCTOR_RECHAZADO,
    PREINSCRIPCION_CREADA,
    INSCRIPCION_CREADA,
    INSCRIPCION_ACTUALIZADA,
    INSCRIPCION_CANCELADA,
    COBRO_EFECTIVO_CONFIRMADO,
    PAGO_LIBERADO,
    CLASE_CANCELADA,
    RESENIA_CREADA,
    RESENIA_ELIMINADA,
    RESENIA_APROBADA,
    RESENIA_RECHAZADA,
    CLASE_HABILITADA,
    CLASE_FINALIZADA,
    DENUNCIA_CREADA,
    DENUNCIA_EN_AUDITORIA,
    DENUNCIA_RESUELTA,
    PENALIZACION_APLICADA,
    SUSPENSION_LEVANTADA,
    USUARIO_ESTADO_ACTUALIZADO,
    USUARIO_ACTUALIZADO,
    PERFIL_ACTUALIZADO,
    PASSWORD_CAMBIADA,
    CUENTA_DADA_DE_BAJA,
    RESENIA_ACTUALIZADA,
    RESENIA_RESPONDIDA,
    RESENIA_DENUNCIADA,
    /** Un admin bajo del listado publico una resenia ya publicada, sin denuncia de por medio. */
    RESENIA_OCULTADA,
    IMAGEN_ACTIVIDAD_AGREGADA,
    IMAGEN_ACTIVIDAD_ELIMINADA,
    SOLICITUD_INSTRUCTOR_REABIERTA,
    AUSENCIA_PROFESOR_NOTIFICADA,
    FAVORITO_AGREGADO,
    FAVORITO_QUITADO,
    CATEGORIA_CREADA,
    CATEGORIA_ACTUALIZADA,
    CATEGORIA_ELIMINADA,
    DOCUMENTO_INSTRUCTOR_SUBIDO,
    FOTO_PERFIL_SUBIDA,
    FOTO_ACTIVIDAD_SUBIDA,
    ROL_CREADO,
    PERMISOS_ACTUALIZADOS,
    INTERESES_ACTUALIZADOS,
    ROL_ASIGNADO,
    NIVEL_INTENSIDAD_CREADO,
    NIVEL_INTENSIDAD_ACTUALIZADO,
    NIVEL_INTENSIDAD_ELIMINADO,
    /** Alguien mando el formulario "Reportar un problema" de /ayuda. El actor es null si no tenia cuenta. */
    REPORTE_SOPORTE_CREADO,
    REPORTE_SOPORTE_CERRADO
}
