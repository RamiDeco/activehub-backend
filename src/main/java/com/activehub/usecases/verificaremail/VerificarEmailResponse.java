package com.activehub.usecases.verificaremail;

/**
 * @param token           un JWT <b>nuevo</b>. El que tiene el cliente lleva el claim
 *                        {@code emailVerificado: false}, que es lo que usa
 *                        {@code EmailVerificadoFilter} para bloquear todo: si siguiera usando
 *                        el viejo, confirmar el código no cambiaría nada hasta que venciera.
 *                        Hay que reemplazarlo al recibir esta respuesta.
 * @param email           el correo que quedó verificado. En un cambio es el nuevo, así que el
 *                        frontend tiene que refrescar la sesión con este valor.
 * @param emailVerificado siempre true si la llamada salió bien; viaja explícito para que el
 *                        cliente actualice el flag sin volver a pedir {@code /api/auth/me}.
 * @param cambioDeEmail   si lo confirmado fue un cambio y no el alta. Decide el cartel de éxito.
 */
public record VerificarEmailResponse(String token, String email, boolean emailVerificado, boolean cambioDeEmail) {
}
