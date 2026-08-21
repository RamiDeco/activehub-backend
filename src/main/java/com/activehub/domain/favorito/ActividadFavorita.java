package com.activehub.domain.favorito;

import com.activehub.domain.actividad.Actividad;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * No extiende BaseEntity: marcar/desmarcar un favorito es alta/baja dura de
 * esta fila, no hay estado intermedio que soft-deletar.
 */
@Entity
@Table(name = "actividad_favorita", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "actividad_id"}))
@Getter
@NoArgsConstructor
public class ActividadFavorita {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ActividadFavorita(Usuario usuario, Actividad actividad) {
        this.usuario = usuario;
        this.actividad = actividad;
    }
}
