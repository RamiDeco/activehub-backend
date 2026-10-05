package com.activehub.domain.inscripcion;

import jakarta.persistence.Column;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inscripcion_id", nullable = false, unique = true)
    private Inscripcion inscripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 20)
    private MetodoPago metodo;

    @Column(name = "referencia_externa", length = 100)
    private String referenciaExterna;

    /** Id de la orden en Mercado Pago (Orders API, decision de MP-01). */
    @Column(name = "mp_order_id", length = 100)
    private String mpOrderId;

    /** Detalle crudo de MP (p. ej. motivo de rechazo). */
    @Column(name = "status_detail", length = 100)
    private String statusDetail;

    /** Comision de la plataforma. Hoy siempre 0 (MP-01: la plataforma no cobra comision). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal comision = BigDecimal.ZERO;

    @Column(name = "monto_instructor", precision = 10, scale = 2)
    private BigDecimal montoInstructor;

    /** Vencimiento de la autorizacion mientras el pago esta {@link EstadoPago#Retenido}. */
    @Column(name = "autorizado_hasta")
    private Instant autorizadoHasta;

    /**
     * Cuando se capturo la orden en MP (al finalizar la clase, KAN-65). Null = autorizado sin
     * capturar: reintegrar es cancelar la autorizacion; con fecha, es reembolsar.
     */
    @Column(name = "capturado_at")
    private Instant capturadoAt;

    /** Header X-Idempotency-Key enviado a MP: evita crear dos ordenes por un reintento. */
    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
