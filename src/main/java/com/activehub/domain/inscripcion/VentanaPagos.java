package com.activehub.domain.inscripcion;

import java.time.Duration;

/**
 * Umbrales de negocio del ciclo de vida del Pago.
 *
 * <p>RN-04: el pago de Mercado Pago queda Retenido hasta que la clase finaliza y
 * transcurre el "periodo de denuncias"; recien ahi pasa a Liberado y se acredita
 * al instructor.
 *
 * <p>La especificacion funcional nombra ese periodo pero NO define su duracion
 * (es la ambiguedad 2 de la seccion 12 de HISTORIAS-DE-USUARIO.md). Se adopta 48 h
 * como valor por defecto: da mas de un dia habil al alumno para reportar una
 * inasistencia y mantiene el dinero en movimiento. Si el negocio define otro plazo,
 * se cambia unicamente esta constante.
 */
public final class VentanaPagos {

    public static final Duration PERIODO_DENUNCIAS = Duration.ofHours(48);

    private VentanaPagos() {
    }
}
