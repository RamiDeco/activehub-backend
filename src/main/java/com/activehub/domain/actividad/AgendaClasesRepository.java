package com.activehub.domain.actividad;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendaClasesRepository extends JpaRepository<AgendaClases, UUID> {

    List<AgendaClases> findByActividadId(UUID actividadId);

    /** Agendas que todavia pueden generar clases a la fecha dada. */
    @Query("SELECT a FROM AgendaClases a JOIN FETCH a.actividad "
            + "WHERE a.vigenciaDesde <= :fecha AND (a.vigenciaHasta IS NULL OR a.vigenciaHasta >= :fecha)")
    List<AgendaClases> findVigentesConDetalle(@Param("fecha") LocalDate fecha);
}
