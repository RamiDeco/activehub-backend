package com.activehub.usecases.iniciarsesiongoogle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @param idToken el JWT que devuelve Google Identity Services en el navegador (el campo
 *                {@code credential} del callback). Se verifica del lado del servidor: confiar
 *                en un correo que mandó el cliente sería dejar entrar como cualquiera.
 * @param rol     con qué rol quiere registrarse <b>si la cuenta no existe</b>. Es indiferente
 *                cuando ya existe: ahí siempre se entra.
 *
 *                <ul>
 *                  <li>{@code ALUMNO} — se crea en el acto.</li>
 *                  <li>{@code INSTRUCTOR} — <b>no</b> se crea: el alta de instructor exige
 *                      documentación (RN-12), así que vuelve la identidad para precargar el
 *                      formulario.</li>
 *                  <li><b>vacío</b> — es el botón del <b>login</b>, donde no se eligió ningún
 *                      rol. Tampoco se crea nada: quien aprieta "Iniciar sesión con Google"
 *                      espera entrar a su cuenta, no que le aparezca una nueva a medio
 *                      llenar. Vuelve {@code SIN_CUENTA} y la pantalla lo manda a
 *                      registrarse.</li>
 *                </ul>
 */
public record IniciarSesionGoogleRequest(
        @NotBlank(message = "Falta el token de Google") String idToken,
        @Pattern(regexp = "^(ALUMNO|INSTRUCTOR)?$", message = "Rol no soportado") String rol
) {
}
