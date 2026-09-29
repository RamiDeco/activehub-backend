package com.activehub.usecases.listarauditoria;

import com.activehub.shared.audit.AuditAccion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Traduce una fila de auditoria a una frase que un administrador pueda leer.
 *
 * <p>Reportado: la columna "Detalle" mostraba cosas como
 * {@code "cb2ce9c9-af7c-4668-9bd6-d70aec60c25b · -roles.configurar"}. Eso es trazabilidad
 * valida pero ilegible: el id de la entidad y la clave tecnica del permiso no le dicen nada a
 * quien audita.
 *
 * <p><b>Por que en el backend y no en la pantalla.</b> Para escribir "quito el permiso
 * «Configurar roles y permisos» del rol Administrador" hacen falta dos catalogos —permisos y
 * roles— que viven en la base. Resolverlo del lado del cliente obligaria a duplicar el
 * catalogo de 17 claves en el frontend, que es exactamente lo que el repo evita. Ademas asi
 * quedan legibles <b>tambien las filas viejas</b>: la descripcion se calcula al leer, no se
 * guarda.
 *
 * <p><b>El detalle tecnico no se pierde</b>: el id de la entidad tiene su propia columna en la
 * pantalla y en el PDF. Esto reemplaza al texto crudo, no a la evidencia.
 *
 * <p>Regla al agregar un {@link AuditAccion} nuevo: si se puede armar una frase especifica,
 * sumala en el switch; si no, el generico ya dice quien hizo que sobre que tipo de entidad,
 * que sigue siendo infinitamente mas util que un UUID.
 */
final class DescripcionAuditoria {

    private DescripcionAuditoria() {
    }

