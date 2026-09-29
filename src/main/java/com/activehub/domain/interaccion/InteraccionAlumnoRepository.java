package com.activehub.domain.interaccion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InteraccionAlumnoRepository extends JpaRepository<InteraccionAlumno, UUID> {

    /**
     * Las ultimas interacciones del alumno, con la actividad y su taxonomia ya cargadas.
     *
     * <p><b>Con tope y no todas.</b> Es un log que crece sin limite y el motor aplica un
     * decaimiento por antiguedad, asi que lo viejo pesa cada vez menos: traer el historico
     * completo costaria mas y cambiaria poco el resultado. El tope lo pone quien llama.
     *
     * <p>El LEFT JOIN FETCH es obligatorio: una BUSQUEDA no tiene actividad, y con INNER
     * desapareceria del resultado -- la misma trampa que ya costo las denuncias de reseña.
     */
    @Query("SELECT i FROM InteraccionAlumno i "
            + "LEFT JOIN FETCH i.actividad a "
            + "LEFT JOIN FETCH a.tipoActividad t "
            + "LEFT JOIN FETCH t.categoria "
            + "LEFT JOIN FETCH a.nivelIntensidad "
            + "LEFT JOIN FETCH a.instructor "
            + "WHERE i.usuario.id = :usuarioId "
            + "ORDER BY i.createdAt DESC")
    List<InteraccionAlumno> ultimasDelAlumno(@Param("usuarioId") UUID usuarioId, Limit limite);

    /**
     * Si el alumno ya miro esta actividad hace poco. Lo usa {@code registrarinteraccion} para
     * no guardar una fila por cada vez que se repinta el detalle: sin esto, abrir una
     * actividad y volver diez veces la convertiria en el gusto dominante del alumno.
     */
    @Query("SELECT COUNT(i) > 0 FROM InteraccionAlumno i "
            + "WHERE i.usuario.id = :usuarioId AND i.tipo = com.activehub.domain.interaccion.TipoInteraccion.VISTA_ACTIVIDAD "
            + "AND i.actividad.id = :actividadId AND i.createdAt >= :desde")
    boolean existeVistaReciente(
            @Param("usuarioId") UUID usuarioId,
            @Param("actividadId") UUID actividadId,
            @Param("desde") Instant desde);

    /**
     * Lo mismo para una busqueda, que es lo que evita una fila por cada tecla.
     *
     * <p><b>Son dos metodos y no uno con parametros opcionales</b>, y no es preferencia de
     * estilo: la version unificada pasaba {@code null} en el parametro de texto cuando la
     * interaccion era una vista, y Postgres recibe ese null sin tipo como {@code bytea} —
     * {@code lower(bytea)} no existe, asi que toda vista terminaba en un 500. No volver a
     * unificarlos.
     */
    @Query("SELECT COUNT(i) > 0 FROM InteraccionAlumno i "
            + "WHERE i.usuario.id = :usuarioId AND i.tipo = com.activehub.domain.interaccion.TipoInteraccion.BUSQUEDA "
            + "AND lower(i.termino) = lower(:termino) AND i.createdAt >= :desde")
    boolean existeBusquedaReciente(
            @Param("usuarioId") UUID usuarioId,
            @Param("termino") String termino,
            @Param("desde") Instant desde);
}
