package com.activehub.usecases.iniciarsesiongoogle;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tres desenlaces posibles, discriminados por {@code modo}:
 *
 * <ul>
 *   <li><b>{@code SESION}</b> — hay cuenta y hay token. Es lo que pasa siempre que ya existe
 *       una cuenta verificada con ese correo, y también cuando se elige registrarse como
 *       alumno (que se crea en el acto).</li>
 *   <li><b>{@code SIN_CUENTA}</b> — no hay cuenta con ese correo y no se pidió crear ninguna
 *       (el botón del login). La pantalla manda a registrarse.</li>
 *   <li><b>{@code COMPLETAR_INSTRUCTOR}</b> — <b>no se creó nada</b>. Google alcanza para
 *       probar quién es la persona, pero un instructor no puede darse de alta sin
 *       documentación (RN-12), así que se devuelve sólo la identidad para precargar el
 *       formulario. La cuenta la crea `registrarinstructor` cuando llegan los archivos.</li>
 * </ul>
 *
 * @param identidad presente en {@code COMPLETAR_INSTRUCTOR} (completa) y en
 *                  {@code SIN_CUENTA} (sólo el correo).
 */
public record IniciarSesionGoogleResponse(
        String modo,
        String token,
        boolean cuentaNueva,
        Usuario usuario,
        Identidad identidad
) {

    public static final String MODO_SESION = "SESION";
    public static final String MODO_COMPLETAR_INSTRUCTOR = "COMPLETAR_INSTRUCTOR";
    public static final String MODO_SIN_CUENTA = "SIN_CUENTA";

    public static IniciarSesionGoogleResponse sesion(String token, boolean cuentaNueva, Usuario usuario) {
        return new IniciarSesionGoogleResponse(MODO_SESION, token, cuentaNueva, usuario, null);
    }

    public static IniciarSesionGoogleResponse completarInstructor(Identidad identidad) {
        return new IniciarSesionGoogleResponse(MODO_COMPLETAR_INSTRUCTOR, null, true, null, identidad);
    }

    /**
     * No existe cuenta con ese correo y no se pidió crear ninguna (el botón del login). Se
     * devuelve la dirección para que la pantalla pueda decir con cuál se intentó entrar.
     */
    public static IniciarSesionGoogleResponse sinCuenta(String email) {
        return new IniciarSesionGoogleResponse(
                MODO_SIN_CUENTA, null, false, null, new Identidad(email, "", "", null));
    }

    /**
     * Lo que Google confirmó de la persona.
     *
     * @param idToken el mismo token que mandó el cliente, devuelto para que lo adjunte al alta
     *                de instructor. **Se vuelve a verificar allá**: acá no se guarda estado, y
     *                confiar en que "ya lo validamos hace un rato" sería confiar en el cliente.
     */
    public record Identidad(String email, String nombre, String apellido, String idToken) {
    }

    public record Usuario(
            UUID id,
            String nombre,
            String apellido,
            String email,
            String telefono,
            LocalDate fechaNacimiento,
            String rol,
            String estado,
            int cantidadPenalizaciones,
            Instant createdAt,
            boolean emailVerificado,
            String authProveedor
    ) {
    }
}
