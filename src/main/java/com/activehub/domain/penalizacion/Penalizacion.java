package com.activehub.domain.penalizacion;

import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.usuario.Usuario;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * No extiende BaseEntity: una penalizacion aplicada no se da de baja. Desde V36 si cambian dos
 * cosas, y quedan registradas con quien y cuando: el estado del pago de una multa
 * (PENDIENTE_PAGO -> EN_REVISION -> PAGADA) y el levantamiento anticipado de una suspension.
 */
@Entity
@Table(name = "penalizacion")
@Getter
@Setter
@NoArgsConstructor
public class Penalizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 30)
    private TipoPenalizacion tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motivo;

    /** Solo para tipo ECONOMICA. */
    @Column(precision = 10, scale = 2)
    private BigDecimal monto;

    /** Solo para tipo SUSPENSION_TEMPORAL: define la vigencia de la sancion. */
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    /** Denuncia que la origino, si nacio de una resolucion. Null si la aplico el admin a mano. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denuncia_id")
    private Denuncia denuncia;

    /**
     * Solo para tipo ECONOMICA aplicada desde V36. Null en las multas anteriores, que no
     * bloquean: nunca se le pidio a nadie que las pagara por la plataforma.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", length = 20)
    private EstadoPagoPenalizacion estadoPago;

    @Column(name = "pago_aprobado_at")
    private Instant pagoAprobadoAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_aprobado_por_id")
    private Usuario pagoAprobadoPor;

    /** Solo para SUSPENSION_TEMPORAL: el administrador la levanto antes de fechaFin. */
    @Column(name = "levantada_at")
    private Instant levantadaAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "levantada_por_id")
    private Usuario levantadaPor;

    @Column(name = "motivo_levantamiento", columnDefinition = "TEXT")
    private String motivoLevantamiento;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
