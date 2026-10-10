package com.activehub.domain.mercadopago;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CuentaMercadoPagoInstructorRepository
        extends JpaRepository<CuentaMercadoPagoInstructor, UUID> {

    Optional<CuentaMercadoPagoInstructor> findByInstructorId(UUID instructorId);

    /**
     * El estado de la vinculacion SIN tocar los tokens. Cargar la entidad los descifra (y sin
     * {@code MP_TOKEN_ENCRYPTION_KEY} falla): para mostrar "vinculada si/no" no hace falta.
     */
    @Query("SELECT c.id AS id, c.mpUserId AS mpUserId, c.estado AS estado, c.updatedAt AS actualizadaEl, "
            + "c.expiresAt AS expiraEl, c.publicKey AS publicKey FROM CuentaMercadoPagoInstructor c WHERE c.instructor.id = :instructorId")
    Optional<ResumenCuenta> findResumenByInstructorId(UUID instructorId);

    /**
     * KAN-55: ids de las cuentas en {@code estado} cuyo token vence antes de {@code limite}
     * (incluidas las ya vencidas). Solo los ids: cargar todas las entidades juntas descifra
     * todos los tokens en una sola consulta, y uno ilegible tiraria la corrida entera.
     */
    @Query("SELECT c.id FROM CuentaMercadoPagoInstructor c "
            + "WHERE c.estado = :estado AND c.expiresAt IS NOT NULL AND c.expiresAt < :limite")
    List<UUID> findIdsQueVencenAntesDe(EstadoCuentaMercadoPago estado, Instant limite);

    /**
     * KAN-55: desvincula sin cargar la entidad, asi no hace falta descifrar los tokens que se
     * van a borrar (y funciona aunque {@code MP_TOKEN_ENCRYPTION_KEY} haya cambiado). Un UPDATE
     * en bloque no dispara {@code @UpdateTimestamp}: por eso {@code updatedAt} va explicito.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CuentaMercadoPagoInstructor c SET c.estado = :estado, c.accessToken = NULL, "
            + "c.refreshToken = NULL, c.expiresAt = NULL, c.updatedAt = :ahora WHERE c.instructor.id = :instructorId")
    int desvincular(UUID instructorId, EstadoCuentaMercadoPago estado, Instant ahora);

    interface ResumenCuenta {
        UUID getId();

        String getMpUserId();

        EstadoCuentaMercadoPago getEstado();

        Instant getActualizadaEl();

        Instant getExpiraEl();

        /** Publica por diseno (va al navegador para tokenizar la tarjeta). No es un secreto. */
        String getPublicKey();

        /** Lo unico que decide si el instructor puede cobrar con Mercado Pago hoy. */
        default boolean vigente(Instant ahora) {
            return getEstado() == EstadoCuentaMercadoPago.VINCULADA
                    && (getExpiraEl() == null || getExpiraEl().isAfter(ahora));
        }
    }
}
