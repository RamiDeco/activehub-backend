package com.activehub.domain.resenia;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReseniaRepository extends JpaRepository<Resenia, UUID> {

    boolean existsByClaseIdAndAlumnoId(UUID claseId, UUID alumnoId);

    @Query("SELECT r FROM Resenia r JOIN FETCH r.clase c JOIN FETCH c.actividad JOIN FETCH r.alumno "
            + "WHERE r.enModeracion = true ORDER BY r.createdAt ASC")
    List<Resenia> findPendientesConDetalle();

    @Query("SELECT r FROM Resenia r JOIN FETCH r.alumno "
            + "WHERE r.clase.actividad.id = :actividadId AND r.enModeracion = false "
            + "ORDER BY r.createdAt DESC")
    List<Resenia> findVisiblesPorActividad(@Param("actividadId") UUID actividadId);

    @Query("SELECT r FROM Resenia r JOIN FETCH r.clase c JOIN FETCH c.actividad a JOIN FETCH a.instructor "
            + "WHERE r.alumno.id = :alumnoId ORDER BY r.createdAt DESC")
    List<Resenia> findByAlumnoIdConDetalle(@Param("alumnoId") UUID alumnoId);

    @Query("SELECT r FROM Resenia r JOIN FETCH r.clase c JOIN FETCH c.actividad a JOIN FETCH r.alumno "
            + "WHERE a.instructor.id = :instructorId ORDER BY r.createdAt DESC")
    List<Resenia> findByInstructorIdConDetalle(@Param("instructorId") UUID instructorId);
}
