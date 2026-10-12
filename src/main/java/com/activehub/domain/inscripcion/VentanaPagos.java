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

    /**
     * Cuanto dura una autorizacion de Mercado Pago sin capturar en Argentina (MP-01). Pasado
     * ese plazo MP la cancela sola y la plata vuelve al alumno.
     */
    public static final Duration VIGENCIA_AUTORIZACION = Duration.ofDays(5);

    /**
     * KAN-61: si a una autorizacion le falta menos que esto para vencer, se captura aunque la
     * clase todavia no haya finalizado. En el flujo normal no pasa (la inscripcion abre 4 dias
     * antes y se captura al empezar la clase), pero si el scheduler estuvo caido o la clase se
     * corrio de fecha, esperar a que finalice es perder el cobro.
     */
    public static final Duration MARGEN_CAPTURA_ANTICIPADA = Duration.ofHours(24);

    /**
     * KAN-61: a la cantidad de capturas fallidas de un mismo pago se avisa a la administracion.
     * Con el scheduler cada 5 minutos, 6 intentos es media hora fallando: ya no es un tropiezo.
     */
    public static final int MAX_INTENTOS_CAPTURA = 6;

    private VentanaPagos() {
    }
}
