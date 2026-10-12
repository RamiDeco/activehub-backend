package com.activehub.shared.notificacion;

import java.util.UUID;

/**
 * Par {@link DestinoNotificacion} + id de ruta, para que no se pueda guardar un tipo de
 * destino con el id de otra cosa. Se arma con las fábricas, nunca con {@code new}.
 *
 * @param tipo a qué pantalla lleva el click.
 * @param id   el identificador que esa pantalla espera en su ruta. Nulo solo en {@link #ninguno()}.
 */
public record Destino(DestinoNotificacion tipo, UUID id) {

    public static Destino actividad(UUID actividadId) {
        return new Destino(DestinoNotificacion.ACTIVIDAD, actividadId);
    }

    public static Destino clase(UUID claseId) {
        return new Destino(DestinoNotificacion.CLASE, claseId);
    }

    public static Destino inscripcion(UUID inscripcionId) {
        return new Destino(DestinoNotificacion.INSCRIPCION, inscripcionId);
    }

    public static Destino resenia(UUID reseniaId) {
        return new Destino(DestinoNotificacion.RESENIA, reseniaId);
    }

    public static Destino denuncia(UUID denunciaId) {
        return new Destino(DestinoNotificacion.DENUNCIA, denunciaId);
    }

    public static Destino perfilInstructor(UUID usuarioId) {
        return new Destino(DestinoNotificacion.PERFIL_INSTRUCTOR, usuarioId);
    }

    /** Para lo que no tiene pantalla propia (una penalización, una actividad ya eliminada). */
    public static Destino ninguno() {
        return new Destino(DestinoNotificacion.NINGUNO, null);
    }
}
