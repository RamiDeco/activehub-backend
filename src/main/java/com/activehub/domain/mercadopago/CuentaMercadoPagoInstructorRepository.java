package com.activehub.domain.mercadopago;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CuentaMercadoPagoInstructorRepository
        extends JpaRepository<CuentaMercadoPagoInstructor, UUID> {

    Optional<CuentaMercadoPagoInstructor> findByInstructorId(UUID instructorId);

    /**
     * El estado de la vinculacion SIN tocar los tokens. Cargar la entidad los descifra (y sin
     * {@code MP_TOKEN_ENCRYPTION_KEY} falla): para mostrar "vinculada si/no" no hace falta.
     */
    @Query("SELECT c.mpUserId AS mpUserId, c.estado AS estado, c.updatedAt AS actualizadaEl, "
            + "c.expiresAt AS expiraEl FROM CuentaMercadoPagoInstructor c WHERE c.instructor.id = :instructorId")
    Optional<ResumenCuenta> findResumenByInstructorId(UUID instructorId);

    interface ResumenCuenta {
        String getMpUserId();

        EstadoCuentaMercadoPago getEstado();

        Instant getActualizadaEl();

        Instant getExpiraEl();
    }
}
