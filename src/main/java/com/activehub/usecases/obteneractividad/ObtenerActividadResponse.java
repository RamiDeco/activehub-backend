package com.activehub.usecases.obteneractividad;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ObtenerActividadResponse(
        UUID id,
        String nombre,
        String descripcion,
        TipoActividad tipoActividad,
        Categoria categoria,
        NivelIntensidad nivelIntensidad,
        Instructor instructor,
        BigDecimal precio,
        String ubicacion,
        String photoTint,
        BigDecimal rating,
        int duracionMin,
        /** Galería completa (sección 2: `imagenes[]`). La portada sigue siendo `fotoPath`. */
        List<UUID> imagenes,
        /** Si la actividad tiene portada (se sirve por `/api/fotos/actividad/{id}`). */
        boolean tienePortada,
        /**
         * La imagen de la galería que es además la portada (comparten el archivo), o null si la
         * portada se subió aparte o no hay. Sirve para no mostrar la misma foto dos veces.
         */
        UUID portadaImagenId,
        List<Clase> clases,
        Double latitud,
        Double longitud
) {
    public record NivelIntensidad(UUID id, String nombre) {
    }

    public record TipoActividad(UUID id, String nombre) {
    }

    public record Categoria(UUID id, String nombre) {
    }

    /**
     * @param aceptaMercadoPago false = la clase solo acepta Efectivo (KAN-57)
     * @param mpPublicKey       public key de Mercado Pago del instructor para el Brick; null si
     *                          no acepta. Nunca viaja un access token.
     */
    public record Instructor(UUID id, String nombre, String apellido, boolean aceptaMercadoPago, String mpPublicKey) {
    }

    public record Clase(
            UUID id, Instant fechaHora, Instant horaFin, String estado, int cuposMax, int cuposOcupados,
            int cantidadPreInscripcion,
            /**
             * Precio de ESTA clase (V23), que es el que va a pagar quien se anote — no el de la
             * actividad, que pudo haber cambiado después de que la clase se congelara. El panel
             * de inscripción del alumno mostraba el de la actividad y prometía un importe
             * distinto del que después cobraba {@code inscribirse}.
             */
            BigDecimal precio) {
    }
}
