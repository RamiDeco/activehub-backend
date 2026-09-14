package com.activehub.shared.time;

import java.time.ZoneId;

/**
 * Zona horaria del negocio. Todo lo que se guarda es {@code Instant} (UTC), pero cuando hay que
 * pasar a un concepto de calendario — "que dia de la semana es", "que hora es", "vencio hoy" —
 * la respuesta depende de la zona, y para ActiveHub esa zona es la de Argentina, no la del
 * servidor. La constante estaba duplicada en tres servicios.
 */
public final class Zonas {

    public static final ZoneId AR = ZoneId.of("America/Argentina/Buenos_Aires");

    private Zonas() {
    }
}
