package com.activehub.domain.inscripcion;

import java.time.Duration;
import java.time.Instant;

/**
 * Umbrales de negocio (CLAUDE.md): >4 dias hasta la clase => solo
 * PreInscripcion; <=4 dias y >=1 hora => inscripcion definitiva. Comparado
 * como duracion exacta (no dias de calendario) para no depender de zona
 * horaria ni de redondeos.
 */
public final class VentanaInscripcion {

    private static final Duration UMBRAL_PREINSCRIPCION = Duration.ofDays(4);
    private static final Duration UMBRAL_CIERRE = Duration.ofHours(1);

    private VentanaInscripcion() {
    }

    public static boolean esVentanaPreInscripcion(Instant ahora, Instant fechaHora) {
        return Duration.between(ahora, fechaHora).compareTo(UMBRAL_PREINSCRIPCION) > 0;
    }

    public static boolean esVentanaInscripcion(Instant ahora, Instant fechaHora) {
        Duration faltante = Duration.between(ahora, fechaHora);
        return faltante.compareTo(UMBRAL_PREINSCRIPCION) <= 0 && faltante.compareTo(UMBRAL_CIERRE) >= 0;
    }
}
