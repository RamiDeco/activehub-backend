package com.activehub.domain.inscripcion;

import com.activehub.domain.actividad.EstadoClase;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

    /**
     * KAN-60: el pago de una orden de Mercado Pago, con todo lo que necesita el webhook (el
     * alumno a notificar y el instructor, cuyo token se usa para consultar la orden). Sin
     * distinguir mayusculas: MP manda el id de la orden en mayusculas en un lado y minusculas
     * en otro.
     */
    @Query("SELECT p FROM Pago p JOIN FETCH p.inscripcion i JOIN FETCH i.alumno JOIN FETCH i.clase c "
            + "JOIN FETCH c.actividad a JOIN FETCH a.instructor WHERE LOWER(p.mpOrderId) = LOWER(:mpOrderId)")
    java.util.Optional<Pago> findConDetalleByMpOrderId(String mpOrderId);

    /**
     * KAN-55: pagos de Mercado Pago del instructor que todavia necesitan su token (para
     * capturarlos, reembolsarlos o, en revision, consultarlos desde el webhook). Con alguno,
     * no puede desvincular la cuenta.
     */
    @Query("SELECT COUNT(p) FROM Pago p JOIN p.inscripcion i JOIN i.clase c JOIN c.actividad a "
            + "WHERE a.instructor.id = :instructorId AND p.mpOrderId IS NOT NULL AND p.estado IN :estados")
    long countDeMercadoPagoDeInstructorEn(java.util.UUID instructorId, List<EstadoPago> estados);

    /**
     * KAN-66: la fila del pago con {@code SELECT ... FOR UPDATE}. El webhook la pide antes de
     * decidir nada: si en ese momento hay un reintegro en curso desde ActiveHub (que bloquea la
     * misma fila antes de llamar a MP), espera a que termine y recien entonces lee el estado.
     * Sin esto, leia el pago todavia Retenido y tomaba nuestro propio reembolso por uno hecho
     * desde el panel de MP, con una segunda auditoria. Solo bloquea {@code pago}: con los JOIN
     * del detalle, el FOR UPDATE bloquearia tambien la inscripcion, la clase y la actividad.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pago p WHERE LOWER(p.mpOrderId) = LOWER(:mpOrderId)")
    java.util.Optional<Pago> bloquearPorMpOrderId(String mpOrderId);

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

    /**
     * KAN-61: pagos de Mercado Pago autorizados, sin capturar, cuya autorizacion vence antes de
     * {@code limite}, en clases que todavia no finalizaron. Las Finalizadas ya las trae
     * {@link #findSinCapturarDeClasesEn}; las Canceladas ya se reintegraron.
     */
    @Query("SELECT p FROM Pago p JOIN FETCH p.inscripcion i JOIN FETCH i.clase c JOIN FETCH c.actividad a "
            + "JOIN FETCH a.instructor WHERE p.estado = :estado AND p.mpOrderId IS NOT NULL "
            + "AND p.capturadoAt IS NULL AND p.autorizadoHasta < :limite AND c.estado IN :estadosClase")
    List<Pago> findSinCapturarPorVencer(EstadoPago estado, java.time.Instant limite, List<EstadoClase> estadosClase);
}
