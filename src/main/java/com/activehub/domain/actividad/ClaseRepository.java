package com.activehub.domain.actividad;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaseRepository extends JpaRepository<Clase, UUID> {

    List<Clase> findByActividadIdAndEstadoNotInOrderByFechaHoraAsc(UUID actividadId, Collection<EstadoClase> estados);

    List<Clase> findByActividadIdInAndEstadoNotInOrderByFechaHoraAsc(
            Collection<UUID> actividadIds, Collection<EstadoClase> estados);

    List<Clase> findByEstadoInAndFechaHoraBefore(Collection<EstadoClase> estados, Instant limite);

    /**
     * UPDATE atomico condicional: evita la doble ocupacion del ultimo cupo
     * bajo pedidos concurrentes sin necesitar un lock explicito ni un
     * read-then-write en Java. Devuelve 0 filas afectadas si no habia cupo.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Clase c SET c.cuposOcupados = c.cuposOcupados + 1 WHERE c.id = :id AND c.cuposOcupados < c.cuposMax")
    int ocuparCupo(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Clase c SET c.cuposOcupados = c.cuposOcupados - 1 WHERE c.id = :id AND c.cuposOcupados > 0")
    int liberarCupo(@Param("id") UUID id);
}
