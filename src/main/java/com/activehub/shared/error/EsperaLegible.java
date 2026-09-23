package com.activehub.shared.error;

import java.time.Duration;

/**
 * Convierte una espera en algo que se pueda leer en un mensaje: "unos segundos", "2 minutos",
 * "alrededor de 3 horas".
 *
 * <p>Existe porque el tiempo que devuelve el proveedor es exacto e inútil de mostrar: nadie quiere
 * leer "volvé en 154,32 segundos". Se redondea <b>hacia arriba</b> a propósito — prometer menos de
 * lo que falta hace que la persona vuelva, encuentre el mismo cartel y crea que está roto.
 */
final class EsperaLegible {

    private EsperaLegible() {
    }

    static String enPalabras(Duration espera) {
        if (espera == null || espera.isNegative() || espera.isZero()) {
            return "un rato";
        }
        long segundos = espera.toSeconds();
        if (segundos <= 45) {
            return "unos segundos";
        }
        if (segundos < 90) {
            return "un minuto";
        }
        long minutos = (segundos + 59) / 60;
        if (minutos < 60) {
            return minutos + " minutos";
        }
        long horas = (minutos + 59) / 60;
        return horas == 1 ? "alrededor de una hora" : "alrededor de " + horas + " horas";
    }
}