    /**
     * @param permisosPorClave clave tecnica -> etiqueta humana ("roles.configurar" -> "Configurar
     *                         roles y permisos").
     * @param rolesPorId       id de rol -> nombre.
     */
    static String describir(
            AuditAccion accion,
            String actor,
            String metadata,
            java.util.UUID entidadId,
            Map<String, String> permisosPorClave,
            Map<java.util.UUID, String> rolesPorId) {

        String meta = metadata == null ? "" : metadata.trim();

        return switch (accion) {
            case PERMISOS_ACTUALIZADOS -> permisosActualizados(actor, meta, entidadId, permisosPorClave, rolesPorId);
            case ROL_ASIGNADO -> actor + " le asignó el rol " + nombreRol(meta) + " a un usuario.";
            case ROL_CREADO -> actor + " creó el rol " + nombreRol(meta) + ".";

            case LOGIN_OK -> actor + " inició sesión.";
            case LOGIN_FALLIDO -> "Intento de inicio de sesión fallido" + (meta.isEmpty() ? "." : " (" + meta + ").");
            case LOGIN_BLOQUEADO -> "Cuenta bloqueada temporalmente por intentos fallidos de inicio de sesión.";
            case ACCESO_DENEGADO -> actor + " intentó acceder a algo para lo que no tiene permiso"
                    + (meta.isEmpty() ? "." : ": " + meta + ".");

            case REGISTRO_ALUMNO -> "Se registró un alumno nuevo.";
            case REGISTRO_GOOGLE -> "Se registró un alumno nuevo con su cuenta de Google.";
            case EMAIL_VERIFICADO -> actor + " confirmó su correo con el código que le enviamos.";
            case EMAIL_CAMBIADO -> actor + " cambió su correo"
                    + (meta.isEmpty() ? "." : " a " + meta + ", confirmándolo con un código.");
            case REGISTRO_INSTRUCTOR -> "Se registró un instructor nuevo.";
            case ADMIN_CREADO -> actor + " creó una cuenta de administrador.";

            case ACTIVIDAD_CREADA -> actor + " publicó una actividad.";
            case ACTIVIDAD_ACTUALIZADA -> actor + " editó los datos de una actividad.";
            case ACTIVIDAD_ELIMINADA -> actor + " eliminó una actividad.";

            case CLASE_CREADA -> "AGENDA".equals(meta)
                    ? "El sistema generó una clase a partir de una agenda semanal."
                    : actor + " creó una clase.";
            case CLASE_ACTUALIZADA -> actor + " editó una clase.";
            case CLASE_ELIMINADA -> actor + " eliminó una clase sin inscriptos.";
            case CLASE_CANCELADA -> "PENALIZACION".equals(meta)
                    ? "Se canceló una clase por la suspensión de su instructor; se reintegró a los inscriptos."
                    : actor + " canceló una clase y se reintegró a los inscriptos.";
            case CLASE_HABILITADA -> "El sistema habilitó la inscripción a una clase próxima.";
            case CLASE_FINALIZADA -> "El sistema dio por finalizada una clase que ya se dictó.";

            case PREINSCRIPCION_CREADA -> actor + " se preinscribió a una clase.";
            case INSCRIPCION_CREADA -> actor + " se inscribió a una clase.";
            case INSCRIPCION_ACTUALIZADA -> "Se actualizó una inscripción.";
            case INSCRIPCION_CANCELADA -> actor + " canceló una inscripción.";
            case COBRO_EFECTIVO_CONFIRMADO -> actor + " confirmó el cobro en efectivo de una inscripción.";
            case PAGO_LIBERADO -> "El sistema liberó un pago al instructor: la clase finalizó y pasó el período de denuncias.";

            case RESENIA_CREADA -> actor + " escribió una reseña.";
            case RESENIA_ACTUALIZADA -> actor + " editó su reseña; vuelve a moderación.";
            case RESENIA_ELIMINADA -> actor + " eliminó una reseña.";
            case RESENIA_APROBADA -> actor + " aprobó una reseña y quedó publicada.";
            case RESENIA_RECHAZADA -> actor + " rechazó una reseña.";
            case RESENIA_RESPONDIDA -> actor + " respondió públicamente una reseña que recibió.";
            case RESENIA_DENUNCIADA -> actor + " reportó una reseña que recibió.";
            case RESENIA_OCULTADA -> actor + " ocultó una reseña publicada"
                    + (meta.isEmpty() ? "." : ". Motivo: " + meta + ".");

            case DENUNCIA_CREADA -> actor + " reportó la inasistencia de un instructor.";
            case DENUNCIA_EN_AUDITORIA -> actor + " tomó una denuncia para auditarla.";
            case DENUNCIA_RESUELTA -> actor + " resolvió una denuncia" + (meta.isEmpty() ? "." : " (" + resolucion(meta) + ").");
            case PENALIZACION_APLICADA -> actor + " aplicó una penalización" + (meta.isEmpty() ? "." : ": " + penalizacion(meta) + ".");
            case SUSPENSION_LEVANTADA -> "El sistema levantó una suspensión cuya vigencia venció.";

            case USUARIO_ESTADO_ACTUALIZADO -> actor + " cambió el estado de una cuenta" + (meta.isEmpty() ? "." : ": " + meta + ".");
            case USUARIO_ACTUALIZADO -> actor + " editó los datos de un usuario.";
            case PERFIL_ACTUALIZADO -> actor + " actualizó sus propios datos.";
            case PASSWORD_CAMBIADA -> actor + " cambió su contraseña.";
            case PASSWORD_RESTABLECIDA -> actor + " restableció su contraseña con un código enviado a su correo.";
            case CUENTA_DADA_DE_BAJA -> actor + " dio de baja su cuenta.";
            case INTERESES_ACTUALIZADOS -> actor + " actualizó sus intereses deportivos.";

            case INSTRUCTOR_VALIDADO -> actor + " aprobó la solicitud de un instructor.";
            case INSTRUCTOR_RECHAZADO -> actor + " rechazó la solicitud de un instructor.";
            case SOLICITUD_INSTRUCTOR_REABIERTA -> actor + " volvió a postularse como instructor.";
            case DOCUMENTO_INSTRUCTOR_SUBIDO -> actor + " subió documentación de instructor.";
            case AUSENCIA_PROFESOR_NOTIFICADA -> actor + " notificó que no va a dictar una clase; se reintegró a los inscriptos.";

            case CATEGORIA_CREADA -> actor + " creó una categoría.";
            case CATEGORIA_ACTUALIZADA -> actor + " editó una categoría.";
            case CATEGORIA_ELIMINADA -> actor + " eliminó una categoría.";
            case TIPO_ACTIVIDAD_CREADO -> actor + " creó un tipo de actividad.";
            case TIPO_ACTIVIDAD_ACTUALIZADO -> actor + " editó un tipo de actividad.";
            case TIPO_ACTIVIDAD_ELIMINADO -> actor + " eliminó un tipo de actividad.";
            case NIVEL_INTENSIDAD_CREADO -> actor + " creó un nivel de intensidad.";
            case NIVEL_INTENSIDAD_ACTUALIZADO -> actor + " editó un nivel de intensidad.";
            case NIVEL_INTENSIDAD_ELIMINADO -> actor + " eliminó un nivel de intensidad.";

            case FOTO_PERFIL_SUBIDA -> actor + " cambió su foto de perfil.";
            case FOTO_ACTIVIDAD_SUBIDA -> actor + " cambió la portada de una actividad.";
            case IMAGEN_ACTIVIDAD_AGREGADA -> actor + " agregó una imagen a la galería de una actividad.";
            case IMAGEN_ACTIVIDAD_ELIMINADA -> actor + " eliminó una imagen de la galería de una actividad.";
            case FAVORITO_AGREGADO -> actor + " agregó una actividad a favoritos.";
            case FAVORITO_QUITADO -> actor + " quitó una actividad de favoritos.";

            // El alta es publica: si la mando un visitante sin cuenta, el actor ya viene
            // resuelto como "Sistema" y la frase tiene que seguir leyendose bien igual.
            case REPORTE_SOPORTE_CREADO -> "Se recibió un reporte de soporte desde la pantalla de Ayuda.";
            case REPORTE_SOPORTE_CERRADO -> actor + " cerró un reporte de soporte.";
        };
    }

