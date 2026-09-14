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
 * (es la ambiguedad 2 de la seccion 12 de HISTORIAS-DE-USUARIO.md). El usuario la
 * resolvio en <b>24 h</b> (el default provisorio nuestro habia sido 48 h): un dia para
 * que el alumno reporte una inasistencia, y el instructor cobra al dia siguiente. Si el
 * negocio lo cambia otra vez, se toca unicamente esta constante.
 */
public final class VentanaPagos {

    public static final Duration PERIODO_DENUNCIAS = Duration.ofHours(24);

    private VentanaPagos() {
    }
}
