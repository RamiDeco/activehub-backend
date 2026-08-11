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

    @Query("SELECT i FROM Inscripcion i "
            + "JOIN FETCH i.clase c "
            + "JOIN FETCH c.actividad a "
            + "LEFT JOIN FETCH i.pago p "
            + "WHERE i.alumno.id = :alumnoId "
            + "AND (:estado IS NULL OR i.estado = :estado) "
            + "ORDER BY i.createdAt DESC")
    List<Inscripcion> findByAlumnoIdConDetalle(@Param("alumnoId") UUID alumnoId, @Param("estado") EstadoInscripcion estado);
}
