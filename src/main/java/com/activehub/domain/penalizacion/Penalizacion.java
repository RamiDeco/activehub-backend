package com.activehub.domain.penalizacion;

import com.activehub.domain.denuncia.Denuncia;
import com.activehub.domain.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Registro de solo-append e inmutable, mismo criterio que AuditLog: no
 * extiende BaseEntity porque no tiene sentido soft-deletar ni actualizar una
 * penalizacion ya aplicada.
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
