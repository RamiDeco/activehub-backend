package com.activehub.domain.soporte;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReporteSoporteRepository extends JpaRepository<ReporteSoporte, UUID> {

    /**
     * Los abiertos primero y, dentro de cada grupo, el mas viejo arriba: la bandeja se lee de
     * arriba hacia abajo y lo que primero hay que atender es lo que mas tiempo lleva esperando.
     *
     * <p>{@code JOIN FETCH} no: las dos relaciones son opcionales (un reporte anonimo no tiene
     * usuario, uno abierto no tiene {@code cerradoPor}) y un INNER los dejaria afuera. Van con
     * LEFT JOIN FETCH para no caer en N+1 al resolver los nombres.
     */
    @Query("SELECT r FROM ReporteSoporte r "
            + "LEFT JOIN FETCH r.usuario "
            + "LEFT JOIN FETCH r.cerradoPor "
            + "ORDER BY CASE WHEN r.estado = com.activehub.domain.soporte.EstadoReporteSoporte.ABIERTO "
            + "THEN 0 ELSE 1 END ASC, r.createdAt ASC")
    List<ReporteSoporte> findAllConDetalle();
}