    /**
     * {@code PERMISOS_ACTUALIZADOS} guarda los cambios como {@code "+clave, -clave"} — el signo
     * dice si se dio o se quito. Es la unica accion cuya metadata tiene estructura, asi que es
     * la unica que vale la pena parsear en detalle; es tambien la que motivo el reporte.
     */
    private static String permisosActualizados(
            String actor,
            String meta,
            java.util.UUID rolId,
            Map<String, String> permisosPorClave,
            Map<java.util.UUID, String> rolesPorId) {

        String rol = rolesPorId.getOrDefault(rolId, "un rol");
        if (meta.isEmpty() || "sin cambios".equals(meta)) {
            return actor + " guardó los permisos del rol " + rol + " sin cambios.";
        }

        List<String> dados = new ArrayList<>();
        List<String> quitados = new ArrayList<>();
        for (String parte : meta.split(",")) {
            String p = parte.trim();
            if (p.length() < 2) {
                continue;
            }
            String clave = p.substring(1);
            String etiqueta = "«" + permisosPorClave.getOrDefault(clave, clave) + "»";
            if (p.charAt(0) == '+') {
                dados.add(etiqueta);
            } else if (p.charAt(0) == '-') {
                quitados.add(etiqueta);
            }
        }

        StringBuilder sb = new StringBuilder(actor);
        if (!dados.isEmpty()) {
            sb.append(" agregó el permiso ").append(String.join(", ", dados));
        }
        if (!quitados.isEmpty()) {
            sb.append(dados.isEmpty() ? " quitó el permiso " : " y quitó ").append(String.join(", ", quitados));
        }
        if (dados.isEmpty() && quitados.isEmpty()) {
            return actor + " actualizó los permisos del rol " + rol + ".";
        }
        return sb.append(" al rol ").append(rol).append(".").toString();
    }

    /** La metadata de rol guarda el nombre tal cual; se muestra entre comillas si viene. */
    private static String nombreRol(String meta) {
        return meta.isEmpty() ? "un rol" : "«" + meta + "»";
    }

    private static String resolucion(String meta) {
        return switch (meta) {
            case "REINTEGRAR" -> "se reintegró el pago";
            case "SUSPENDER" -> "se suspendió al instructor";
            case "PENALIZAR" -> "se le aplicó una multa al instructor";
            case "DESESTIMAR" -> "se desestimó";
            case "OCULTAR_RESENIA" -> "se ocultó la reseña";
            default -> meta;
        };
    }

    private static String penalizacion(String meta) {
        if (meta.startsWith("ECONOMICA")) {
            return "multa económica";
        }
        if (meta.startsWith("SUSPENSION_TEMPORAL")) {
            return "suspensión temporal" + (meta.contains("·") ? meta.substring(meta.indexOf('·') + 1).trim() : "");
        }
        return meta;
    }
}
