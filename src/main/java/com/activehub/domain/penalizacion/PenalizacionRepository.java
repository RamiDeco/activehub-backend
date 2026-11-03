package com.activehub.domain.penalizacion;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PenalizacionRepository extends JpaRepository<Penalizacion, UUID> {

    /** Orden pedido por el usuario: de la más antigua a la más reciente. */
    @Query("SELECT p FROM Penalizacion p JOIN FETCH p.usuario u LEFT JOIN FETCH p.denuncia "
            + "LEFT JOIN FETCH p.levantadaPor LEFT JOIN FETCH p.pagoAprobadoPor ORDER BY p.createdAt ASC")
    List<Penalizacion> findAllConDetalle();

    /**
     * Suspension de un usuario vigente hoy. "Vigente" incluye los dos extremos: una suspension
     * del 1 al 20 sigue en pie todo el dia 20.
     */
    @Query("SELECT p FROM Penalizacion p WHERE p.usuario.id = :usuarioId AND p.tipo = :tipo "
            + "AND p.fechaInicio <= :fecha AND p.fechaFin >= :fecha AND p.levantadaAt IS NULL "
            + "ORDER BY p.fechaFin DESC LIMIT 1")
    Optional<Penalizacion> findSuspensionVigente(UUID usuarioId, TipoPenalizacion tipo, LocalDate fecha);

    /** La multa que bloquea al usuario, si tiene alguna (V36): pendiente de pago o en revision. */
    @Query("SELECT p FROM Penalizacion p WHERE p.usuario.id = :usuarioId "
            + "AND p.estadoPago IN :estados ORDER BY p.createdAt ASC LIMIT 1")
    Optional<Penalizacion> findMultaQueBloquea(UUID usuarioId, Collection<EstadoPagoPenalizacion> estados);

    /** Las penalizaciones de un usuario, de la mas reciente a la mas antigua: "Mis sanciones". */
    @Query("SELECT p FROM Penalizacion p WHERE p.usuario.id = :usuarioId ORDER BY p.createdAt DESC")
    List<Penalizacion> findDeUsuario(UUID usuarioId);

    /** Suspensiones cuya vigencia ya vencio: las levanta el scheduler. */
    @Query("SELECT p FROM Penalizacion p JOIN FETCH p.usuario u "
            + "WHERE p.tipo = :tipo AND p.fechaFin IS NOT NULL AND p.fechaFin < :fecha")
    List<Penalizacion> findSuspensionesVencidas(TipoPenalizacion tipo, LocalDate fecha);
}
