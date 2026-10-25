package com.activehub.domain.actividad;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActividadRepository extends JpaRepository<Actividad, UUID>, JpaSpecificationExecutor<Actividad> {

    boolean existsByTipoActividadIdAndDeletedFalse(UUID tipoActividadId);

    boolean existsByNivelIntensidadIdAndDeletedFalse(UUID nivelIntensidadId);

    long countByNivelIntensidadIdAndDeletedFalse(UUID nivelIntensidadId);

    /**
     * Recalcula el promedio de puntaje sobre las resenias visibles (no ocultas,
     * no eliminadas) de todas las clases de la actividad. Se dispara cada vez
     * que cambia el conjunto de resenias visibles: al crear una (se publica en
     * el acto) y al ocultarla, por los dos caminos — ver CrearReseniaService,
     * OcultarReseniaService y ResolverDenunciaService (OCULTAR_RESENIA). Un camino nuevo que
     * cambie la visibilidad de una resenia tiene que llamarlo, y leer todo antes (deja las
     * entidades detached).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE actividad SET rating = COALESCE((SELECT AVG(r.puntaje) FROM resenia r "
            + "JOIN clase c ON c.id = r.clase_id "
            + "WHERE c.actividad_id = :actividadId AND r.deleted = false "
            + "AND r.oculta = false), 0) "
            + "WHERE id = :actividadId", nativeQuery = true)
    void recalcularRating(@Param("actividadId") UUID actividadId);
}
