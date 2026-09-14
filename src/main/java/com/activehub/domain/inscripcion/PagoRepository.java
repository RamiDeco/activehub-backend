package com.activehub.domain.inscripcion;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

    /**
     * Pagos en un estado dado, con la inscripcion y la clase ya cargadas: lo usa el
     * scheduler de liberacion, que necesita mirar el estado de la clase y su fechaHora.
     */
    @Query("SELECT p FROM Pago p JOIN FETCH p.inscripcion i JOIN FETCH i.clase WHERE p.estado = :estado")
    List<Pago> findByEstadoConClase(EstadoPago estado);
}
