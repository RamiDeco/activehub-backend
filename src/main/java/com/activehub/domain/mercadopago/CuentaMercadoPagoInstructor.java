package com.activehub.domain.mercadopago;

import com.activehub.domain.usuario.Usuario;
import com.activehub.shared.persistence.TokenCifradoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * La cuenta de Mercado Pago que un instructor vinculo por OAuth (marketplace): con su access
 * token la plataforma crea las ordenes a nombre del instructor.
 *
 * <p>No extiende BaseEntity: desvincular es un cambio de {@code estado}, no una baja; y hay
 * una sola fila por instructor (UNIQUE), que se reutiliza si vuelve a vincular.
 *
 * <p>Los tokens se persisten cifrados con {@link TokenCifradoConverter}; en memoria estan en
 * claro, asi que esta entidad nunca sale por un Controller (regla general, aca mas que nunca).
 */
@Entity
@Table(name = "cuenta_mercadopago_instructor")
@Getter
@Setter
@NoArgsConstructor
public class CuentaMercadoPagoInstructor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false, unique = true)
    private Usuario instructor;

    @Column(name = "mp_user_id", length = 50)
    private String mpUserId;

    @Convert(converter = TokenCifradoConverter.class)
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Convert(converter = TokenCifradoConverter.class)
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "public_key", length = 100)
    private String publicKey;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuentaMercadoPago estado = EstadoCuentaMercadoPago.VINCULADA;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
