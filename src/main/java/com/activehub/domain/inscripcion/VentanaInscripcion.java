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

    /** Publico: lo usa tambien el job que pasa las clases de Programada a Habilitada. */
    public static final Duration UMBRAL_PREINSCRIPCION = Duration.ofDays(4);

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

    /**
     * Una clase esta <b>congelada</b> cuando ya entro a la ventana de inscripcion (faltan
     * {@code <= 4 dias}) y tiene al menos un inscripto definitivo: a partir de ahi hay gente
     * que pago por unos datos concretos, asi que no se editan ni se pisan desde la actividad.
     *
     * <p>Se mira {@code faltante <= UMBRAL_PREINSCRIPCION} y no {@code esVentanaInscripcion}
     * a proposito: esa otra devuelve false cuando faltan menos de {@code UMBRAL_CIERRE} para
     * empezar, o sea que la clase se "descongelaria" justo en la ultima hora y una vez dictada.
     * Congelada es un estado sin vuelta atras.
     *
     * <p>El camino para una clase congelada que igual no se puede dictar no es editarla sino
     * cancelarla ({@code cancelarclase}), que reintegra y avisa a cada alumno.
     */
    public static boolean estaCongelada(Instant ahora, Instant fechaHora, int cuposOcupados) {
        return cuposOcupados > 0
                && Duration.between(ahora, fechaHora).compareTo(UMBRAL_PREINSCRIPCION) <= 0;
    }
}
