package com.activehub.domain.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Un código de 6 dígitos emitido para confirmar una dirección de correo.
 *
 * <p>No extiende BaseEntity, misma razón que {@code Notificacion}: es de solo-append salvo
 * {@code usadoAt} e {@code intentos}, que se actualizan in-place al validar.
 *
 * <p><b>El código no se guarda en claro.</b> Son seis dígitos, pero es un secreto de un solo
 * uso y esta tabla la lee cualquiera con acceso a la base; se guarda el hash que produce el
 * {@code PasswordEncoder} de la app.
 */
@Entity
@Table(name = "verificacion_email")
@Getter
@Setter
@NoArgsConstructor
public class VerificacionEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * El correo <b>al que se mandó</b>, que no siempre es el de la cuenta: en un cambio de
     * correo es el nuevo, y {@code usuario.email} recién se actualiza cuando este código se
     * confirma. Así un error de tipeo en el nuevo no deja al usuario sin el viejo.
     */
    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "codigo_hash", nullable = false, length = 255)
    private String codigoHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PropositoVerificacion proposito;

    @Column(name = "expira_at", nullable = false)
    private Instant expiraAt;

    /** Cuándo se confirmó. Nulo = todavía pendiente. Un código usado no vuelve a servir. */
    @Column(name = "usado_at")
    private Instant usadoAt;

    /** Fallidos acumulados. Pasado el máximo el código se invalida: 6 dígitos son 10^6. */
    @Column(nullable = false)
    private int intentos = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public VerificacionEmail(
            Usuario usuario, String email, String codigoHash, PropositoVerificacion proposito, Instant expiraAt) {
        this.usuario = usuario;
        this.email = email;
        this.codigoHash = codigoHash;
        this.proposito = proposito;
        this.expiraAt = expiraAt;
    }

    public boolean estaVigente(Instant ahora) {
        return usadoAt == null && ahora.isBefore(expiraAt);
    }
}
