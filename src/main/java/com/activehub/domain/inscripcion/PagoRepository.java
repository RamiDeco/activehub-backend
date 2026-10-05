package com.activehub.domain.inscripcion;

import com.activehub.domain.actividad.EstadoClase;
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

    /**
     * KAN-65: pagos de Mercado Pago autorizados y todavia sin capturar de clases en el estado
     * dado (Finalizada). Trae el instructor porque la captura va con su token, y la consulta
     * corre fuera de transaccion: sin el fetch, el proxy lazy no se podria leer.
     */
    @Query("SELECT p FROM Pago p JOIN FETCH p.inscripcion i JOIN FETCH i.clase c JOIN FETCH c.actividad a "
            + "JOIN FETCH a.instructor WHERE p.estado = :estado AND p.mpOrderId IS NOT NULL "
            + "AND p.capturadoAt IS NULL AND c.estado = :estadoClase")
    List<Pago> findSinCapturarDeClasesEn(EstadoPago estado, EstadoClase estadoClase);
}
