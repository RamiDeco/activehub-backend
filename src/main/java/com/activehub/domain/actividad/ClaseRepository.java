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

    @Query("SELECT c FROM Clase c JOIN FETCH c.actividad a WHERE a.instructor.id = :instructorId ORDER BY c.fechaHora ASC")
    List<Clase> findByInstructorIdConDetalle(@Param("instructorId") UUID instructorId);

    @Query("SELECT c FROM Clase c JOIN FETCH c.actividad a ORDER BY c.fechaHora ASC")
    List<Clase> findAllConDetalle();

    /**
     * E2I-HU06 criterio 8: solapamiento de horario dentro de la misma actividad. Dos
     * intervalos se pisan si cada uno empieza antes de que el otro termine; se excluyen
     * las Canceladas porque un horario liberado se puede volver a usar.
     */
    @Query("SELECT COUNT(c) > 0 FROM Clase c WHERE c.actividad.id = :actividadId "
            + "AND c.estado <> com.activehub.domain.actividad.EstadoClase.Cancelada "
            + "AND (:excluirClaseId IS NULL OR c.id <> :excluirClaseId) "
            + "AND c.fechaHora < :horaFin AND c.horaFin > :fechaHora")
    boolean existeSolapamiento(
            @Param("actividadId") UUID actividadId,
            @Param("fechaHora") Instant fechaHora,
            @Param("horaFin") Instant horaFin,
            @Param("excluirClaseId") UUID excluirClaseId);

    boolean existsByAgendaClasesIdAndFechaHora(UUID agendaClasesId, Instant fechaHora);

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
