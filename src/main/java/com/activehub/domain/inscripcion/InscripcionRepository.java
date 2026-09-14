package com.activehub.domain.inscripcion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {

    Optional<Inscripcion> findByClaseIdAndAlumnoIdAndEstadoNot(UUID claseId, UUID alumnoId, EstadoInscripcion estadoExcluido);

    List<Inscripcion> findByClaseIdAndEstadoInOrderByCreatedAtAsc(UUID claseId, Collection<EstadoInscripcion> estados);

    List<Inscripcion> findByClaseIdAndEstadoNot(UUID claseId, EstadoInscripcion estado);

    long countByClaseIdAndEstado(UUID claseId, EstadoInscripcion estado);

    boolean existsByClaseIdAndAlumnoIdAndEstado(UUID claseId, UUID alumnoId, EstadoInscripcion estado);

    /** Hay alguien anotado (Inscripto, PagoPendiente o PreInscripción) en esta clase. */
    boolean existsByClaseIdAndEstadoNot(UUID claseId, EstadoInscripcion estadoExcluido);

    @Query("SELECT i FROM Inscripcion i "
            + "JOIN FETCH i.clase c "
            + "JOIN FETCH c.actividad a "
            + "LEFT JOIN FETCH i.pago p "
            + "WHERE i.alumno.id = :alumnoId "
            + "AND (:estado IS NULL OR i.estado = :estado) "
            + "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByAlumnoIdConDetalle(@Param("alumnoId") UUID alumnoId, @Param("estado") EstadoInscripcion estado);

    @Query("SELECT i FROM Inscripcion i "
            + "JOIN FETCH i.clase c "
            + "JOIN FETCH c.actividad a "
            + "JOIN FETCH i.alumno al "
            + "LEFT JOIN FETCH i.pago p "
            + "ORDER BY i.createdAt DESC")
    List<Inscripcion> findAllConDetalle();

    /** Inscripciones a las clases de un instructor: alimenta su panel y sus metricas. */
    @Query("SELECT i FROM Inscripcion i "
            + "JOIN FETCH i.clase c "
            + "JOIN FETCH c.actividad a "
            + "JOIN FETCH i.alumno al "
            + "LEFT JOIN FETCH i.pago p "
            + "WHERE a.instructor.id = :instructorId "
            + "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByInstructorIdConDetalle(@Param("instructorId") UUID instructorId);
}
