package com.activehub.domain.usuario;

import com.activehub.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "perfil_instructor")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class PerfilInstructor extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String especialidad;

    @Column(name = "anios_experiencia")
    private Integer aniosExperiencia;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_verificacion", nullable = false, length = 20)
    private EstadoVerificacion estadoVerificacion = EstadoVerificacion.PENDIENTE;

    /** Motivo del ÚLTIMO rechazo. Se conserva al volver a postularse (el admin lo ve para comparar). */
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;

    /**
     * Cuándo fue el último rechazo (V35). Para volver a postularse hace falta documentación
     * subida DESPUÉS de esta fecha, y es lo que marca como "nuevo" un documento para el admin.
     */
    @Column(name = "rechazado_at")
    private Instant rechazadoAt;

    public PerfilInstructor(Usuario usuario, String especialidad, Integer aniosExperiencia, String descripcion) {
        this.usuario = usuario;
        this.especialidad = especialidad;
        this.aniosExperiencia = aniosExperiencia;
        this.descripcion = descripcion;
        this.estadoVerificacion = EstadoVerificacion.PENDIENTE;
    }
}
