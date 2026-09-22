package com.activehub.domain.interaccion;

import com.activehub.domain.actividad.Actividad;
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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Una señal de comportamiento del alumno (V27). Solo-append e inmutable, como
 * {@code AuditLog}: no extiende {@code BaseEntity} porque no tiene sentido actualizar ni dar
 * de baja un hecho que ya ocurrio.
 *
 * <p>Las dos columnas de contenido son excluyentes segun el tipo: {@code actividad} para
 * {@link TipoInteraccion#VISTA_ACTIVIDAD} y {@code termino} para
 * {@link TipoInteraccion#BUSQUEDA}. Los dos constructores de abajo son los unicos caminos para
 * crearla, justamente para que no se pueda armar una fila con el contenido de otro tipo.
 */
@Entity
@Table(name = "interaccion_alumno")
@Getter
@NoArgsConstructor
public class InteraccionAlumno {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoInteraccion tipo;

    /** Solo en {@link TipoInteraccion#VISTA_ACTIVIDAD}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id")
    private Actividad actividad;

    /** Solo en {@link TipoInteraccion#BUSQUEDA}. Ya viene recortado a 120 caracteres. */
    @Column(length = 120)
    private String termino;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private InteraccionAlumno(Usuario usuario, TipoInteraccion tipo, Actividad actividad, String termino) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.actividad = actividad;
        this.termino = termino;
    }

    public static InteraccionAlumno vista(Usuario usuario, Actividad actividad) {
        return new InteraccionAlumno(usuario, TipoInteraccion.VISTA_ACTIVIDAD, actividad, null);
    }

    public static InteraccionAlumno busqueda(Usuario usuario, String termino) {
        return new InteraccionAlumno(usuario, TipoInteraccion.BUSQUEDA, null, termino);
    }
}
