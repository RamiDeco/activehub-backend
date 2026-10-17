package com.activehub.domain.penalizacion;

import java.math.BigDecimal;

/**
 * Umbrales de negocio de las penalizaciones.
 *
 * <p>El minimo de 15 dias para una Suspension temporal lo fijo el usuario: una suspension
 * mas corta no cambia nada en la practica (el instructor pierde a lo sumo una clase) y
 * termina siendo un tiron de orejas sin efecto. Vale tanto para el alta manual del admin
 * como para la suspension que sale de resolver una denuncia.
 *
 * <p>Los <b>topes</b> tambien los pidio el usuario: sin ellos el formulario aceptaba una
 * suspension de cien años o una multa de un millon, que no son sanciones sino errores de
 * tipeo con consecuencias. Una suspension mas larga que un año es, en los hechos, una baja,
 * y para eso esta la baja de la cuenta. El frontend replica los cuatro numeros en
 * {@code lib/limites.ts}; si cambias uno, cambialo alla tambien.
 */
public final class VentanaPenalizacion {

    public static final int MINIMO_DIAS_SUSPENSION = 15;
    public static final int MAXIMO_DIAS_SUSPENSION = 365;
    public static final BigDecimal MONTO_MINIMO_MULTA = new BigDecimal("1000");
    public static final BigDecimal MONTO_MAXIMO_MULTA = new BigDecimal("500000");

    private VentanaPenalizacion() {
    }

    /** Mensaje unico para el rango de la multa, asi las dos pantallas y los dos slices dicen lo mismo. */
    public static String mensajeRangoMulta() {
        return "La multa tiene que estar entre $" + formato(MONTO_MINIMO_MULTA)
                + " y $" + formato(MONTO_MAXIMO_MULTA) + ".";
    }

    public static boolean multaFueraDeRango(BigDecimal monto) {
        return monto.compareTo(MONTO_MINIMO_MULTA) < 0 || monto.compareTo(MONTO_MAXIMO_MULTA) > 0;
    }

    public static String mensajeMaximoDias() {
        return "La suspensión no puede durar más de " + MAXIMO_DIAS_SUSPENSION + " días.";
    }

    private static String formato(BigDecimal monto) {
        return String.format(java.util.Locale.forLanguageTag("es-AR"), "%,d", monto.longValue());
    }
}
