package com.activehub.usecases.recomendaractividades;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param sinSenales    true cuando el alumno no declaro intereses y tampoco tiene historial:
 *                      ahi no se recomienda <b>nada</b> y la pantalla muestra la invitacion a
 *                      cargar sus intereses. Es lo que evita el comportamiento viejo, que
 *                      rellenaba con las primeras tres actividades del catalogo y las
 *                      rotulaba "Recomendado para vos".
 * @param recomendadas  ya ordenadas de mayor a menor puntaje.
 */
public record RecomendarActividadesResponse(boolean sinSenales, List<Recomendada> recomendadas) {

    /**
     * La actividad con la misma forma que devuelve el catalogo, mas las dos cosas propias de
     * una recomendacion.
     *
     * @param puntaje puntaje calculado, redondeado a dos decimales. Viaja para poder explicar
     *                y auditar el orden; la pantalla no lo muestra.
     * @param motivos por que se recomienda, en castellano y listo para mostrar. Es la parte
     *                que vuelve auditable al motor: una recomendacion que no se puede explicar
     *                se lee como un error.
     */
    public record Recomendada(
            UUID id,
            String nombre,
            TipoActividad tipoActividad,
            Categoria categoria,
            NivelIntensidad nivelIntensidad,
            Instructor instructor,
            BigDecimal precio,
            String ubicacion,
            String photoTint,
            BigDecimal rating,
            int duracionMin,
            ProximaClase proximaClase,
            Double latitud,
            Double longitud,
            double puntaje,
            List<String> motivos
    ) {
    }

    public record NivelIntensidad(UUID id, String nombre) {
    }

    public record TipoActividad(UUID id, String nombre) {
    }

    public record Categoria(UUID id, String nombre) {
    }

    public record Instructor(UUID id, String nombre, String apellido) {
    }

    public record ProximaClase(Instant fechaHora, String estado, int cuposMax, int cuposOcupados) {
    }
}
