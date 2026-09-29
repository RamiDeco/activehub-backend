package com.activehub.domain.penalizacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PenalizacionRepository extends JpaRepository<Penalizacion, UUID> {

    /** Orden pedido por el usuario: de la más antigua a la más reciente. */
    @Query("SELECT p FROM Penalizacion p JOIN FETCH p.usuario u LEFT JOIN FETCH p.denuncia ORDER BY p.createdAt ASC")
    List<Penalizacion> findAllConDetalle();

    /**
     * Suspension de un usuario vigente hoy. "Vigente" incluye los dos extremos: una suspension
     * del 1 al 20 sigue en pie todo el dia 20.
     */
    @Query("SELECT p FROM Penalizacion p WHERE p.usuario.id = :usuarioId AND p.tipo = :tipo "
            + "AND p.fechaInicio <= :fecha AND p.fechaFin >= :fecha ORDER BY p.fechaFin DESC LIMIT 1")
    Optional<Penalizacion> findSuspensionVigente(UUID usuarioId, TipoPenalizacion tipo, LocalDate fecha);

    /** Suspensiones cuya vigencia ya vencio: las levanta el scheduler. */
    @Query("SELECT p FROM Penalizacion p JOIN FETCH p.usuario u "
            + "WHERE p.tipo = :tipo AND p.fechaFin IS NOT NULL AND p.fechaFin < :fecha")
    List<Penalizacion> findSuspensionesVencidas(TipoPenalizacion tipo, LocalDate fecha);
}
