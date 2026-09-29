package com.activehub.domain.usuario;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificacionEmailRepository extends JpaRepository<VerificacionEmail, UUID> {

    /** El último código emitido para ese usuario, usado o no: es el que se valida. */
    Optional<VerificacionEmail> findFirstByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);

    /**
     * Invalida los códigos pendientes del usuario. Se llama antes de emitir uno nuevo: si el
     * anterior siguiera sirviendo, "reenviar" dejaría dos códigos válidos a la vez y el
     * contador de intentos de uno no protegería al otro.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VerificacionEmail v SET v.usadoAt = :ahora "
            + "WHERE v.usuario.id = :usuarioId AND v.usadoAt IS NULL")
    int invalidarPendientes(@Param("usuarioId") UUID usuarioId, @Param("ahora") java.time.Instant ahora);
}
