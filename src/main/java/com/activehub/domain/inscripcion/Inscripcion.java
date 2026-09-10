package com.activehub.domain.inscripcion;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * No extiende BaseEntity: "Cancelada" ya es un estado terminal de negocio que
 * cumple el rol del soft-delete (agregar ademas BaseEntity.deleted seria una
 * segunda forma de "desaparecer" una fila que el historico/roster necesita
 * seguir devolviendo).
 */
@Entity
@Table(name = "inscripcion")
@Getter
@Setter
@NoArgsConstructor
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clase_id", nullable = false)
    private Clase clase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Usuario alumno;

    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado;

    @OneToOne(mappedBy = "inscripcion", fetch = FetchType.LAZY)
    private Pago pago;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
