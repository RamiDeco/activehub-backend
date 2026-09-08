package com.activehub.domain.penalizacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PenalizacionRepository extends JpaRepository<Penalizacion, UUID> {

    @Query("SELECT p FROM Penalizacion p JOIN FETCH p.usuario u LEFT JOIN FETCH p.denuncia ORDER BY p.createdAt DESC")
    List<Penalizacion> findAllConDetalle();

    /** Suspensiones cuya vigencia ya vencio: las levanta el scheduler. */
    @Query("SELECT p FROM Penalizacion p JOIN FETCH p.usuario u "
            + "WHERE p.tipo = :tipo AND p.fechaFin IS NOT NULL AND p.fechaFin < :fecha")
    List<Penalizacion> findSuspensionesVencidas(TipoPenalizacion tipo, LocalDate fecha);
}
